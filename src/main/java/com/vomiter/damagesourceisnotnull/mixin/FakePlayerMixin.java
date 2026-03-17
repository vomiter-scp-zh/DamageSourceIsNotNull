package com.vomiter.damagesourceisnotnull.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(FakePlayer.class)
public abstract class FakePlayerMixin extends Entity {

    public FakePlayerMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @WrapMethod(method = "die")
    private void wrapDie(DamageSource damageSource, Operation<Void> original){
        if(damageSource == null) damageSource = DamageSourceGuard.guard((LivingEntity)(Object)this, "die", damageSource);
        original.call(damageSource);
    }
}
