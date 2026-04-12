package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.effect.RadiationStatusEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModStatusEffects {
    private ModStatusEffects() {}

    public static final StatusEffect RADIATION = Registry.register(
            Registries.STATUS_EFFECT,
            new Identifier(IndustrialLegacy.MOD_ID, "radiation"),
            new RadiationStatusEffect()
    );

    public static void register() {
        // classload triggers static init
    }
}
