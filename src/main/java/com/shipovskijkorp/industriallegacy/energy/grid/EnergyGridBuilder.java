package com.shipovskijkorp.industriallegacy.energy.grid;

import com.shipovskijkorp.industriallegacy.block.CableBlock;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Builds an {@link EnergyGrid} (connected component of cable blocks).
 *
 * <p>Splitter cables that are powered by redstone are treated as disconnected (not part of any grid),
 * matching the IC2 "load/unload" behavior.</p>
 */
final class EnergyGridBuilder {

    private EnergyGridBuilder() {}

    private static final int MAX_NODES = 4096;

    public static @Nullable EnergyGrid build(World world, BlockPos startCablePos, int gridId) {
        if (world == null || world.isClient) return null;

        BlockState startState = world.getBlockState(startCablePos);
        if (!ModBlocks.isCable(startState.getBlock())) return null;
        if (!(startState.getBlock() instanceof CableBlock startCable)) return null;
        if (isDisabledSplitter(world, startCablePos, startCable)) return null;

        long startKey = startCablePos.asLong();

        LongOpenHashSet cables = new LongOpenHashSet();
        ArrayDeque<Long> q = new ArrayDeque<>();
        cables.add(startKey);
        q.add(startKey);

        BlockPos.Mutable cur = new BlockPos.Mutable();
        BlockPos.Mutable nb = new BlockPos.Mutable();

        while (!q.isEmpty() && cables.size() < MAX_NODES) {
            long key = q.poll();
            cur.set(BlockPos.unpackLongX(key), BlockPos.unpackLongY(key), BlockPos.unpackLongZ(key));

            BlockState state = world.getBlockState(cur);
            if (!ModBlocks.isCable(state.getBlock())) continue;
            if (!(state.getBlock() instanceof CableBlock cable)) continue;
            if (isDisabledSplitter(world, cur, cable)) continue;

            for (Direction dir : Direction.values()) {
                nb.set(cur).move(dir);
                long nkey = nb.asLong();

                BlockState ns = world.getBlockState(nb);
                if (!ModBlocks.isCable(ns.getBlock())) continue;
                if (!(ns.getBlock() instanceof CableBlock nc)) continue;
                if (isDisabledSplitter(world, nb, nc)) continue;

                if (cables.add(nkey)) {
                    q.add(nkey);
                    if (cables.size() >= MAX_NODES) break;
                }
            }
        }

        // Validate + build adjacency and per-node data.
        LongOpenHashSet toRemove = new LongOpenHashSet();
        Long2ObjectOpenHashMap<long[]> neighbors = new Long2ObjectOpenHashMap<>();
        Long2DoubleOpenHashMap loss = new Long2DoubleOpenHashMap();
        Long2LongOpenHashMap capacity = new Long2LongOpenHashMap();

        LongIterator it = cables.iterator();
        while (it.hasNext()) {
            long key = it.nextLong();
            cur.set(BlockPos.unpackLongX(key), BlockPos.unpackLongY(key), BlockPos.unpackLongZ(key));

            BlockState state = world.getBlockState(cur);
            if (!ModBlocks.isCable(state.getBlock())) {
                toRemove.add(key);
                continue;
            }
            if (!(state.getBlock() instanceof CableBlock cable)) {
                toRemove.add(key);
                continue;
            }
            if (isDisabledSplitter(world, cur, cable)) {
                toRemove.add(key);
                continue;
            }

            loss.put(key, cable.getKind().loss);
            capacity.put(key, cable.getKind().capacity);

            LongArrayList nbList = new LongArrayList(6);
            for (Direction dir : Direction.values()) {
                nb.set(cur).move(dir);
                long nkey = nb.asLong();
                if (cables.contains(nkey)) {
                    nbList.add(nkey);
                }
            }
            neighbors.put(key, nbList.toLongArray());
        }

        if (!toRemove.isEmpty()) {
            LongIterator rit = toRemove.iterator();
            while (rit.hasNext()) {
                long key = rit.nextLong();
                cables.remove(key);
                neighbors.remove(key);
                loss.remove(key);
                capacity.remove(key);
            }
        }

        if (cables.isEmpty()) return null;

        return new EnergyGrid(gridId, cables, neighbors, loss, capacity);
    }

    private static boolean isDisabledSplitter(World world, BlockPos pos, CableBlock cable) {
        return cable.getKind() == CableKind.SPLITTER && world.isReceivingRedstonePower(pos);
    }
}
