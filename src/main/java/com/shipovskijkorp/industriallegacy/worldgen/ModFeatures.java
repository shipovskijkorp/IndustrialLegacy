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
 * Custom feature type registrations.
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
        // classload triggers static init
    }
}
