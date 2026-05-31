package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.armor.NanoArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * IL-like NanoSuit damage absorption.
 *
 * Forge special armor combines same-priority armor pieces additively, so we base each piece on the original
 * incoming damage and then clamp the combined absorbed amount.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNanoArmorMixin {
    @ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private float industriallegacy$applyNanoArmorAbsorption(float amount, DamageSource source) {
        if (amount <= 0.0f) return amount;
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return amount;

        LivingEntity self = (LivingEntity) (Object) this;
        float absorbedTotal = 0.0f;
        absorbedTotal += industriallegacy$nanoAbsorbFromSlot(self, EquipmentSlot.HEAD, amount, 0.15f * (float) NanoArmorItem.DAMAGE_ABSORPTION_RATIO);
        absorbedTotal += industriallegacy$nanoAbsorbFromSlot(self, EquipmentSlot.CHEST, amount, 0.40f * (float) NanoArmorItem.DAMAGE_ABSORPTION_RATIO);
        absorbedTotal += industriallegacy$nanoAbsorbFromSlot(self, EquipmentSlot.LEGS, amount, 0.30f * (float) NanoArmorItem.DAMAGE_ABSORPTION_RATIO);
        absorbedTotal += industriallegacy$nanoAbsorbFromSlot(self, EquipmentSlot.FEET, amount, 0.15f * (float) NanoArmorItem.DAMAGE_ABSORPTION_RATIO);

        return Math.max(0.0f, amount - Math.min(amount, absorbedTotal));
    }

    @Unique
    private static float industriallegacy$nanoAbsorbFromSlot(LivingEntity entity, EquipmentSlot slot, float originalDamage, float ratio) {
        if (ratio <= 0.0f || originalDamage <= 0.0f) return 0.0f;

        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof NanoArmorItem nano)) return 0.0f;

        float wantAbsorb = originalDamage * ratio;
        long charge = nano.getEnergy(stack);
        int energyPerDamage = NanoArmorItem.ENERGY_PER_DAMAGE;
        int damageLimit = energyPerDamage > 0 ? (int) Math.floor(25.0 * (double) charge / (double) energyPerDamage) : Integer.MAX_VALUE;
        float canAbsorb = Math.min(wantAbsorb, (float) damageLimit);
        if (canAbsorb <= 0.0f) return 0.0f;

        long energyCost = (long) Math.ceil(canAbsorb * (double) energyPerDamage);
        long drained = industriallegacy$nanoDrainIgnoreLimit(stack, energyCost);
        if (drained <= 0L) return 0.0f;

        double paidRatio = Math.min(1.0, (double) drained / (double) energyCost);
        return (float) (canAbsorb * paidRatio);
    }

    @Unique
    private static long industriallegacy$nanoDrainIgnoreLimit(ItemStack stack, long amount) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }
}
