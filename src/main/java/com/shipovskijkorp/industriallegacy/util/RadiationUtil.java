package com.shipovskijkorp.industriallegacy.util;

import com.shipovskijkorp.industriallegacy.item.armor.HazmatArmorItem;
import com.shipovskijkorp.industriallegacy.registry.ModStatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Shared radiation helpers for radioactive items and armor.
 */
public final class RadiationUtil {
    private RadiationUtil() {}

    public static boolean isProtected(LivingEntity living) {
        return HazmatArmorItem.hasCompleteHazmat(living);
    }

    public static void clearIfProtected(LivingEntity living) {
        if (isProtected(living) && living.hasStatusEffect(ModStatusEffects.RADIATION)) {
            living.removeStatusEffect(ModStatusEffects.RADIATION);
        }
    }

    public static void apply(LivingEntity living, int durationTicks, int amplifier) {
        if (durationTicks <= 0 || amplifier < 0) return;
        if (isProtected(living)) {
            clearIfProtected(living);
            return;
        }

        StatusEffectInstance current = living.getStatusEffect(ModStatusEffects.RADIATION);
        int mergedDuration = Math.max(durationTicks, current == null ? 0 : current.getDuration());
        int mergedAmplifier = Math.max(amplifier, current == null ? 0 : current.getAmplifier());
        living.addStatusEffect(new StatusEffectInstance(ModStatusEffects.RADIATION, mergedDuration, mergedAmplifier, true, true));
    }
}
