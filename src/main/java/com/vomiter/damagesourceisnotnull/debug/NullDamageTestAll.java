package com.vomiter.damagesourceisnotnull.debug;

import com.vomiter.damagesourceisnotnull.DamageSourceIsNotNull;
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

public final class NullDamageTestAll {

    private static final int TESTALL_PER_TICK = 10;
    private static final int TESTALL_MAX_LINES = 60;
    private static TestAllSession TESTALL;

    private NullDamageTestAll() {
    }

    public static boolean isTestAllRunning() {
        return TESTALL != null;
    }

    private enum FailStage {
        CREATE,
        HURT,
        DIE
    }

    private static final class FailureInfo {
        final Class<?> entityClass;
        boolean createFailed;
        boolean hurtFailed;
        boolean dieFailed;
        String createError;
        String hurtError;
        String dieError;

        FailureInfo(Class<?> entityClass) {
            this.entityClass = entityClass;
        }

        boolean anyFailed() {
            return createFailed || hurtFailed || dieFailed;
        }

        String summary() {
            StringBuilder sb = new StringBuilder();

            if (createFailed) {
                sb.append("create");
            }
            if (hurtFailed) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("hurt");
            }
            if (dieFailed) {
                if (sb.length() > 0) sb.append(", ");
                sb.append("die");
            }

