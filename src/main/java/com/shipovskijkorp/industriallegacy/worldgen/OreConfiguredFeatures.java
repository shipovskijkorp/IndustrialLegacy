package com.shipovskijkorp.industriallegacy.worldgen;

import net.minecraft.world.gen.feature.OreConfiguredFeatures;
import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.OreConfiguredFeatures;

import java.util.List;

/**
 * ConfiguredFeatures for IL ores.
 *
 * Note: biome injection and placed features are registered elsewhere (PlacedFeatures / ModWorldGen).
 */
public final class OreConfiguredFeatures {
    private OreConfiguredFeatures() {}

    public static final RegistryKey<ConfiguredFeature<?, ?>> SILVER_ORE = key("silver_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> NICKEL_ORE = key("nickel_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> BAUXITE_ORE = key("bauxite_ore");
    public static final RegistryKey<ConfiguredFeature<?, ?>> SULFUR_ORE = key("sulfur_ore");

    private static RegistryKey<ConfiguredFeature<?, ?>> key(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(SILVER_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        net.minecraft.world.gen.feature.OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.SILVER_ORE.getDefaultState())),
                                8)));

        ctx.register(NICKEL_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        net.minecraft.world.gen.feature.OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.NICKEL_ORE.getDefaultState())),
                                8)));

        ctx.register(BAUXITE_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        net.minecraft.world.gen.feature.OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.BAUXITE_ORE.getDefaultState())),
                                16)));

        ctx.register(SULFUR_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        net.minecraft.world.gen.feature.OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                        ModBlocks.SULFUR_ORE.getDefaultState())),
                                7)));
    }
}
