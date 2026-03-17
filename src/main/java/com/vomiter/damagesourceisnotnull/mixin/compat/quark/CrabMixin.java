package com.vomiter.damagesourceisnotnull.mixin.compat.quark;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.violetmoon.quark.content.mobs.entity.Crab;

@Mixin(Crab.class)
public class CrabMixin {
    @WrapMethod(method = "isInvulnerableTo")
    private boolean wrapInvulnerableTo(DamageSource damageSource, Operation<Boolean> original){
        if(damageSource == null && (Object)this instanceof LivingEntity living) damageSource = DamageSourceGuard.guard(living, "invulnerable", damageSource);
        return original.call(damageSource);
    }
}