            return sb.toString();
        }
    }

    private static final class TestAllSession {
        final UUID owner;
        final ServerLevel level;
        final Vec3 testPos;
        final Iterator<EntityType<?>> it;

        int scanned = 0;
        int skippedNonLiving = 0;
        int createReturnedNull = 0;

        int createFailures = 0;
        int hurtFailures = 0;
        int dieFailures = 0;

        final Map<ResourceLocation, FailureInfo> failures = new LinkedHashMap<>();

        TestAllSession(ServerPlayer player) {
            this.owner = player.getUUID();
            this.level = player.serverLevel();
            this.testPos = player.position().add(player.getLookAngle().scale(2.0)).add(0, -30, 0);
            this.it = ForgeRegistries.ENTITY_TYPES.getValues().iterator();
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TESTALL == null) return;

        ServerPlayer player = TESTALL.level.getServer().getPlayerList().getPlayer(TESTALL.owner);
        if (player == null) {
            TESTALL = null;
            return;
        }

        int processedThisTick = 0;
        while (processedThisTick < TESTALL_PER_TICK && TESTALL.it.hasNext()) {
            EntityType<?> type = TESTALL.it.next();
            processedThisTick++;

            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key == null) {
                continue;
            }

            TESTALL.scanned++;
            testOne(TESTALL, player, type, key);
        }

        if (!TESTALL.it.hasNext()) {
            sendTestAllResult(player, TESTALL);
            TESTALL = null;
        } else if (TESTALL.scanned % 200 == 0) {
            player.sendSystemMessage(
                    Component.literal(
                                    "[DSN TESTALL] scanned=" + TESTALL.scanned
                                            + ", createFail=" + TESTALL.createFailures
                                            + ", hurtFail=" + TESTALL.hurtFailures
                                            + ", dieFail=" + TESTALL.dieFailures
                            )
                            .withStyle(ChatFormatting.DARK_AQUA)
            );
        }
    }

    private static void testOne(TestAllSession s, ServerPlayer player, EntityType<?> type, ResourceLocation key) {
        Entity e;

        try {
            e = type.create(s.level);
        } catch (Throwable t) {
            FailureInfo info = getOrCreateFailure(s, key, null);
            markFailure(s, key, info, FailStage.CREATE, t);
            return;
        }

        if (e == null) {
            s.createReturnedNull++;
            DamageSourceIsNotNull.LOGGER.warn("[DamageSourceTestAll] {} returned null from EntityType#create().", key);
            return;
        }

        if (!(e instanceof LivingEntity living)) {
            s.skippedNonLiving++;
            return;
        }

        FailureInfo info = getOrCreateFailure(s, key, e.getClass());

        try {
            living.moveTo(s.testPos.x, s.testPos.y, s.testPos.z, player.getYRot(), 0.0F);
        } catch (Throwable t) {
            // 不把 moveTo 失敗直接算成 vulnerable，僅記錄，避免誤導
            DamageSourceIsNotNull.LOGGER.warn(
                    "[DamageSourceTestAll] {} ({}) failed during moveTo before test.",
                    key, e.getClass().getName(), t
            );
        }

        try {
            living.hurt(null, 1.0F);
        } catch (Throwable t) {
            markFailure(s, key, info, FailStage.HURT, t);
        }

        try {
            if (living instanceof IEntityToDieWithoutLoot dieWithoutLoot) {
                dieWithoutLoot.setToDieWithoutLoot(true);
            }
            living.die(null);
        } catch (Throwable t) {
            markFailure(s, key, info, FailStage.DIE, t);
        }

        if (e.isAddedToWorld()) {
            e.discard();
        }

        if (!info.anyFailed()) {
            s.failures.remove(key);
        }
    }

    private static FailureInfo getOrCreateFailure(TestAllSession s, ResourceLocation key, Class<?> entityClass) {
        FailureInfo existing = s.failures.get(key);
        if (existing != null) {
            return existing;
        }

        FailureInfo created = new FailureInfo(entityClass);
        s.failures.put(key, created);
        return created;
    }

    private static void markFailure(TestAllSession s, ResourceLocation key, FailureInfo info, FailStage stage, Throwable t) {
        String errorName = t.getClass().getName() + (t.getMessage() != null ? ": " + t.getMessage() : "");

        switch (stage) {
            case CREATE -> {
                if (!info.createFailed) {
                    info.createFailed = true;
                    info.createError = errorName;
                    s.createFailures++;
                }
                DamageSourceIsNotNull.LOGGER.error(
                        "[DamageSourceTestAll] {} failed CREATE stage during null DamageSource coverage scan.",
                        key, t
                );
            }
            case HURT -> {
                if (!info.hurtFailed) {
                    info.hurtFailed = true;
                    info.hurtError = errorName;
                    s.hurtFailures++;
                }
                DamageSourceIsNotNull.LOGGER.error(
                        "[DamageSourceTestAll] {} ({}) failed HURT stage during null DamageSource coverage scan.",
                        key, info.entityClass != null ? info.entityClass.getName() : "unknown", t
                );
            }
            case DIE -> {
                if (!info.dieFailed) {
                    info.dieFailed = true;
                    info.dieError = errorName;
                    s.dieFailures++;
                }
                DamageSourceIsNotNull.LOGGER.error(
                        "[DamageSourceTestAll] {} ({}) failed DIE stage during null DamageSource coverage scan.",
                        key, info.entityClass != null ? info.entityClass.getName() : "unknown", t
                );
            }
        }
    }

    static int testAll(CommandSourceStack src) {
        if (!(src.getEntity() instanceof ServerPlayer player)) {
            return 0;
        }

        if (TESTALL != null) {
            src.sendSystemMessage(
                    Component.literal("[DSN TESTALL] A test session is already running.")
                            .withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        TESTALL = new TestAllSession(player);

        src.sendSystemMessage(
                Component.literal("[DSN TESTALL] Starting null DamageSource coverage scan over all registered EntityTypes...")
                        .withStyle(ChatFormatting.DARK_AQUA)
        );
        src.sendSystemMessage(
                Component.literal("[DSN TESTALL] This is a heuristic debug scan. Some entities may fail due to custom construction logic.")
                        .withStyle(ChatFormatting.GRAY)
        );
        src.sendSystemMessage(
                Component.literal("[DSN TESTALL] If any living entities fail the test, please send the latest.log for coverage review.")
                        .withStyle(ChatFormatting.GRAY)
        );

        return 1;
    }

    private static void sendTestAllResult(ServerPlayer player, TestAllSession s) {
        int failures = s.failures.size();

        player.sendSystemMessage(
                Component.literal("[DSN TESTALL] Done.")
                        .withStyle(ChatFormatting.DARK_AQUA)
        );

        player.sendSystemMessage(
                Component.literal(
                                "[DSN TESTALL] scanned=" + s.scanned
                                        + ", nonLivingSkipped=" + s.skippedNonLiving
                                        + ", createReturnedNull=" + s.createReturnedNull
                                        + ", createFail=" + s.createFailures
                                        + ", hurtFail=" + s.hurtFailures
                                        + ", dieFail=" + s.dieFailures
                                        + ", uniqueFailedTypes=" + failures
                        )
                        .withStyle(failures == 0 ? ChatFormatting.GREEN : ChatFormatting.YELLOW)
        );

        if (failures == 0) {
            return;
        }

        player.sendSystemMessage(
                Component.literal("[DSN TESTALL] EntityTypes that failed null DamageSource test (first " + TESTALL_MAX_LINES + "):")
                        .withStyle(ChatFormatting.RED)
        );

        int shown = 0;
        for (var entry : s.failures.entrySet()) {
            if (shown >= TESTALL_MAX_LINES) break;

            ResourceLocation id = entry.getKey();
            FailureInfo info = entry.getValue();

            String clazz = info.entityClass != null ? info.entityClass.getSimpleName() : "unknown";
            String stageSummary = info.summary();

            player.sendSystemMessage(
                    Component.literal(" - " + id + " [" + clazz + "] failed: " + stageSummary)
                            .withStyle(ChatFormatting.RED)
            );
            shown++;
        }

        int remaining = failures - shown;
        if (remaining > 0) {
            player.sendSystemMessage(
                    Component.literal(" ...and " + remaining + " more")
                            .withStyle(ChatFormatting.RED)
            );
        }

        player.sendSystemMessage(
                Component.literal("[DSN TESTALL] Please attach latest.log when reporting these results.")
                        .withStyle(ChatFormatting.GRAY)
        );
    }
}