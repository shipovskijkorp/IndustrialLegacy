package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.DebugWrenchItem;
import com.shipovskijkorp.industriallegacy.item.ReBatteryItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.shipovskijkorp.industriallegacy.item.TreetapItem;


public final class ModItems {
    private ModItems() {}


    public static final Item CABLE = register("cable", new CableItem(new FabricItemSettings()));

    public static final Item DEBUG_WRENCH = register("debug_wrench", new DebugWrenchItem(new FabricItemSettings().maxCount(1)));

    public static final Item SILVER_INGOT =
            register("silver_ingot", new Item(new Item.Settings()));

    public static final Item TIN_INGOT =
            register("tin_ingot", new Item(new Item.Settings()));

    public static final Item LEAD_INGOT =
            register("lead_ingot", new Item(new Item.Settings()));

    // --- Crushed ores (macerator output) ---
    public static final Item CRUSHED_IRON_ORE =
            register("crushed_iron_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_GOLD_ORE =
            register("crushed_gold_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_COPPER_ORE =
            register("crushed_copper_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_TIN_ORE =
            register("crushed_tin_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_LEAD_ORE =
            register("crushed_lead_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_URANIUM_ORE =
            register("crushed_uranium_ore", new Item(new Item.Settings()));

    public static final Item SULFUR =
            register("sulfur", new Item(new Item.Settings()));
    public static final Item BRONZE_INGOT =
            register("bronze_ingot", new Item(new Item.Settings()));

    public static final Item STICKY_RESIN = register("sticky_resin",
            new Item(new FabricItemSettings()));

    public static final Item RUBBER = register("rubber",
            new Item(new FabricItemSettings()));

    public static final Item TREETAP = register("treetap",
            new TreetapItem(new FabricItemSettings().maxDamage(64)));

    public static final Item RE_BATTERY = register("re_battery",
            new ReBatteryItem(new FabricItemSettings().maxCount(16)));


    public static final Item FORGE_HAMMER = register("forge_hammer",
            new Item(new FabricItemSettings().maxCount(1).maxDamage(79)));

    public static final Item CUTTER = register("cutter",
            new Item(new FabricItemSettings().maxCount(1).maxDamage(60)));


    // --- Materials (IL resources; split into separate IDs in IL) ---

    public static final Item IRIDIUM =
            register("iridium", new Item(new Item.Settings()));

    public static final Item MIXED_METAL_INGOT =
            register("mixed_metal_ingot", new Item(new Item.Settings()));

    public static final Item ADVANCED_ALLOY =
            register("advanced_alloy", new Item(new Item.Settings()));

    public static final Item ELECTRONIC_CIRCUIT =
            register("electronic_circuit", new Item(new Item.Settings()));


    // Plates (normal)
    public static final Item BRONZE_PLATE = register("bronze_plate", new Item(new Item.Settings()));
    public static final Item COPPER_PLATE = register("copper_plate", new Item(new Item.Settings()));
    public static final Item GOLD_PLATE = register("gold_plate", new Item(new Item.Settings()));
    public static final Item IRON_PLATE = register("iron_plate", new Item(new Item.Settings()));
    public static final Item LAPIS_PLATE = register("lapis_plate", new Item(new Item.Settings()));
    public static final Item LEAD_PLATE = register("lead_plate", new Item(new Item.Settings()));
    public static final Item OBSIDIAN_PLATE = register("obsidian_plate", new Item(new Item.Settings()));
    public static final Item STEEL_PLATE = register("steel_plate", new Item(new Item.Settings()));
    public static final Item TIN_PLATE = register("tin_plate", new Item(new Item.Settings()));
    public static final Item IRIDIUM_PLATE = register("iridium_plate", new Item(new Item.Settings()));

    // Dense plates (IL: plate#dense_*)
    public static final Item DENSE_BRONZE_PLATE = register("dense_bronze_plate", new Item(new Item.Settings()));
    public static final Item DENSE_COPPER_PLATE = register("dense_copper_plate", new Item(new Item.Settings()));
    public static final Item DENSE_GOLD_PLATE = register("dense_gold_plate", new Item(new Item.Settings()));
    public static final Item DENSE_IRON_PLATE = register("dense_iron_plate", new Item(new Item.Settings()));
    public static final Item DENSE_LAPIS_PLATE = register("dense_lapis_plate", new Item(new Item.Settings()));
    public static final Item DENSE_LEAD_PLATE = register("dense_lead_plate", new Item(new Item.Settings()));
    public static final Item DENSE_OBSIDIAN_PLATE = register("dense_obsidian_plate", new Item(new Item.Settings()));
    public static final Item DENSE_STEEL_PLATE = register("dense_steel_plate", new Item(new Item.Settings()));
    public static final Item DENSE_TIN_PLATE = register("dense_tin_plate", new Item(new Item.Settings()));

    // Casings (IL: casing#*)
    public static final Item BRONZE_CASING = register("bronze_casing", new Item(new Item.Settings()));
    public static final Item COPPER_CASING = register("copper_casing", new Item(new Item.Settings()));
    public static final Item GOLD_CASING = register("gold_casing", new Item(new Item.Settings()));
    public static final Item IRON_CASING = register("iron_casing", new Item(new Item.Settings()));
    public static final Item LEAD_CASING = register("lead_casing", new Item(new Item.Settings()));
    public static final Item STEEL_CASING = register("steel_casing", new Item(new Item.Settings()));
    public static final Item TIN_CASING = register("tin_casing", new Item(new Item.Settings()));


    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(IndustrialLegacy.MOD_ID, path), item);
    }

    public static void register() {
        // classload triggers static init
    }
}