package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.heightprovider.YOffset;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;

import java.util.List;

/**
 * Registers ConfiguredFeature + PlacedFeature for IL ores.
 *
 * IMPORTANT:
 * - We intentionally do NOT register/generate "silver_ore" (IC2 doesn't have it as a block).
 * - Nickel/Bauxite/Sulfur are removed as requested.
 */
public final class OrePlacedFeatures {
    private OrePlacedFeatures() {}

    // ----- Configured keys -----
    private static final RegistryKey<ConfiguredFeature<?, ?>> TIN_ORE_CONFIGURED = configuredKey("tin_ore");
    private static final RegistryKey<ConfiguredFeature<?, ?>> LEAD_ORE_CONFIGURED = configuredKey("lead_ore");
    private static final RegistryKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED = configuredKey("uranium_ore");

    // ----- Placed keys -----
    public static final RegistryKey<PlacedFeature> TIN_ORE_PLACED = placedKey("tin_ore");
    public static final RegistryKey<PlacedFeature> LEAD_ORE_PLACED = placedKey("lead_ore");
    public static final RegistryKey<PlacedFeature> URANIUM_ORE_PLACED = placedKey("uranium_ore");

    private static RegistryKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    private static RegistryKey<PlacedFeature> placedKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    public static void register() {
        // counts/heights are just sane defaults to make it WORK; rebalance later.

        registerOre(
                TIN_ORE_CONFIGURED, TIN_ORE_PLACED,
                ModBlocks.TIN_ORE.getDefaultState(),
                8,
                oreModifiers(20, -64, 128)
        );

        registerOre(
                LEAD_ORE_CONFIGURED, LEAD_ORE_PLACED,
                ModBlocks.LEAD_ORE.getDefaultState(),
                8,
                oreModifiers(12, -64, 64)
        );

        registerOre(
                URANIUM_ORE_CONFIGURED, URANIUM_ORE_PLACED,
                ModBlocks.URANIUM_ORE.getDefaultState(),
                4,
                oreModifiers(2, -64, 32)
        );
    }

    private static List<PlacementModifier> oreModifiers(int veinsPerChunk, int minY, int maxY) {
        return List.of(
                CountPlacementModifier.of(veinsPerChunk),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(minY), YOffset.fixed(maxY)),
                BiomePlacementModifier.of()
        );
    }

    private static void registerOre(
            RegistryKey<ConfiguredFeature<?, ?>> configuredKey,
            RegistryKey<PlacedFeature> placedKey,
            net.minecraft.block.BlockState oreState,
            int veinSize,
            List<PlacementModifier> modifiers
    ) {
        Identifier configuredId = configuredKey.getValue();
        if (!Registries.CONFIGURED_FEATURE.containsId(configuredId)) {
            ConfiguredFeature<?, ?> configured = new ConfiguredFeature<>(
                    Feature.ORE,
                    new OreFeatureConfig(
                            List.of(OreFeatureConfig.createTarget(
                                    net.minecraft.world.gen.feature.OreConfiguredFeatures.STONE_ORE_REPLACEABLES,
                                    oreState
                            )),
                            veinSize
                    )
            );
            Registry.register(Registries.CONFIGURED_FEATURE, configuredId, configured);
        }

        Identifier placedId = placedKey.getValue();
        if (!Registries.PLACED_FEATURE.containsId(placedId)) {
            RegistryEntry<ConfiguredFeature<?, ?>> entry =
                    Registries.CONFIGURED_FEATURE.getEntry(configuredKey).orElseThrow();

            PlacedFeature placed = new PlacedFeature(entry, modifiers);
            Registry.register(Registries.PLACED_FEATURE, placedId, placed);
        }
    }
}
