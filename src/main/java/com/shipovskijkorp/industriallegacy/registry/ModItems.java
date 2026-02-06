package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.DebugWrenchItem;
import com.shipovskijkorp.industriallegacy.item.ReBatteryItem;
import com.shipovskijkorp.industriallegacy.item.TreetapItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Item registrations.
 *
 * Notes:
 * - Avoid duplicate registry IDs for the same concept.
 * - Some legacy constants are kept as aliases (NOT registered as separate items).
 */
public final class ModItems {
    private ModItems() {}

    // --- Tools / special items ---
    public static final Item CABLE = register("cable", new CableItem(new FabricItemSettings()));
    public static final Item DEBUG_WRENCH = register("debug_wrench", new DebugWrenchItem(new FabricItemSettings().maxCount(1)));

    public static final Item TREETAP = register("treetap", new TreetapItem(new FabricItemSettings().maxDamage(64)));
    public static final Item FORGE_HAMMER = register("forge_hammer", new Item(new FabricItemSettings().maxCount(1).maxDamage(79)));
    public static final Item CUTTER = register("cutter", new Item(new FabricItemSettings().maxCount(1).maxDamage(60)));

    // Electric items
    public static final Item RE_BATTERY = register("re_battery", new ReBatteryItem(new FabricItemSettings().maxCount(16)));

    // --- Basic materials / drops ---
    public static final Item STICKY_RESIN = register("sticky_resin", new Item(new FabricItemSettings()));
    public static final Item RUBBER = register("rubber", new Item(new FabricItemSettings()));

    /** IC2 semantics: sulfur is a dust item. */
    public static final Item SULFUR = register("sulfur", new Item(new Item.Settings()));
    /** Alias for clarity: in IC2, sulfur is a dust item. */
    public static final Item SULFUR_DUST = SULFUR;

    // --- Ingots ---
    public static final Item SILVER_INGOT = register("silver_ingot", new Item(new Item.Settings()));
    public static final Item TIN_INGOT = register("tin_ingot", new Item(new Item.Settings()));
    public static final Item LEAD_INGOT = register("lead_ingot", new Item(new Item.Settings()));
    public static final Item BRONZE_INGOT = register("bronze_ingot", new Item(new Item.Settings()));

