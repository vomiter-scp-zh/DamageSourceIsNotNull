package com.vomiter.damagesourceisnotnull.mixin;

import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Entity.class, priority = 10000)
public abstract class EntityMixin {

    @ModifyVariable(method = "isInvulnerableTo", at = @At("HEAD"), argsOnly = true)
    private DamageSource dsn$guardInvulnerableSource(DamageSource source) {
        // isInvulnerableTo 在 Entity 層就會用到 source.is(...), 所以必須先 guard
        if ((Object) this instanceof LivingEntity living) {
            return DamageSourceGuard.guard(living, "invulnerableTo", source);
        }
        // 極少數非 LivingEntity 的情況：仍然給 generic fallback
        return source != null ? source : ((Entity)(Object)this).level().damageSources().generic();
    }
}