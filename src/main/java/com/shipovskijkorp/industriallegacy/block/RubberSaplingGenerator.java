package com.shipovskijkorp.industriallegacy.block;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;

/**
 * Rubber sapling growth -> IC2-style rubber tree.
 */
public class RubberSaplingGenerator extends SaplingGenerator {
    public static final RegistryKey<ConfiguredFeature<?, ?>> RUBBER_TREE =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier(IndustrialLegacy.MOD_ID, "rubber_tree"));

    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        return RUBBER_TREE;
    }
}
