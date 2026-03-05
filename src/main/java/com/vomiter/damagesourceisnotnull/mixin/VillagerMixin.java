package com.vomiter.damagesourceisnotnull.mixin;

import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(Villager.class)
public abstract class VillagerMixin extends Entity {

    public VillagerMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource dsg$guardDieSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "die", source);
    }
}
