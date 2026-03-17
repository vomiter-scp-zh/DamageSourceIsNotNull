package com.vomiter.damagesourceisnotnull.mixin.compat.alexs_mobs;

import com.github.alexthe666.alexsmobs.entity.EntityRoadrunner;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityRoadrunner.class)
public class RoadRunnerMixin {
    @WrapMethod(method = "isInvulnerableTo")
    private boolean wrapInvulnerableTo(DamageSource damageSource, Operation<Boolean> original){
        if(damageSource == null && (Object)this instanceof LivingEntity living) damageSource = DamageSourceGuard.guard(living, "invulnerable", damageSource);
        return original.call(damageSource);
    }
}
