package com.shipovskijkorp.industriallegacy.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.Random;

/**
 * Chunk-based rubber tree decorator (IL IlWorldDecorator.genRubberTree).
 *
 * This feature is injected into overworld biomes via placed_feature rubber_tree_patch.
 */
public class RubberTreePatchFeature extends Feature<DefaultFeatureConfig> {
    public RubberTreePatchFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        ChunkPos chunk = new ChunkPos(origin);

        long worldSeed = world.toServerWorld().getSeed();

        // IL: xSeed/zSeed derived from Random(worldSeed), shifted >> 3
        Random base = new Random(worldSeed);
        long xSeed = base.nextLong() >> 3;
        long zSeed = base.nextLong() >> 3;

        long chunkSeed = (xSeed * (long) chunk.x + zSeed * (long) chunk.z) ^ worldSeed;
        Random rnd = new Random(chunkSeed);

        long rubberTreeSeed = rnd.nextLong();
        RubberTreeGenerator.genRubberTreeChunk(world, chunk, rubberTreeSeed, 1.0f);

        return true;
    }
}
