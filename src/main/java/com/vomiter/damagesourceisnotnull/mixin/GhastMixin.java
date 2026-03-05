package com.vomiter.damagesourceisnotnull.mixin;

import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(Ghast.class)
public abstract class GhastMixin extends Entity {

    public GhastMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private DamageSource dsg$guardHurtSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "hurt", source);
    }
}
