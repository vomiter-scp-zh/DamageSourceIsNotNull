package com.vomiter.damagesourceisnotnull.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.vomiter.damagesourceisnotnull.DamageSourceGuard;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerMixin extends Entity {

    public AbstractVillagerMixin(EntityType<?> p_19870_, Level p_19871_) {
        super(p_19870_, p_19871_);
    }

    @WrapMethod(method = "die")
    private void wrapDie(DamageSource damageSource, Operation<Void> original){
        if(damageSource == null) damageSource = DamageSourceGuard.guard((LivingEntity)(Object)this, "die", damageSource);
        original.call(damageSource);
    }
}
