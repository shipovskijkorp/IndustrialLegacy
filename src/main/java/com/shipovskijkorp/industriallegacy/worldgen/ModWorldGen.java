package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

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

        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OrePlacedFeatures.SILVER_ORE_PLACED
        );

        // Silver ore gets a boost in high-mountain biomes (no Badlands/mesa extra generation).
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(
                        biomeKey("stony_peaks"),
                        biomeKey("jagged_peaks"),
                        biomeKey("frozen_peaks"),
                        biomeKey("snowy_slopes"),
                        biomeKey("grove"),
                        biomeKey("meadow")
                ),
                GenerationStep.Feature.UNDERGROUND_ORES,
                OrePlacedFeatures.SILVER_ORE_MOUNTAINS_PLACED
        );

        // Rubber trees (IL-style) — injected into overworld once per chunk at VEGETAL_DECORATION.
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.VEGETAL_DECORATION,
                RubberPlacedFeatures.RUBBER_TREE_PATCH
        );
    }


    private static RegistryKey<Biome> biomeKey(String path) {
        return RegistryKey.of(RegistryKeys.BIOME, new Identifier("minecraft", path));
    }
}
