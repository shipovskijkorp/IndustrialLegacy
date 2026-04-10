package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * IC2-like QuantumSuit damage absorption.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityQuantumArmorMixin {
    @ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private float industriallegacy$applyQuantumArmorAbsorption(float amount, DamageSource source) {
        if (amount <= 0.0f) return amount;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return amount;

        LivingEntity self = (LivingEntity) (Object) this;
        float remaining = amount;
        float absorbedTotal = 0.0f;

        if (source.isOf(DamageTypes.FALL)) {
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.FEET, 1.0f, remaining);
            remaining = amount - absorbedTotal;
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.LEGS, 0.8f, remaining);
        } else {
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.HEAD, 0.15f, remaining);
            remaining = amount - absorbedTotal;
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.CHEST, 0.48f, remaining);
            remaining = amount - absorbedTotal;
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.LEGS, 0.30f, remaining);
            remaining = amount - absorbedTotal;
            absorbedTotal += absorbFromSlot(self, EquipmentSlot.FEET, 0.15f, remaining);
        }

        float out = amount - absorbedTotal;
        return Math.max(0.0f, out);
    }

    private static float absorbFromSlot(LivingEntity entity, EquipmentSlot slot, float ratio, float remainingDamage) {
        if (remainingDamage <= 0.0f) return 0.0f;

        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof QuantumArmorItem quantum)) return 0.0f;

        float wantAbsorb = remainingDamage * ratio;
        if (wantAbsorb <= 0.0f) return 0.0f;

        long charge = quantum.getEnergy(stack);
        int energyPerDamage = QuantumArmorItem.ENERGY_PER_DAMAGE;
        int damageLimit = (energyPerDamage > 0) ? (int) Math.floor(25.0 * (double) charge / (double) energyPerDamage) : Integer.MAX_VALUE;
        float canAbsorbByCharge = Math.min(wantAbsorb, (float) damageLimit);
        if (canAbsorbByCharge <= 0.0f) return 0.0f;

        long energyCost = (long) Math.ceil(canAbsorbByCharge * (double) energyPerDamage);
        long actuallyDrained = drainIgnoreLimit(stack, energyCost);
        if (actuallyDrained <= 0L) return 0.0f;

        double paidRatio = Math.min(1.0, (double) actuallyDrained / (double) energyCost);
        return (float) (canAbsorbByCharge * paidRatio);
    }

    private static long drainIgnoreLimit(ItemStack stack, long amount) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }
}
