package com.vomiter.damagesourceisnotnull.mixin.compat.quark;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.violetmoon.quark.content.mobs.entity.Crab;

@Mixin(Crab.class)
public class CrabMixin {
    @ModifyVariable(method = "isInvulnerableTo", at = @At("HEAD"), argsOnly = true, require = 0)
    private DamageSource dsn$guardInvulnerableSource(DamageSource source) {
        return source != null ? source : ((Entity)(Object)this).level().damageSources().generic();
    }
}
