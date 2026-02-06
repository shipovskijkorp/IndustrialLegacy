package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.energy.item.IElectricItem;
import com.shipovskijkorp.industriallegacy.item.armor.NanoArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IC2 Exp nano boots fall protection:
 * - if (fallDamage = (int)distance - 3) < 8 and enough energy (energyPerDamage * fallDamage)
 *   then discharge and cancel fall damage.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNanoBootsFallMixin {

    @Inject(method = "handleFallDamage(FFLnet/minecraft/entity/damage/DamageSource;)Z", at = @At("HEAD"), cancellable = true)
    private void industriallegacy$nanoBootsCancelSmallFall(float fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack boots = self.getEquippedStack(EquipmentSlot.FEET);
        if (!(boots.getItem() instanceof NanoArmorItem)) return;

        int fallDamage = (int) fallDistance - 3;
        if (fallDamage < 0) fallDamage = 0;

        if (fallDamage >= 8) return; // IC2: no cancel for 8+

        long energyCost = (long) NanoArmorItem.ENERGY_PER_DAMAGE * (long) fallDamage;
        if (energyCost <= 0L) return;

        long drained = drainIgnoreLimit(boots, energyCost);
        if (drained >= energyCost) {
            cir.setReturnValue(false); // cancel vanilla fall damage
        } else {
            // refund if partial (shouldn't happen often)
            if (drained > 0L && boots.getItem() instanceof IElectricItem ei) {
                ei.setEnergy(boots, ei.getEnergy(boots) + drained);
            }
        }
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
