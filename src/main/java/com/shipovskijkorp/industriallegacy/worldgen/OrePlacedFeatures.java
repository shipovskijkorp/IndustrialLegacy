package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Registry keys for IL ore placed features.
 *
 * <p>In 1.20.1, placed/configured features are part of the dynamic registries and are typically
 * supplied via datapack JSON. These keys are referenced from {@link ModWorldGen} for biome
 * injection.
 */
public final class OrePlacedFeatures {
    private OrePlacedFeatures() {}

    public static final RegistryKey<PlacedFeature> TIN_ORE_PLACED = key("tin_ore");
    public static final RegistryKey<PlacedFeature> LEAD_ORE_PLACED = key("lead_ore");
    public static final RegistryKey<PlacedFeature> URANIUM_ORE_PLACED = key("uranium_ore");

    private static RegistryKey<PlacedFeature> key(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }
}
