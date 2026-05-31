package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IL Quantum boots fall event: cancel all fall damage if charge covers max((int)distance - 10, 0).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityQuantumBootsFallMixin {
    @Inject(method = "handleFallDamage(FFLnet/minecraft/entity/damage/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    private void industriallegacy$quantumBootsFall(float fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack boots = self.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof QuantumArmorItem)) return;

        int fallDamage = Math.max((int) fallDistance - 10, 0);
        long energyCost = (long) QuantumArmorItem.ENERGY_PER_DAMAGE * (long) fallDamage;
        if (energyCost <= 0L) {
            cir.setReturnValue(false);
            return;
        }

        if (drainIgnoreLimit(boots, energyCost, true) >= energyCost) {
            drainIgnoreLimit(boots, energyCost, false);
            cir.setReturnValue(false);
        }
    }

    private static long drainIgnoreLimit(ItemStack stack, long amount, boolean simulate) {
        if (!(stack.getItem() instanceof IElectricItem ei)) return 0L;
        long stored = Math.max(0L, ei.getEnergy(stack));
        long extracted = Math.min(amount, stored);
        if (!simulate && extracted > 0L) {
            ei.setEnergy(stack, stored - extracted);
        }
        return extracted;
    }
}
