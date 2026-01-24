package com.vomiter.damagesourceisnotnull;

import com.mojang.logging.LogUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public final class DamageSourceGuard {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ConcurrentHashMap<String, Long> LAST_LOG_MS = new ConcurrentHashMap<>();

    private DamageSourceGuard() {}

    /**
     * @param self  目前正在 hurt/die 的 LivingEntity
     * @param phase "hurt" or "die"
     * @param source 原始傳入的 DamageSource（可能為 null）
     * @return 保證非 null 的 DamageSource（若原本是 null 則 fallback）
     */
    public static DamageSource guard(LivingEntity self, String phase, DamageSource source) {
        if (self.level().isClientSide) return source;

        if (source != null) return source;

        logNullSource(self, phase);

        // 最穩 fallback：generic（不依賴 attacker/間接來源）
        return self.level().damageSources().generic();
    }

    private static void logNullSource(LivingEntity self, String phase) {
        // 取一個「疑似兇手」簽名：第一個非 MC / NeoForge / Mixin / Java 的 class
        String suspect = findSuspect();

        // 節流：避免每 tick 洗版
        long now = System.currentTimeMillis();
        Long prev = LAST_LOG_MS.put(suspect, now);
        long throttleMs = 5000L;
        if (prev != null && (now - prev) < throttleMs) return;

        String selfInfo;
        try {
            selfInfo = self.getType().toString()
                    + " pos=" + self.blockPosition()
                    + " dim=" + self.level().dimension().location();
        } catch (Throwable t) {
            selfInfo = "<unprintable-entity>";
        }

        LOGGER.error("[DamageSourceGuard] null DamageSource in {}. entity={} suspect={}", phase, selfInfo, suspect);
        LOGGER.error("[DamageSourceGuard] stacktrace:", new RuntimeException("null DamageSource stack"));
    }

    private static String findSuspect() {
        StackTraceElement[] st = new Exception().getStackTrace();
        for (StackTraceElement e : st) {
            String cn = e.getClassName();

            // 過濾常見框架/底層
            if (cn.startsWith("net.minecraft.")) continue;
            if (cn.startsWith("net.neoforged.")) continue;
            if (cn.startsWith("org.spongepowered.")) continue;
            if (cn.startsWith("java.")) continue;
            if (cn.startsWith("sun.")) continue;

            // 過濾自己（避免第一個命中是 guard 本身）
            if (cn.startsWith("com.vomiter.damagesourceguard.")) continue;

            return cn + "#" + e.getMethodName() + ":" + e.getLineNumber();
        }
        return "<unknown>";
    }
}
