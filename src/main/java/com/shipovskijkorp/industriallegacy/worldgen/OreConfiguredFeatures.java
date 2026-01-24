package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.PlacedFeature;

import java.util.List;

public class OreConfiguredFeatures {
    public static final RegistryKey<ConfiguredFeature<?, ?>> SILVER_ORE = key("silver_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> NICKEL_ORE = key("nickel_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> BAUXITE_ORE = key("bauxite_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SULFUR_ORE = key("sulfur_ore");
    private static final RegistryKey<ConfiguredFeature<?, ?>> RUBBER_TREE_CONFIGURED =
            configuredKey("rubber_tree");
    private static final RegistryKey<PlacedFeature> RUBBER_TREE_PLACED =
            placedKey("rubber_tree");


    private static RegistryKey<ConfiguredFeature<?, ?>> key(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE,
                new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(SILVER_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.SILVER_ORE.getDefaultState())),
                                8)));

        ctx.register(NICKEL_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.NICKEL_ORE.getDefaultState())),
                                8)));

        ctx.register(BAUXITE_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.BAUXITE_ORE.getDefaultState())),
                                16)));

        ctx.register(SULFUR_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.SULFUR_ORE.getDefaultState())),
                                7)));
        BiomeModifications.addFeature(
                BiomeSelectors.categories(Biome.Category.FOREST, Biome.Category.SWAMP),
                GenerationStep.Feature.VEGETAL_DECORATION,
                RUBBER_TREE_PLACED
        );

    }

}

