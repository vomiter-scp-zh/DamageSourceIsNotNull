package com.vomiter.damagesourceisnotnull.mixin;

import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import com.vomiter.damagesourceisnotnull.debug.IEntityToDieWithoutLoot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class LivingEntityMixin extends Entity implements IEntityToDieWithoutLoot {
    public LivingEntityMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @ModifyVariable(method = "die", at = @At("HEAD"), argsOnly = true)
    private DamageSource dsg$guardDieSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "die", source);
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private DamageSource dsg$guardHurtSource(DamageSource source) {
        return DamageSourceGuard.guard((LivingEntity)(Object)this, "hurt", source);
    }

    @Unique private boolean damageSourceIsNotNull$dieWithoutLoot = false;
    @Unique
    public void setToDieWithoutLoot(boolean b){
        damageSourceIsNotNull$dieWithoutLoot = b;
    }

    @Inject(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropAllDeathLoot(Lnet/minecraft/world/damagesource/DamageSource;)V"), cancellable = true)
    private void dieWithoutLoot(DamageSource p_21014_, CallbackInfo ci){
        if(!damageSourceIsNotNull$dieWithoutLoot) return;
        ci.cancel();
    }

}
