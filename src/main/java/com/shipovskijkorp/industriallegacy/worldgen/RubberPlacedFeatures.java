package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.PlacedFeature;

/** Placed feature keys for rubber tree world generation. */
public final class RubberPlacedFeatures {
    private RubberPlacedFeatures() {}

    public static final RegistryKey<PlacedFeature> RUBBER_TREE_PATCH =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, "rubber_tree_patch"));
}