    // --- Crushed ores (macerator output) ---
    // Canonical IDs
    public static final Item CRUSHED_IRON_ORE = register("crushed_iron_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_GOLD_ORE = register("crushed_gold_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_COPPER_ORE = register("crushed_copper_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_TIN_ORE = register("crushed_tin_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_LEAD_ORE = register("crushed_lead_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_URANIUM_ORE = register("crushed_uranium_ore", new Item(new Item.Settings()));

    /**
     * Silver was introduced earlier with ID silver_crushed_ore.
     * Keep it stable, but expose it under the same CRUSHED_* naming convention.
     */
    public static final Item CRUSHED_SILVER_ORE = register("silver_crushed_ore", new Item(new Item.Settings()));

    // --- Legacy aliases (do NOT register extra IDs) ---
    /** @deprecated use {@link #CRUSHED_COPPER_ORE} (id: crushed_copper_ore) */
    @Deprecated public static final Item COPPER_CRUSHED_ORE = CRUSHED_COPPER_ORE;
    /** @deprecated use {@link #CRUSHED_GOLD_ORE} (id: crushed_gold_ore) */
    @Deprecated public static final Item GOLD_CRUSHED_ORE = CRUSHED_GOLD_ORE;
    /** @deprecated use {@link #CRUSHED_IRON_ORE} (id: crushed_iron_ore) */
    @Deprecated public static final Item IRON_CRUSHED_ORE = CRUSHED_IRON_ORE;
    /** @deprecated use {@link #CRUSHED_LEAD_ORE} (id: crushed_lead_ore) */
    @Deprecated public static final Item LEAD_CRUSHED_ORE = CRUSHED_LEAD_ORE;
    /** @deprecated use {@link #CRUSHED_TIN_ORE} (id: crushed_tin_ore) */
    @Deprecated public static final Item TIN_CRUSHED_ORE = CRUSHED_TIN_ORE;
    /** @deprecated use {@link #CRUSHED_URANIUM_ORE} (id: crushed_uranium_ore) */
    @Deprecated public static final Item URANIUM_CRUSHED_ORE = CRUSHED_URANIUM_ORE;
    /** @deprecated use {@link #CRUSHED_SILVER_ORE} (id: silver_crushed_ore) */
    @Deprecated public static final Item SILVER_CRUSHED_ORE = CRUSHED_SILVER_ORE;

    // --- Dusts (IC2: dust.*) ---
    public static final Item BRONZE_DUST = register("bronze_dust", new Item(new Item.Settings()));
    public static final Item CLAY_DUST = register("clay_dust", new Item(new Item.Settings()));
    public static final Item COAL_DUST = register("coal_dust", new Item(new Item.Settings()));
    public static final Item COAL_FUEL_DUST = register("coal_fuel_dust", new Item(new Item.Settings()));
    public static final Item COPPER_DUST = register("copper_dust", new Item(new Item.Settings()));
    public static final Item DIAMOND_DUST = register("diamond_dust", new Item(new Item.Settings()));
    public static final Item ENERGIUM_DUST = register("energium_dust", new Item(new Item.Settings()));
    public static final Item GOLD_DUST = register("gold_dust", new Item(new Item.Settings()));
    public static final Item IRON_DUST = register("iron_dust", new Item(new Item.Settings()));
    public static final Item LAPIS_DUST = register("lapis_dust", new Item(new Item.Settings()));
    public static final Item LEAD_DUST = register("lead_dust", new Item(new Item.Settings()));
    public static final Item LITHIUM_DUST = register("lithium_dust", new Item(new Item.Settings()));
    public static final Item OBSIDIAN_DUST = register("obsidian_dust", new Item(new Item.Settings()));
    public static final Item SILICON_DIOXIDE = register("silicon_dioxide", new Item(new Item.Settings()));
    public static final Item SILVER_DUST = register("silver_dust", new Item(new Item.Settings()));
    public static final Item STONE_DUST = register("stone_dust", new Item(new Item.Settings()));
    public static final Item TIN_DUST = register("tin_dust", new Item(new Item.Settings()));
    public static final Item TIN_HYDRATED_DUST = register("tin_hydrated_dust", new Item(new Item.Settings()));

    // --- Small dusts (tiny piles) ---
    public static final Item SMALL_BRONZE_DUST = register("small_bronze_dust", new Item(new Item.Settings()));
    public static final Item SMALL_COPPER_DUST = register("small_copper_dust", new Item(new Item.Settings()));
    public static final Item SMALL_GOLD_DUST = register("small_gold_dust", new Item(new Item.Settings()));
    public static final Item SMALL_IRON_DUST = register("small_iron_dust", new Item(new Item.Settings()));
    public static final Item SMALL_LAPIS_DUST = register("small_lapis_dust", new Item(new Item.Settings()));
    public static final Item SMALL_LEAD_DUST = register("small_lead_dust", new Item(new Item.Settings()));
    public static final Item SMALL_LITHIUM_DUST = register("small_lithium_dust", new Item(new Item.Settings()));
    public static final Item SMALL_OBSIDIAN_DUST = register("small_obsidian_dust", new Item(new Item.Settings()));
    public static final Item SMALL_SILVER_DUST = register("small_silver_dust", new Item(new Item.Settings()));
    public static final Item SMALL_SULFUR_DUST = register("small_sulfur_dust", new Item(new Item.Settings()));
    public static final Item SMALL_TIN_DUST = register("small_tin_dust", new Item(new Item.Settings()));

    // --- IC2 crafting materials (needed for experimental recipes) ---
    public static final Item BIO_CHAFF = register("bio_chaff", new Item(new Item.Settings()));
    public static final Item PLANT_BALL = register("plant_ball", new Item(new Item.Settings()));
    public static final Item COAL_BALL = register("coal_ball", new Item(new Item.Settings()));
    public static final Item COAL_BLOCK = register("coal_block", new Item(new Item.Settings()));
    public static final Item COAL_CHUNK = register("coal_chunk", new Item(new Item.Settings()));
    public static final Item CARBON_FIBRE = register("carbon_fibre", new Item(new Item.Settings()));
    public static final Item CARBON_MESH = register("carbon_mesh", new Item(new Item.Settings()));
    public static final Item CARBON_PLATE = register("carbon_plate", new Item(new Item.Settings()));
    public static final Item TIN_CAN = register("tin_can", new Item(new Item.Settings()));

    // Crop-ish / misc (often referenced in IC2 experimental configs)
    public static final Item COFFEE_BEANS = register("coffee_beans", new Item(new Item.Settings()));
    public static final Item COFFEE_POWDER = register("coffee_powder", new Item(new Item.Settings()));
    public static final Item GRIN_POWDER = register("grin_powder", new Item(new Item.Settings()));
    public static final Item WEED = register("weed", new Item(new Item.Settings()));

    // --- Materials (IL resources; split into separate IDs in IL) ---
    public static final Item IRIDIUM_SHARD = register("iridium_shard", new Item(new Item.Settings()));

    public static final Item IRIDIUM = register("iridium", new Item(new Item.Settings()));
    public static final Item MIXED_METAL_INGOT = register("mixed_metal_ingot", new Item(new Item.Settings()));
    public static final Item ADVANCED_ALLOY = register("advanced_alloy", new Item(new Item.Settings()));
    public static final Item ELECTRONIC_CIRCUIT = register("electronic_circuit", new Item(new Item.Settings()));

    // Batteries (electric items)
    public static final Item ENERGY_CRYSTAL = register("energy_crystal", new com.shipovskijkorp.industriallegacy.item.EnergyCrystalItem(new Item.Settings().maxCount(1)));

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

    // Dense plates
    public static final Item DENSE_BRONZE_PLATE = register("dense_bronze_plate", new Item(new Item.Settings()));
    public static final Item DENSE_COPPER_PLATE = register("dense_copper_plate", new Item(new Item.Settings()));
    public static final Item DENSE_GOLD_PLATE = register("dense_gold_plate", new Item(new Item.Settings()));
    public static final Item DENSE_IRON_PLATE = register("dense_iron_plate", new Item(new Item.Settings()));
    public static final Item DENSE_LAPIS_PLATE = register("dense_lapis_plate", new Item(new Item.Settings()));
    public static final Item DENSE_LEAD_PLATE = register("dense_lead_plate", new Item(new Item.Settings()));
    public static final Item DENSE_OBSIDIAN_PLATE = register("dense_obsidian_plate", new Item(new Item.Settings()));
    public static final Item DENSE_STEEL_PLATE = register("dense_steel_plate", new Item(new Item.Settings()));
    public static final Item DENSE_TIN_PLATE = register("dense_tin_plate", new Item(new Item.Settings()));

    // Casings
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