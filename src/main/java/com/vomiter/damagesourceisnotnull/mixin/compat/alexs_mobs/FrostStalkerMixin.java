package com.vomiter.damagesourceisnotnull.mixin.compat.alexs_mobs;

import com.github.alexthe666.alexsmobs.entity.EntityFroststalker;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityFroststalker.class)
public class FrostStalkerMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, require = 0)
    private DamageSource dsg$guardHurtSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "hurt", source);
    }
}
