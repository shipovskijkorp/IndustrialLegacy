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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IL-like QuantumSuit absorption, wired into the armor reduction stage,
 * which is much closer to old Forge ISpecialArmor#getProperties behavior.
 *
 * Normal damage:
 * - head 0.15
 * - chest 0.40 * 1.2 = 0.48
 * - legs 0.30
 * - boots 0.15
 * Full set = 1.08
 *
 * Fall damage:
 * - boots 1.0
 * - leggings 0.8
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityQuantumArmorMixin {
    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void industriallegacy$applyQuantumArmorToArmorStage(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        if (amount <= 0.0f) {
            cir.setReturnValue(amount);
            return;
        }

        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            cir.setReturnValue(amount);
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        float absorbedTotal = 0.0f;

        if (source.isOf(DamageTypes.FALL)) {
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.FEET, amount, 1.0f);
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.LEGS, amount, 0.8f);
        } else {
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.HEAD, amount, 0.15f);
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.CHEST, amount, 0.48f);
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.LEGS, amount, 0.30f);
            absorbedTotal += industriallegacy$quantumAbsorbFromSlot(self, EquipmentSlot.FEET, amount, 0.15f);
        }

        float result = Math.max(0.0f, amount - Math.min(amount, absorbedTotal));
        cir.setReturnValue(result);
    }

    @Unique
    private static float industriallegacy$quantumAbsorbFromSlot(LivingEntity entity, EquipmentSlot slot, float originalDamage, float ratio) {
        if (ratio <= 0.0f || originalDamage <= 0.0f) return 0.0f;

        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof QuantumArmorItem quantum)) return 0.0f;

        float wantAbsorb = originalDamage * ratio;

        long charge = quantum.getEnergy(stack);
        int energyPerDamage = QuantumArmorItem.ENERGY_PER_DAMAGE;

        int damageLimit = (energyPerDamage > 0)
                ? (int) Math.floor(25.0 * (double) charge / (double) energyPerDamage)
                : Integer.MAX_VALUE;

        float canAbsorb = Math.min(wantAbsorb, (float) damageLimit);
        if (canAbsorb <= 0.0f) return 0.0f;

        long energyCost = (long) Math.ceil(canAbsorb * (double) energyPerDamage);
        long actuallyDrained = industriallegacy$quantumDrainIgnoreLimit(stack, energyCost);
        if (actuallyDrained <= 0L) return 0.0f;

        double paidRatio = Math.min(1.0, (double) actuallyDrained / (double) energyCost);
        return (float) (canAbsorb * paidRatio);
    }

    @Unique
    private static long industriallegacy$quantumDrainIgnoreLimit(ItemStack stack, long amount) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;

        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);

        if (extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }

        return extracted;
    }
}