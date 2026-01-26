package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.worldgen.feature.RubberTreeFeature;
import com.shipovskijkorp.industriallegacy.worldgen.feature.RubberTreePatchFeature;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * Custom worldgen Feature<?> registrations.
 *
 * These IDs are referenced by datapack JSON:
 * - data/industrial_legacy/worldgen/configured_feature/rubber_tree.json
 * - data/industrial_legacy/worldgen/configured_feature/rubber_tree_patch.json
 *
 * If they're not registered at runtime, Minecraft will crash during registry loading.
 */
public final class ModFeatures {
    private ModFeatures() {}

    public static final Feature<DefaultFeatureConfig> RUBBER_TREE =
            Registry.register(Registries.FEATURE, new Identifier(IndustrialLegacy.MOD_ID, "rubber_tree"),
                    new RubberTreeFeature(DefaultFeatureConfig.CODEC));

    public static final Feature<DefaultFeatureConfig> RUBBER_TREE_PATCH =
            Registry.register(Registries.FEATURE, new Identifier(IndustrialLegacy.MOD_ID, "rubber_tree_patch"),
                    new RubberTreePatchFeature(DefaultFeatureConfig.CODEC));

    public static void register() {
        // Trigger static init
    }
}
