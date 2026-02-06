package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.item.armor.NanoArmorItem;
import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * IC2-like NanoSuit damage absorption:
 * - total absorption ratio = sum(slotBaseRatio) * 0.9
 * - consumes ENERGY_PER_DAMAGE EU per damage absorbed (ignoring transfer limit)
 * - if charge is insufficient, that piece contributes less / none
 *
 * Notes:
 * - We keep vanilla armor protection at 0 for Nano armor material to avoid double reduction.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNanoArmorMixin {

    @ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD"), argsOnly = true)
    private float industriallegacy$applyNanoArmorAbsorption(float amount, DamageSource source) {
        if (amount <= 0.0f) return amount;

        // IC2: unblockable sources bypass armor
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) return amount;

        LivingEntity self = (LivingEntity) (Object) this;

        float remaining = amount;
        float absorbedTotal = 0.0f;

        // Apply per slot (IC2 base ratios)
        absorbedTotal += absorbFromSlot(self, EquipmentSlot.HEAD, 0.15f, remaining);
        remaining = amount - absorbedTotal;

        absorbedTotal += absorbFromSlot(self, EquipmentSlot.CHEST, 0.40f, remaining);
        remaining = amount - absorbedTotal;

        absorbedTotal += absorbFromSlot(self, EquipmentSlot.LEGS, 0.30f, remaining);
        remaining = amount - absorbedTotal;

        absorbedTotal += absorbFromSlot(self, EquipmentSlot.FEET, 0.15f, remaining);
        remaining = amount - absorbedTotal;

        // Return reduced damage
        float out = amount - absorbedTotal;
        if (out < 0.0f) out = 0.0f;
        return out;
    }

    private static float absorbFromSlot(LivingEntity entity, EquipmentSlot slot, float baseRatio, float remainingDamage) {
        if (remainingDamage <= 0.0f) return 0.0f;

        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof NanoArmorItem nano)) return 0.0f;

        // IC2: absorptionRatio = baseRatio * 0.9
        float ratio = (float) (baseRatio * NanoArmorItem.DAMAGE_ABSORPTION_RATIO);

        // How much this piece would absorb from remaining damage
        float wantAbsorb = remainingDamage * ratio;
        if (wantAbsorb <= 0.0f) return 0.0f;

        // IC2: damageLimit = floor(25 * charge / energyPerDamage)
        long charge = nano.getEnergy(stack);
        int energyPerDamage = NanoArmorItem.ENERGY_PER_DAMAGE;
        int damageLimit = (energyPerDamage > 0) ? (int) Math.floor(25.0 * (double) charge / (double) energyPerDamage) : Integer.MAX_VALUE;

        float canAbsorbByCharge = Math.min(wantAbsorb, (float) damageLimit);
        if (canAbsorbByCharge <= 0.0f) return 0.0f;

        long energyCost = (long) Math.ceil(canAbsorbByCharge * (double) energyPerDamage);
        long actuallyDrained = drainIgnoreLimit(stack, energyCost);

        if (actuallyDrained <= 0L) return 0.0f;

        // If we couldn't pay full cost, scale absorbed damage down accordingly.
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
