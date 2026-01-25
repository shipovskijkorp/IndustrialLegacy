package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;
import net.minecraft.world.gen.placementmodifier.CountPlacementModifier;
import net.minecraft.world.gen.placementmodifier.HeightRangePlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;

import java.util.List;

/**
 * PlacedFeatures for IL ores (tin/lead/uranium).
 *
 * Register AFTER configured features are registered.
 */
public final class OrePlacedFeatures {
    private OrePlacedFeatures() {}

    // Placed feature keys (registry ids match the ore ids)
    public static final RegistryKey<PlacedFeature> TIN_ORE_PLACED = placedKey("tin_ore");
    public static final RegistryKey<PlacedFeature> LEAD_ORE_PLACED = placedKey("lead_ore");
    public static final RegistryKey<PlacedFeature> URANIUM_ORE_PLACED = placedKey("uranium_ore");

    // Configured feature keys (must exist in CONFIGURED_FEATURE registry)
    private static final RegistryKey<ConfiguredFeature<?, ?>> TIN_ORE_CONFIGURED = configuredKey("tin_ore");
    private static final RegistryKey<ConfiguredFeature<?, ?>> LEAD_ORE_CONFIGURED = configuredKey("lead_ore");
    private static final RegistryKey<ConfiguredFeature<?, ?>> URANIUM_ORE_CONFIGURED = configuredKey("uranium_ore");

    private static RegistryKey<PlacedFeature> placedKey(String name) {
        return RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    private static RegistryKey<ConfiguredFeature<?, ?>> configuredKey(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    public static void registerAll() {
        // Примерные распределения (потом подстроишь баланс как надо)
        // Tin: часто, средние высоты
        registerIfAbsent(
                TIN_ORE_PLACED,
                TIN_ORE_CONFIGURED,
                oreModifiersWithCount(14, -16, 96)
        );

        // Lead: чуть реже, ближе к низам
        registerIfAbsent(
                LEAD_ORE_PLACED,
                LEAD_ORE_CONFIGURED,
                oreModifiersWithCount(10, -32, 64)
        );

        // Uranium: редко, глубже
        registerIfAbsent(
                URANIUM_ORE_PLACED,
                URANIUM_ORE_CONFIGURED,
                oreModifiersWithCount(5, -64, 32)
        );
    }

    private static void registerIfAbsent(
            RegistryKey<PlacedFeature> placedKey,
            RegistryKey<ConfiguredFeature<?, ?>> configuredKey,
            List<PlacementModifier> modifiers
    ) {
        Identifier id = placedKey.getValue();
        if (BuiltinRegistries.PLACED_FEATURE.containsId(id)) {
            return; // уже зарегистрировано
        }

        RegistryEntry<ConfiguredFeature<?, ?>> configuredEntry =
                BuiltinRegistries.CONFIGURED_FEATURE.getEntry(configuredKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "Missing configured feature: " + configuredKey.getValue()
                        ));

        PlacedFeature placed = new PlacedFeature(configuredEntry, List.copyOf(modifiers));
        Registry.register(BuiltinRegistries.PLACED_FEATURE, id, placed);
    }

    private static List<PlacementModifier> oreModifiersWithCount(int veinsPerChunk, int minY, int maxY) {
        return List.of(
                CountPlacementModifier.of(veinsPerChunk),
                SquarePlacementModifier.of(),
                HeightRangePlacementModifier.uniform(YOffset.fixed(minY), YOffset.fixed(maxY)),
                BiomePlacementModifier.of()
        );
    }
}
