package com.shipovskijkorp.industriallegacy.mixin;

import com.shipovskijkorp.industriallegacy.item.armor.HazmatArmorItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import net.minecraft.entity.damage.DamageTypes;

/**
 * IC2-like hazmat suit absorption and rubber boots fall protection.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHazmatArmorMixin {
    @ModifyVariable(method = "damage(Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"), argsOnly = true)
    private float industriallegacy$applyHazmatAbsorption(float amount, DamageSource source) {
        if (amount <= 0.0f) return amount;

        LivingEntity self = (LivingEntity) (Object) this;
        int pieceCount = countHazmatPieces(self);
        if (pieceCount <= 0) return amount;

        boolean hazardSource = isHazmatAbsorbSource(self, source);
        boolean fullSuit = HazmatArmorItem.hasCompleteHazmat(self);

        if (fullSuit && hazardSource) {
            if (isFireLike(self, source)) {
                self.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 60, 1, true, true));
            }
            return 0.0f;
        }

        if (!hazardSource && source.isIn(DamageTypeTags.BYPASSES_ARMOR)) {
            return amount;
        }

        if (source.isOf(DamageTypes.FALL)) {
            ItemStack boots = self.getEquippedStack(EquipmentSlot.FEET);
            if (boots.getItem() instanceof HazmatArmorItem) {
                int intDamage = Math.max(0, Math.round(amount));
                float remaining = amount;
                float bootRatio = (intDamage < 8) ? 1.0f : 0.875f;
                remaining *= (1.0f - bootRatio);
                damagePiece(self, EquipmentSlot.FEET, Math.max(0, (intDamage + 1) / 2));

                int otherPieces = pieceCount - 1;
                if (otherPieces > 0) {
                    remaining *= Math.max(0.0f, 1.0f - otherPieces * 0.05f);
                    damageNonBootPieces(self, Math.max(1, intDamage * 2));
                }
                return Math.max(0.0f, remaining);
            }
        }

        float ratio = Math.min(1.0f, pieceCount * 0.05f);
        int durabilityDamage = Math.max(1, Math.round(amount * 2.0f));
        damageAllHazmatPieces(self, durabilityDamage);
        return Math.max(0.0f, amount * (1.0f - ratio));
    }

    private static boolean isFireLike(LivingEntity entity, DamageSource source) {
        return source.isOf(DamageTypes.IN_FIRE)
                || source.isOf(DamageTypes.LAVA)
                || source.isOf(DamageTypes.HOT_FLOOR)
                || source.isOf(DamageTypes.ON_FIRE);
    }

    private static boolean isHazmatAbsorbSource(LivingEntity entity, DamageSource source) {
        if (source.isOf(DamageTypes.IN_FIRE)) return true;
        if (source.isOf(DamageTypes.IN_WALL)) return true;
        if (source.isOf(DamageTypes.LAVA)) return true;
        if (source.isOf(DamageTypes.HOT_FLOOR)) return true;
        if (source.isOf(DamageTypes.ON_FIRE)) return true;
        if (source.isOf(DamageTypes.LIGHTNING_BOLT)) return true;
        return false;
    }

    private static int countHazmatPieces(LivingEntity entity) {
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getEquippedStack(slot).getItem() instanceof HazmatArmorItem) count++;
        }
        return count;
    }

    private static void damageAllHazmatPieces(LivingEntity entity, int amount) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            damagePiece(entity, slot, amount);
        }
    }

    private static void damageNonBootPieces(LivingEntity entity, int amount) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            damagePiece(entity, slot, amount);
        }
    }

    private static void damagePiece(LivingEntity entity, EquipmentSlot slot, int amount) {
        if (amount <= 0) return;
        ItemStack stack = entity.getEquippedStack(slot);
        if (!(stack.getItem() instanceof HazmatArmorItem)) return;
        stack.damage(amount, entity, e -> e.sendEquipmentBreakStatus(slot));
    }
}
