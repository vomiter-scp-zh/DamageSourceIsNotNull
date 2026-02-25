package com.vomiter.damagesourceisnotnull.debug;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class NullDamageSourceCommand {
    private NullDamageSourceCommand() {}
    public static LivingEntity TO_DIE;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dsnnull")
                        .then(Commands.literal("mob")
                                .then(Commands.literal("hurt").executes(ctx -> run(ctx.getSource(), Mode.HURT)))
                                .then(Commands.literal("kill").executes(ctx -> run(ctx.getSource(), Mode.KILL)))
                                .then(Commands.literal("hard_kill").executes(ctx -> run(ctx.getSource(), Mode.HARD_KILL)))

        )


        );
    }

    // ===== core =====

    private enum Mode { HURT, KILL, HARD_KILL }

    private static int run(CommandSourceStack src, Mode mode) {
        if (!(src.getEntity() instanceof ServerPlayer player)) return 0;

        ServerLevel level = player.serverLevel();

        LivingEntity target = findNearestLivingEntity(player, 16.0);

        if (target == null) {
            target = spawnPig(level, player);
        }

        if (mode == Mode.HURT) {
            target.hurt(null, 4.0F); // 刻意傳 null
        } else if(mode == Mode.KILL) {
            target.die(null); // 刻意傳 null
            if(!(target instanceof Player)) target.discard();
        } else {
            TO_DIE = target;
        }

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
}
