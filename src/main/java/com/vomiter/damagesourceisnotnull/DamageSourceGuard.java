package com.vomiter.damagesourceisnotnull;

import com.mojang.logging.LogUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public final class DamageSourceGuard {

    private static final Logger LOGGER = DSNNULLFabric.LOGGER;
    private static final ConcurrentHashMap<String, Long> LAST_LOG_MS = new ConcurrentHashMap<>();

    private DamageSourceGuard() {}

    /**
     * @param self  目前正在 hurt/die 的 LivingEntity
     * @param phase "hurt" or "die"
     * @param source 原始傳入的 DamageSource（可能為 null）
     * @return 保證非 null 的 DamageSource（若原本是 null 則 fallback）
     */
    public static DamageSource guard(LivingEntity self, String phase, DamageSource source) {
        if (source != null) return source;

        // client 端也要補上 fallback，否則渲染/同步可能怪
        DamageSource fallback = self.level().damageSources().generic();

        // 只有 server 端才記錄兇手與堆疊
        if (!self.level().isClientSide()) {
            logNullSource(self, phase);
        }

        return fallback;
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
            selfInfo = self.getType()
                    + " pos=" + self.getOnPos()
                    + " dim=" + self.level().dimension().location();
        } catch (Throwable t) {
            selfInfo = "<unprintable-entity>";
        }

        String suspectClass = suspect.split("#", 2)[0];
        String mod = ModIdResolver.resolveModFromClassName(suspectClass);

        LOGGER.error("[DamageSourceGuard] null DamageSource in {}", phase);
        LOGGER.error("[DamageSourceGuard] entity={}", selfInfo);
        LOGGER.error("[DamageSourceGuard] suspect={}", suspect);
        LOGGER.error("[DamageSourceGuard] suspectMod={}", mod);
        LOGGER.error("[DamageSourceGuard] stacktrace:", new RuntimeException("null DamageSource stack"));
    }

    private static String findSuspect() {
        StackTraceElement[] st = new Exception().getStackTrace();
        for (StackTraceElement e : st) {
            String cn = e.getClassName();

            // 過濾常見框架/底層
            if (cn.startsWith("net.minecraft.")) continue;
            if (cn.startsWith("net.neoforged.")) continue;
            if (cn.startsWith("net.minecraftforge.")) continue;
            if (cn.startsWith("net.fabricmc")) continue;
            if (cn.startsWith("org.spongepowered.")) continue;
            if (cn.startsWith("java.")) continue;
            if (cn.startsWith("sun.")) continue;

            // 過濾自己（避免第一個命中是 guard 本身）
            if (cn.startsWith("com.vomiter.damagesourceisnotnull.DamageSourceGuard")) continue;

            return cn + "#" + e.getMethodName() + ":" + e.getLineNumber();
        }
        return "<unknown>";
    }
}
