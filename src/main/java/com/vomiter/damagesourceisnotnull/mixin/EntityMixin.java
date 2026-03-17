package com.vomiter.damagesourceisnotnull.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Entity.class, priority = 10000)
public abstract class EntityMixin {
    @WrapMethod(method = "isInvulnerableTo")
    private boolean wrapInvulnerableTo(DamageSource damageSource, Operation<Boolean> original){
        if(damageSource == null && (Object)this instanceof LivingEntity living) damageSource = DamageSourceGuard.guard(living, "invulnerable", damageSource);
        return original.call(damageSource);
    }
}