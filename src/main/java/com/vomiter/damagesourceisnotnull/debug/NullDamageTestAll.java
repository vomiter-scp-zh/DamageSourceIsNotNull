package com.vomiter.damagesourceisnotnull.debug;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NullDamageTestAll {
    // ===== test-all session =====

    private static final int TESTALL_PER_TICK = 10;
    private static final int TESTALL_MAX_LINES = 60; // 避免 chat 洗版
    private static TestAllSession TESTALL;

    private static final class TestAllSession {
        final UUID owner;
        final ServerLevel level;
        final Vec3 spawnPos;
        final Iterator<EntityType<?>> it;
        int scanned = 0;
        int skippedNonLiving = 0;
        int createFailed = 0;

        // 用 ResourceLocation 當 key，避免 EntityType 直接當 key 的奇怪生命週期/equals 問題
        final Map<ResourceLocation, Class<?>> failures = new LinkedHashMap<>();

        TestAllSession(ServerPlayer player) {
            this.owner = player.getUUID();
            this.level = player.serverLevel();
            this.spawnPos = player.position().add(player.getLookAngle().scale(2.0)).add(0, -30, 0);

            // ForgeRegistries.ENTITY_TYPES 會含所有註冊的 EntityType
            this.it = ForgeRegistries.ENTITY_TYPES.getValues().iterator();
        }
    }


    // 新增：每 tick 跑 test-all
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TESTALL == null) return;

        ServerPlayer player = TESTALL.level.getServer().getPlayerList().getPlayer(TESTALL.owner);
        if (player == null) {
            // owner 下線就直接終止 session
            TESTALL = null;
            return;
        }

        int processedThisTick = 0;
        while (processedThisTick < TESTALL_PER_TICK && TESTALL.it.hasNext()) {
            EntityType<?> type = TESTALL.it.next();
            processedThisTick++;

            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key == null) {
                // 理論上不該發生，但保守處理
                continue;
            }

            TESTALL.scanned++;
            Entity e = type.create(TESTALL.level);
            try {
                if (!(e instanceof LivingEntity living)) {
                    TESTALL.skippedNonLiving++;
                    continue;
                }

                // 放到玩家前方，避免生成在玩家身上；也避免某些 entity 需要 position 才能走到 hurt flow
                living.moveTo(TESTALL.spawnPos.x, TESTALL.spawnPos.y, TESTALL.spawnPos.z, player.getYRot(), 0.0F);

                // 可選：有些 entity 在未加入世界時行為可能不同；加入世界更接近「真實 tick 中」狀態
                // 但這也可能帶來副作用（AI/事件）。你要更“真實”就保留 add；要更“乾淨”就註解掉。
                TESTALL.level.addFreshEntity(living);

                // 核心：用 null DamageSource 觸發各家 override hurt/die 的地雷
                if(living instanceof IEntityToDieWithoutLoot dieWithoutLoot) dieWithoutLoot.setToDieWithoutLoot(true);
                living.hurt(null, 1);
                living.die(null);
            } catch (Throwable t) {
                TESTALL.failures.put(key, e.getClass());

            }
            if(e.isAlive()) e.discard();

        }

        if (!TESTALL.it.hasNext()) {
            // 完成：回報結果
            sendTestAllResult(player, TESTALL);
            TESTALL = null;
        } else {
            if (TESTALL.scanned % 200 == 0) {
                player.sendSystemMessage(
                        Component.literal("[DSN TESTALL] scanned=" + TESTALL.scanned + ", failures=" + TESTALL.failures.size())
                                .withStyle(ChatFormatting.DARK_AQUA)
                );
            }
        }
    }

    static int testAll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) return 0;

        if (TESTALL != null) {
            src.sendSystemMessage(Component.literal("[DSN TESTALL] A test session is already running.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        TESTALL = new TestAllSession(player);

        src.sendSystemMessage(Component.literal("[DSN TESTALL] Starting null DamageSource hurt() scan over all registered EntityTypes...")
                .withStyle(ChatFormatting.DARK_AQUA));
        src.sendSystemMessage(Component.literal("[DSN TESTALL] Each tick processes " + TESTALL_PER_TICK + " types. Results will be posted here when done.")
                .withStyle(ChatFormatting.GRAY));

        return 1;
    }

    private static void sendTestAllResult(ServerPlayer player, TestAllSession s) {
        int failures = s.failures.size();

        player.sendSystemMessage(Component.literal("[DSN TESTALL] Done.")
                .withStyle(ChatFormatting.DARK_AQUA));

        player.sendSystemMessage(Component.literal(
                        "[DSN TESTALL] scanned=" + s.scanned
                                + ", nonLivingSkipped=" + s.skippedNonLiving
                                + ", failures=" + failures)
                .withStyle(failures == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW));

        if (failures == 0) return;

        player.sendSystemMessage(Component.literal("[DSN TESTALL] Problematic EntityTypes (first " + TESTALL_MAX_LINES + "):")
                .withStyle(ChatFormatting.RED));

        int shown = 0;
        for (var entry : s.failures.entrySet()) {
            if (shown >= TESTALL_MAX_LINES) break;
            ResourceLocation id = entry.getKey();
            var clazz = entry.getValue();

            player.sendSystemMessage(
                    Component.literal(" - " + clazz.getName())
                            .withStyle(ChatFormatting.RED)
            );
            shown++;
        }

        int remaining = failures - shown;
        if (remaining > 0) {
            player.sendSystemMessage(Component.literal(" ...and " + remaining + " more")
                    .withStyle(ChatFormatting.RED));
        }
    }

}
