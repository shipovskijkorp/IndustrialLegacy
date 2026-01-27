package com.shipovskijkorp.industriallegacy;

import com.shipovskijkorp.industriallegacy.registry.ModItemGroups;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.worldgen.ModWorldGen;
import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
        ModItems.register();
        ModItemGroups.register();
        ModWorldGen.register();
        ModRecipes.register();

        // GUI (Generator/BatBox) — Step 1: register ScreenHandler types (scaffold; wired fully in later steps).
        ModScreenHandlers.register();

        // GUI button packets (e.g., BatBox redstone mode).
        ModPackets.registerServerReceivers();

        // EnergyNetLocal tick end hook (stats snapshot + over-voltage effects).
        ServerTickEvents.END_WORLD_TICK.register(world -> EnergyNetLocal.get(world).onWorldTickEnd(world));

        LOGGER.info("Industrial Legacy initialized");
    }
}
