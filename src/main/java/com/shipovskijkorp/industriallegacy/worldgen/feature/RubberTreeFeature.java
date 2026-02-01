package com.shipovskijkorp.industriallegacy.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Random;

/**
 * Single rubber tree generation feature (used by sapling growth).
 *
 * Config is empty (DefaultFeatureConfig) by design: IL has fixed rules.
 */
public class RubberTreeFeature extends Feature<DefaultFeatureConfig> {
    public RubberTreeFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();

        // Use deterministic java.util.Random derived from MC random.
        Random rnd = new Random(context.getRandom().nextLong());
        return RubberTreeGenerator.grow(world, origin, rnd);
    }
}
