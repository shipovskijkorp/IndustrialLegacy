package com.shipovskijkorp.industriallegacy;

import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import com.shipovskijkorp.industriallegacy.block.entity.CableBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.api.IEuEnergyStorage;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.registry.ModBlockEntities;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItemGroups;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.worldgen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Industrial Legacy (Fabric 1.20.1)
 */
public class IndustrialLegacy implements ModInitializer {
    public static final String MOD_ID = "industrial_legacy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModRecipes.register();
        ModScreenHandlers.register();
        ModWorldGen.register();
        ModItemGroups.register();
        ModPackets.registerServerReceivers();

        ServerTickEvents.END_WORLD_TICK.register(world -> EnergyNetLocal.get(world).onWorldTickEnd(world));

        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (chunk.getBlockEntities().values().stream().anyMatch(be -> be instanceof CableBlockEntity || be instanceof IEuEnergyStorage)) {
                EnergyNetLocal.get(world).invalidateAll();
            }
        });

        ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            if (chunk.getBlockEntities().values().stream().anyMatch(be -> be instanceof CableBlockEntity || be instanceof IEuEnergyStorage)) {
                EnergyNetLocal.get(world).invalidateAll();
            }
        });

        LOGGER.info("Industrial Legacy initialized");
    }
}
