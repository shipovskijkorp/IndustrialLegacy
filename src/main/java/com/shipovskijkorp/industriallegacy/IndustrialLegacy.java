package com.shipovskijkorp.industriallegacy;

import com.shipovskijkorp.industriallegacy.registry.ModItemGroups;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.worldgen.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Industrial Legacy (Fabric 1.20.1)
 */
public class IndustrialLegacy implements ModInitializer {
    public static final String MOD_ID = "industrial_legacy";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    //lol
    @Override
    public void onInitialize() {
        // Force-load and register all blocks early.
        // This prevents accidental registry lookups returning minecraft:air during other bootstrap stages.
        ModBlocks.register();

        ModItems.register();
        ModItemGroups.register();
        ModWorldGen.register();

        // GUI (Generator/BatBox) — Step 1: register ScreenHandler types (scaffold; wired fully in later steps).
        ModScreenHandlers.register();

        // GUI button packets (e.g., BatBox redstone mode).
        ModPackets.registerServerReceivers();

        LOGGER.info("Industrial Legacy initialized");
    }
}
