package com.vomiter.damagesourceisnotnull.mixin.compat.alexs_mobs;

import com.github.alexthe666.alexsmobs.entity.EntityMurmurHead;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EntityMurmurHead.class)
public class MurmurHeadMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, require = 0)
    private DamageSource dsg$guardHurtSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "hurt", source);
    }

    @ModifyVariable(method = "isInvulnerableTo", at = @At("HEAD"), argsOnly = true, require = 0)
    private DamageSource dsn$guardInvulnerableSource(DamageSource source) {
        if ((Object) this instanceof LivingEntity living) {
            return DamageSourceGuard.guard(living, "invulnerableTo", source);
        }
        return source != null ? source : ((Entity)(Object)this).level().damageSources().generic();
    }
}
