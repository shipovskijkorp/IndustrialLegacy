package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeConfiguredFeatures;

import java.util.Random;

public class RubberSaplingGenerator extends SaplingGenerator {
    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        // пока используем ванильный дуб; позже можно заменить на свой RUBBER_TREE_CONFIGURED
        return TreeConfiguredFeatures.OAK;
    }
}
