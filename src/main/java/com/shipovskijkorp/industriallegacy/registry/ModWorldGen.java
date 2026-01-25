package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.worldgen.OrePlacedFeatures;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.gen.GenerationStep;

/**
 * Worldgen entrypoint.
 *
 * Make sure IndustrialLegacy.onInitialize() calls ModWorldGen.register().
 * If this method isn't called, you'll see "Applied 0 biome modifications" in logs.
 */
public final class ModWorldGen {
    private ModWorldGen() {}

    public static void register() {
        OrePlacedFeatures.register();
        registerBiomeFeatures();
    }

    private static void registerBiomeFeatures() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OrePlacedFeatures.TIN_ORE_PLACED
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OrePlacedFeatures.LEAD_ORE_PLACED
        );

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OrePlacedFeatures.URANIUM_ORE_PLACED
        );

        // NOTE:
        // Silver ore is intentionally NOT added anywhere (IC2 doesn't have it as a block).
        // Nickel/Bauxite/Sulfur are removed as requested.
    }
}
