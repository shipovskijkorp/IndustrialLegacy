package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Block;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;

import java.util.List;

/**
 * Runtime worldgen registration + biome injections.
 *
 * IMPORTANT: call ModWorldGen.register() from your mod initializer.
 */
public final class ModWorldGen {
    private ModWorldGen() {}

    private static final RuleTest STONE_REPLACEABLES = new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES);

    public static void register() {
        registerConfiguredIfMissing();
        OrePlacedFeatures.registerAll();
        injectBiomes();
        IndustrialLegacy.LOGGER.info("Worldgen registered (ores: tin/lead/uranium)");
    }

    private static void registerConfiguredIfMissing() {
        // Регистрируем configured-features, если их ещё нет.
        // ВАЖНО: блоки должны существовать в Registries.BLOCK к этому моменту (ModBlocks.register() раньше).
        registerOreConfiguredIfAbsent("tin_ore", 8);
        registerOreConfiguredIfAbsent("lead_ore", 8);
        registerOreConfiguredIfAbsent("uranium_ore", 6);
    }

    private static void registerOreConfiguredIfAbsent(String oreId, int veinSize) {
        Identifier id = new Identifier(IndustrialLegacy.MOD_ID, oreId);
        if (BuiltinRegistries.CONFIGURED_FEATURE.containsId(id)) {
            return; // уже есть
        }

        Block oreBlock = blockOrThrow(id);
        ConfiguredFeature<?, ?> configured = new ConfiguredFeature<>(
                Feature.ORE,
                new OreFeatureConfig(
                        List.of(OreFeatureConfig.createTarget(STONE_REPLACEABLES, oreBlock.getDefaultState())),
                        veinSize
                )
        );

        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, id, configured);
    }

    private static Block blockOrThrow(Identifier id) {
        if (!Registries.BLOCK.containsId(id)) {
            throw new IllegalStateException("Missing block for worldgen: " + id + " (register it in ModBlocks)");
        }
        return Registries.BLOCK.get(id);
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
    }
}
