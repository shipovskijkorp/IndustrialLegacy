package com.shipovskijkorp.industriallegacy;

import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.registry.ModItemGroups;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import com.shipovskijkorp.industriallegacy.registry.ModRecipes;
import com.shipovskijkorp.industriallegacy.registry.ModScreenHandlers;
import com.shipovskijkorp.industriallegacy.net.ModPackets;
import com.shipovskijkorp.industriallegacy.worldgen.ModWorldGen;
import com.shipovskijkorp.industriallegacy.energy.grid.EnergyNetLocal;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
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

        // Oxidation stage for copper uninsulated cable (0..1)
        ModelPredicateProviderRegistry.register(
                ModItems.CABLE,
                new Identifier(IndustrialLegacy.MOD_ID, "ox"),
                (ItemStack stack, ClientWorld world, LivingEntity entity, int seed) -> {
                    CableKind kind = CableItem.getKind(stack);
                    int ins = CableItem.getInsulation(stack);
                    if (kind != CableKind.COPPER || ins != 0) return 0.0f;
                    int ox = CableItem.getOxidation(stack); // 0..3
                    return ox / 3.0f; // 0.0, 0.333, 0.666, 1.0
                }
        );

        LOGGER.info("Industrial Legacy initialized");
    }
}
