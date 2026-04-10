package com.vomiter.damagesourceisnotnull.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.Comparator;
import java.util.List;

public final class NullDamageSourceCommand {
    private NullDamageSourceCommand() {}

    private static LivingEntity THE_LIVING_TO_DIE;
    private static boolean HARD_KILL_SWITCH = false;
    private static LivingEntity THE_LIVING_TO_HURT;
    private static boolean HARD_HURT_SWITCH = false;

    // 你原本的 tick-scheduled hurt/kill
    public static void onLiving(LivingEvent.LivingTickEvent event){
        var living = event.getEntity();
        if (HARD_KILL_SWITCH) {
            if (living.equals(THE_LIVING_TO_DIE)) {
                living.die(null);
                living.discard();
                HARD_KILL_SWITCH = false;
            }
        } else if (HARD_HURT_SWITCH) {
            if (living.equals(THE_LIVING_TO_HURT)) {
                living.hurt(null, 1);
                HARD_HURT_SWITCH = false;
            }
        }
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("dsnnull")
                        .then(Commands.literal("debug")
                                .then(Commands.literal("hurt").executes(ctx -> run(ctx.getSource(), Mode.HURT)))
                                .then(Commands.literal("kill").executes(ctx -> run(ctx.getSource(), Mode.KILL)))
                                .then(Commands.literal("testall").executes(ctx -> NullDamageTestAll.testAll(ctx.getSource())))
                        )
        );

        dispatcher.register(
                Commands.literal("dsnnull")
                        .then(Commands.literal("notify")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.literal("on").executes(ctx -> set(ctx.getSource(), true)))
                                .then(Commands.literal("off").executes(ctx -> set(ctx.getSource(), false)))
                                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                        )
        );
    }

    // ===== core =====

    private enum Mode { HURT, KILL }

    private static int run(CommandSourceStack src, Mode mode) {
        if (!(src.getEntity() instanceof ServerPlayer player)) return 0;

        ServerLevel level = player.serverLevel();

        LivingEntity target = findNearestLivingEntity(player, 16.0);

        if (target == null) {
            target = spawnPig(level, player);
        }

        if (mode == Mode.HURT) {
            THE_LIVING_TO_HURT = target;
            HARD_HURT_SWITCH = true;
        } else if(mode == Mode.KILL) {
            THE_LIVING_TO_DIE = target;
            HARD_KILL_SWITCH = true;
        }

        String modeText = (mode == Mode.HURT) ? "HURT" : "KILL";
        src.sendSystemMessage(
                Component.literal("[DSN DEBUG] Simulating a null DamageSource " + modeText + " call to mimic a mod bug.")
                        .withStyle(ChatFormatting.DARK_AQUA)
        );

        src.sendSystemMessage(
                Component.literal("[DSN DEBUG] If the guard is working, you should find this in latest.log: suspect=com.vomiter.damagesourceisnotnull.debug.NullDamageSourceCommand")
                        .withStyle(ChatFormatting.YELLOW)
        );

        src.sendSystemMessage(
                Component.literal("[DSN DEBUG] You can also use /dsnnull notify on to receive in-game notifications when the guard intercepts a null DamageSource.")
                        .withStyle(ChatFormatting.GRAY)
        );

        return 1;
    }

    // ===== helpers =====

    private static LivingEntity findNearestLivingEntity(ServerPlayer player, double radius) {
        ServerLevel level = player.serverLevel();
        Vec3 center = player.position();

        AABB box = new AABB(
                center.x - radius, center.y - radius, center.z - radius,
                center.x + radius, center.y + radius, center.z + radius
        );

        List<LivingEntity> list = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                e -> e.isAlive() && e != player
        );

        return list.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }

    private static LivingEntity spawnPig(ServerLevel level, ServerPlayer player) {
        Pig pig = EntityType.PIG.create(level);
        if (pig == null) throw new IllegalStateException("Failed to create pig");

        Vec3 pos = player.position().add(player.getLookAngle().scale(2.0));
        pig.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0.0F);

        level.addFreshEntity(pig);
        return pig;
    }

    private static int set(CommandSourceStack src, boolean enabled) {
        DamageSourceGuard.setIngameNotifyEnabled(enabled);
        src.sendSystemMessage(Component.literal("[DSN] In-game notify: " + (enabled ? "ON" : "OFF"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        return 1;
    }

    private static int status(CommandSourceStack src) {
        boolean enabled = DamageSourceGuard.isIngameNotifyEnabled();
        src.sendSystemMessage(Component.literal("[DSN] In-game notify is " + (enabled ? "ON" : "OFF"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
        return 1;
    }
}