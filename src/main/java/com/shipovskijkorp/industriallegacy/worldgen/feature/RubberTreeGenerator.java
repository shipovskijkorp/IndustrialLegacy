package com.shipovskijkorp.industriallegacy.worldgen.feature;

import com.shipovskijkorp.industriallegacy.block.RubberLogBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;

import java.util.Random;

/**
 * IC2-style rubber tree worldgen (WorldGenRubTree + Ic2WorldDecorator.genRubberTree).
 *
 * Notes:
 * - Config is intentionally empty (rules are fixed like IC2).
 * - Resin mechanics here only mark a boolean on the log (RubberLogBlock.RESIN).
 *   The full IC2 "wet side" directional holes are handled at the block/state level.
 */
public final class RubberTreeGenerator {
    private RubberTreeGenerator() {}

    // Vanilla biome tags (data/minecraft/tags/worldgen/biome/*.json)
    private static final TagKey<Biome> IS_SWAMP = TagKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "is_swamp"));
    private static final TagKey<Biome> IS_FOREST = TagKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "is_forest"));
    private static final TagKey<Biome> IS_JUNGLE = TagKey.of(RegistryKeys.BIOME, new Identifier("minecraft", "is_jungle"));

    /**
     * IC2: gen.generate(world, rnd, new BlockPos(randomX, seaLevel, randomZ)) where
     * WorldGenRubTree.generate internally offsets +8,+8.
     */
    public static boolean generateLikeIc2(StructureWorldAccess world, Random random, BlockPos pos) {
        int x = pos.getX() + 8;
        int z = pos.getZ() + 8;

        int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z);
        BlockPos base = new BlockPos(x, topY, z);
        return grow(world, base, random);
    }

    /** IC2 WorldGenRubTree.grow(...) */
    public static boolean grow(StructureWorldAccess world, BlockPos pos, Random random) {
        int height = getGrowHeight(world, pos);
        if (height < 2) return false;

        height -= random.nextInt(height / 2 + 1);

        BlockState leaves = ModBlocks.RUBBER_LEAVES.getDefaultState();
        BlockState logPlain = ModBlocks.RUBBER_LOG.getDefaultState().with(RubberLogBlock.RESIN, false);
        BlockState logResin = ModBlocks.RUBBER_LOG.getDefaultState().with(RubberLogBlock.RESIN, true);

        int treeHoleChance = 25;

        BlockPos.Mutable tmp = new BlockPos.Mutable();

        for (int cHeight = 0; cHeight < height; cHeight++) {
            BlockPos cPos = pos.up(cHeight);

            // Trunk block (with possible resin)
            if (random.nextInt(100) <= treeHoleChance) {
                treeHoleChance -= 10;
                world.setBlockState(cPos, logResin, Block.NOTIFY_ALL);
            } else {
                world.setBlockState(cPos, logPlain, Block.NOTIFY_ALL);
            }

            // Leaves layers
            if (height < 4 || (height < 7 && cHeight > 1) || cHeight > 2) {
                for (int cx = pos.getX() - 2; cx <= pos.getX() + 2; cx++) {
                    for (int cz = pos.getZ() - 2; cz <= pos.getZ() + 2; cz++) {
                        int chance = Math.max(1, cHeight + 4 - height);
                        int dx = Math.abs(cx - pos.getX());
                        int dz = Math.abs(cz - pos.getZ());

                        if ((dx <= 1 && dz <= 1) ||
                                (dx <= 1 && random.nextInt(chance) == 0) ||
                                (dz <= 1 && random.nextInt(chance) == 0)) {

                            tmp.set(cx, pos.getY() + cHeight, cz);
                            if (world.getBlockState(tmp).isAir()) {
                                world.setBlockState(tmp, leaves, Block.NOTIFY_ALL);
                            }
                        }
                    }
                }
            }
        }

        // Extra leaves on top
        for (int i = 0; i <= height / 4 + random.nextInt(2); i++) {
            tmp.set(pos.getX(), pos.getY() + height + i, pos.getZ());
            if (world.getBlockState(tmp).isAir()) {
                world.setBlockState(tmp, leaves, Block.NOTIFY_ALL);
            }
        }

        return true;
    }

    /** IC2 WorldGenRubTree.getGrowHeight(...) */
    private static int getGrowHeight(StructureWorldAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        BlockState base = world.getBlockState(below);

        // Close to vanilla "can sustain sapling": dirt-like blocks.
        if (!base.isIn(net.minecraft.registry.tag.BlockTags.DIRT)) {
            return 0;
        }

        // Allow sapling to be present one block above (sapling grow case),
        // otherwise require air above.
        BlockState up = world.getBlockState(pos.up());
        if (!up.isAir() && !up.isOf(ModBlocks.RUBBER_SAPLING)) {
            return 0;
        }

        int height = 1;
        BlockPos cur = pos.up();
        while (world.getBlockState(cur).isAir() && height < 8) {
            cur = cur.up();
            height++;
        }
        return height;
    }

    /**
     * IC2 Ic2WorldDecorator.genRubberTree(...) - chunk-based algorithm.
     *
     * @param baseScale default 1.0
     */
    @SuppressWarnings("unchecked")
    public static void genRubberTreeChunk(StructureWorldAccess world, ChunkPos chunk, long rubberTreeSeed, float baseScale) {
        Random rnd = new Random();
        rnd.setSeed(rubberTreeSeed);

        // Sample 4 biomes in chunk (8/23 offset)
        RegistryEntry<Biome>[] biomes = new RegistryEntry[4];
        for (int i = 0; i < 4; i++) {
            int x = chunk.getStartX() + 8 + (i & 0x1) * 15;
            int z = chunk.getStartZ() + 8 + ((i & 0x2) >>> 1) * 15;
            BlockPos p = new BlockPos(x, world.getSeaLevel(), z);
            biomes[i] = world.getBiome(p);
        }

        int rubberTrees = 0;
        for (RegistryEntry<Biome> biome : biomes) {
            if (biome == null) continue;

            if (biome.isIn(IS_SWAMP)) {
                rubberTrees += rnd.nextInt(10) + 5;
            }
            if (biome.isIn(IS_FOREST) || biome.isIn(IS_JUNGLE)) {
                rubberTrees += rnd.nextInt(5) + 1;
            }
        }

        rubberTrees = Math.round(rubberTrees * baseScale);
        rubberTrees /= 2;

        if (rubberTrees > 0 && rnd.nextInt(100) < rubberTrees) {
            for (int j = 0; j < rubberTrees; j++) {
                int x = chunk.getStartX() + rnd.nextInt(16);
                int z = chunk.getStartZ() + rnd.nextInt(16);

                if (!generateLikeIc2(world, rnd, new BlockPos(x, world.getSeaLevel(), z))) {
                    rubberTrees -= 3;
                }
            }
        }
    }
}
