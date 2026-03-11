package com.vomiter.damagesourceisnotnull.mixin.compat.quark;

import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.violetmoon.quark.content.mobs.entity.Toretoise;

@Mixin(Toretoise.class)
public class ToretoiseMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, require = 0)
    private DamageSource dsg$guardHurtSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "hurt", source);
    }
}
