package com.shipovskijkorp.industriallegacy.worldgen;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.structure.rule.TagMatchRuleTest;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;

import java.util.List;

/**
 * ConfiguredFeatures for IL ores.
 *
 * Note: biome injection and placed features are registered elsewhere.
 */
public final class OreConfiguredFeatures {
    private OreConfiguredFeatures() {}
    public static final RegistryKey<ConfiguredFeature<?, ?>> LEAD_ORE = key("lead_ore");


    // Ванильный "STONE_ORE_REPLACEABLES" не используем, чтобы не упираться в конфликт имён.
    private static final RuleTest STONE_REPLACEABLES = new TagMatchRuleTest(BlockTags.STONE_ORE_REPLACEABLES);

    private static RegistryKey<ConfiguredFeature<?, ?>> key(String name) {
        return RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, name));
    }

    public static void bootstrap(Registerable<ConfiguredFeature<?, ?>> ctx) {

        ctx.register(LEAD_ORE,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreFeatureConfig(
                                List.of(OreFeatureConfig.createTarget(
                                        STONE_REPLACEABLES,
                                        ModBlocks.LEAD_ORE.getDefaultState())),
                                8
                        )));

    }
}
