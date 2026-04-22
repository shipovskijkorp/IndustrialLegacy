package com.shipovskijkorp.industriallegacy.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/**
 * IC2-like radiation effect.
 *
 * Damage formula mirrors IC2Potion#performEffect:
 * amplifier / 100 + 0.5 damage per tick.
 *
 * In original IC2 1.12.2 the HUD icon came from potion icon atlas index (6, 0).
 * In modern versions the equivalent is provided via textures/mob_effect/radiation.png.
 */
public final class RadiationStatusEffect extends StatusEffect {
    public RadiationStatusEffect() {
        super(StatusEffectCategory.HARMFUL, 5_149_489);
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.damage(entity.getDamageSources().magic(), amplifier / 100.0f + 0.5f);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        int rate = amplifier >= 31 ? 0 : (25 >> amplifier);
        return rate <= 0 || duration % rate == 0;
    }
}
