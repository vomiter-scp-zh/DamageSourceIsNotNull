package com.vomiter.damagesourceisnotnull.mixin.compat.alexs_mobs;

import com.github.alexthe666.alexsmobs.entity.EntityMoose;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityMoose.class)
public class MooseMixin {
    @WrapMethod(method = "hurt")
    private boolean wrapHurt(DamageSource damageSource, float amount, Operation<Boolean> original){
        if(damageSource == null) damageSource = DamageSourceGuard.guard((LivingEntity)(Object)this, "die", damageSource);
        original.call(damageSource, amount);
        return false;
    }
}
