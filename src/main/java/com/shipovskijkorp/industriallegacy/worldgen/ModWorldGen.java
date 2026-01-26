package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.gen.GenerationStep;

/**
 * Runtime worldgen registration + biome injections.
 *
 * IMPORTANT: call ModWorldGen.register() from your mod initializer.
 */
public final class ModWorldGen {
    private ModWorldGen() {}

    public static void register() {
        // Custom Feature<?> types must be registered before datapack configured/placed features are parsed,
        // otherwise Minecraft will crash with "Unknown registry key in worldgen/feature".
        ModFeatures.register();

        injectBiomes();

        // Note: configured/placed features are supplied via datapack JSON in 1.20.1.
        // This class only wires them into biomes.
        IndustrialLegacy.LOGGER.info("Worldgen hooks registered (ores + rubber trees)");
    }

    private static void injectBiomes() {
        // Все три руды — в оверворлд на стадии UNDERGROUND_ORES
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

        // Rubber trees (IC2-style) — injected into overworld once per chunk at VEGETAL_DECORATION.
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.VEGETAL_DECORATION,
                RubberPlacedFeatures.RUBBER_TREE_PATCH
        );
    }
}
