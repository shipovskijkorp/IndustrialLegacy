package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.*;
import com.shipovskijkorp.industriallegacy.item.armor.ElectricJetpackItem;
import com.shipovskijkorp.industriallegacy.item.armor.StaticBootsItem;
import com.shipovskijkorp.industriallegacy.item.armor.SolarHelmetItem;
import com.shipovskijkorp.industriallegacy.item.armor.EnergyPackItem;
import com.shipovskijkorp.industriallegacy.item.armor.HazmatArmorItem;
import com.shipovskijkorp.industriallegacy.item.armor.ModArmorMaterials;
import com.shipovskijkorp.industriallegacy.item.armor.NanoArmorItem;
import com.shipovskijkorp.industriallegacy.item.armor.NanoHelmetItem;
import com.shipovskijkorp.industriallegacy.item.armor.NightVisionGogglesItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumBootsItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumChestplateItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumHelmetItem;
import com.shipovskijkorp.industriallegacy.item.armor.QuantumLeggingsItem;
import com.shipovskijkorp.industriallegacy.item.reactor.*;
import com.shipovskijkorp.industriallegacy.item.tool.BronzeToolMaterial;
import com.shipovskijkorp.industriallegacy.item.tool.ContainmentBoxItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricChainsawItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricDrillItem;
import com.shipovskijkorp.industriallegacy.item.tool.PainterItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricWrenchItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricTreetapItem;
import com.shipovskijkorp.industriallegacy.item.tool.ElectricHoeItem;
import com.shipovskijkorp.industriallegacy.item.tool.IridiumDrillItem;
import com.shipovskijkorp.industriallegacy.item.tool.MiningLaserItem;
import com.shipovskijkorp.industriallegacy.item.tool.NanoSaberItem;
import com.shipovskijkorp.industriallegacy.item.tool.ToolboxItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
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

    public static final Item TREETAP = register("treetap", new TreetapItem(new FabricItemSettings().maxDamage(64)));
    public static final Item FORGE_HAMMER = register("forge_hammer", new Item(new FabricItemSettings().maxCount(1).maxDamage(79)));
    public static final Item CUTTER = register("cutter", new Item(new FabricItemSettings().maxCount(1).maxDamage(60)));

    // IC2 electric tools
    public static final Item POWER_UNIT = register("power_unit", new Item(new Item.Settings()));
    public static final Item DRILL = register("drill", new ElectricDrillItem(new Item.Settings(), 50L, 2, 30_000L, 100L, 1, 8.0f));
    /** Alias for clarity: IC2 registry id is drill. */
    public static final Item MINING_DRILL = DRILL;
    public static final Item DIAMOND_DRILL = register("diamond_drill", new ElectricDrillItem(new Item.Settings(), 80L, 3, 30_000L, 100L, 1, 16.0f));
    public static final Item IRIDIUM_DRILL = register("iridium_drill", new IridiumDrillItem(new Item.Settings()));
    public static final Item CHAINSAW = register("chainsaw", new ElectricChainsawItem(new Item.Settings()));
    public static final Item ELECTRIC_TREETAP = register("electric_treetap", new ElectricTreetapItem(new Item.Settings()));
    public static final Item ELECTRIC_HOE = register("electric_hoe", new ElectricHoeItem(new Item.Settings()));
    public static final Item ELECTRIC_WRENCH = register("electric_wrench", new ElectricWrenchItem(new Item.Settings()));

    public static final Item PAINTER = register("painter", new PainterItem(new Item.Settings(), null));
    public static final Item PAINTER_WHITE = register("painter_white", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.WHITE));
    public static final Item PAINTER_ORANGE = register("painter_orange", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.ORANGE));
    public static final Item PAINTER_MAGENTA = register("painter_magenta", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.MAGENTA));
    public static final Item PAINTER_LIGHT_BLUE = register("painter_light_blue", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.LIGHT_BLUE));
    public static final Item PAINTER_YELLOW = register("painter_yellow", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.YELLOW));
    public static final Item PAINTER_LIME = register("painter_lime", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.LIME));
    public static final Item PAINTER_PINK = register("painter_pink", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.PINK));
    public static final Item PAINTER_GRAY = register("painter_gray", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.GRAY));
    public static final Item PAINTER_LIGHT_GRAY = register("painter_light_gray", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.LIGHT_GRAY));
    public static final Item PAINTER_CYAN = register("painter_cyan", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.CYAN));
    public static final Item PAINTER_PURPLE = register("painter_purple", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.PURPLE));
    public static final Item PAINTER_BLUE = register("painter_blue", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.BLUE));
    public static final Item PAINTER_BROWN = register("painter_brown", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.BROWN));
    public static final Item PAINTER_GREEN = register("painter_green", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.GREEN));
    public static final Item PAINTER_RED = register("painter_red", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.RED));
    public static final Item PAINTER_BLACK = register("painter_black", new PainterItem(new Item.Settings(), net.minecraft.util.DyeColor.BLACK));

    // Electric items
    public static final Item RE_BATTERY = register("re_battery", new ReBatteryItem(new FabricItemSettings().maxCount(16)));

    public static final Item ADVANCED_RE_BATTERY = register("advanced_re_battery", new AdvancedReBatteryItem(new FabricItemSettings().maxCount(16)));
    public static final Item CHARGING_RE_BATTERY = register("charging_re_battery", new ChargingBatteryItem(new FabricItemSettings().maxCount(16), 40_000L, 128L, 1));
    public static final Item ADVANCED_CHARGING_RE_BATTERY = register("advanced_charging_re_battery", new ChargingBatteryItem(new FabricItemSettings().maxCount(16), 400_000L, 1_024L, 2));
    public static final Item CHARGING_ENERGY_CRYSTAL = register("charging_energy_crystal", new ChargingBatteryItem(new FabricItemSettings().maxCount(16), 4_000_000L, 8_192L, 3));
    public static final Item CHARGING_LAPOTRON_CRYSTAL = register("charging_lapotron_crystal", new ChargingBatteryItem(new FabricItemSettings().maxCount(16), 40_000_000L, 32_768L, 4));

    // --- Basic materials / drops ---
    public static final Item STICKY_RESIN = register("sticky_resin", new StickyResinItem(new FabricItemSettings()));
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
    public static final Item STEEL_INGOT = register("steel_ingot", new Item(new Item.Settings()));
    public static final Item LITHIUM_INGOT = register("lithium_ingot", new Item(new Item.Settings()));


    // --- Crushed ores (macerator output) ---
    // Canonical IDs
    public static final Item CRUSHED_IRON_ORE = register("crushed_iron_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_GOLD_ORE = register("crushed_gold_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_COPPER_ORE = register("crushed_copper_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_TIN_ORE = register("crushed_tin_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_LEAD_ORE = register("crushed_lead_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_URANIUM_ORE = register("crushed_uranium_ore", new Item(new Item.Settings()));
    public static final Item CRUSHED_LITHIUM_ORE = register("crushed_lithium_ore", new Item(new Item.Settings()));


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

    // --- Purified crushed ores (ore washing output) ---
    public static final Item PURIFIED_COPPER_ORE = register("copper_purified_crushed_ore", new Item(new Item.Settings()));
    public static final Item PURIFIED_GOLD_ORE = register("gold_purified_crushed_ore", new Item(new Item.Settings()));
    public static final Item PURIFIED_IRON_ORE = register("iron_purified_crushed_ore", new Item(new Item.Settings()));
    public static final Item PURIFIED_LEAD_ORE = register("lead_purified_crushed_ore", new Item(new Item.Settings()));
    public static final Item PURIFIED_SILVER_ORE = register("silver_purified_crushed_ore", new Item(new Item.Settings()));
    public static final Item PURIFIED_TIN_ORE = register("tin_purified_crushed_ore", new Item(new Item.Settings()));

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
    public static final Item IODINE = register("iodine", new Item(new Item.Settings()));

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
    public static final Item CF_POWDER = register("cf_powder", new Item(new Item.Settings()));
    public static final Item PLANT_BALL = register("plant_ball", new Item(new Item.Settings()));
    public static final Item COAL_BALL = register("coal_ball", new Item(new Item.Settings()));
    public static final Item COAL_BLOCK = register("coal_block", new Item(new Item.Settings()));
    public static final Item COAL_CHUNK = register("coal_chunk", new Item(new Item.Settings()));
    public static final Item CARBON_FIBRE = register("carbon_fibre", new Item(new Item.Settings()));
    public static final Item CARBON_MESH = register("carbon_mesh", new Item(new Item.Settings()));
    public static final Item CARBON_PLATE = register("carbon_plate", new Item(new Item.Settings()));
    public static final Item TIN_CAN = register("tin_can", new Item(new Item.Settings()));
    public static final Item FILLED_TIN_CAN = register("filled_tin_can", new FilledTinCanItem(new Item.Settings().maxCount(64)));
    public static final Item SCRAP = register("scrap", new Item(new Item.Settings()));
    public static final Item SCRAP_BOX = register("scrap_box", new ScrapBoxItem(new Item.Settings()));
    public static final Item FERTILIZER = register("fertilizer", new Item(new Item.Settings()));
    public static final Item IRON_ROD = register("iron_rod", new Item(new Item.Settings()));
    public static final Item BRONZE_ROD = register("bronze_rod", new Item(new Item.Settings()));
    public static final Item STEEL_ROD = register("steel_rod", new Item(new Item.Settings()));
    public static final Item FUEL_ROD = register("fuel_rod", new Item(new Item.Settings()));
    public static final Item PURIFIED_URANIUM_ORE = register("purified_uranium_ore", new Item(new Item.Settings()));
    public static final Item URANIUM_238 = register("uranium_238", new RadioactiveItem(new Item.Settings(), 10 * 20, 90));
    public static final Item SMALL_URANIUM_235 = register("small_uranium_235", new RadioactiveItem(new Item.Settings(), 150 * 20, 100));
    public static final Item SMALL_URANIUM_238 = register("small_uranium_238", new RadioactiveItem(new Item.Settings(), 10 * 20, 90));
    public static final Item SMALL_PLUTONIUM = register("small_plutonium", new RadioactiveItem(new Item.Settings(), 150 * 20, 100));
    public static final Item URANIUM = register("uranium", new RadioactiveItem(new Item.Settings(), 60 * 20, 100));
    public static final Item URANIUM_235 = register("uranium_235", new RadioactiveItem(new Item.Settings(), 150 * 20, 100));
    public static final Item PLUTONIUM = register("plutonium", new RadioactiveItem(new Item.Settings(), 150 * 20, 100));
    public static final Item MOX = register("mox", new RadioactiveItem(new Item.Settings(), 300 * 20, 100));
    public static final Item URANIUM_PELLET = register("uranium_pellet", new RadioactiveItem(new Item.Settings(), 60 * 20, 100));
    public static final Item MOX_PELLET = register("mox_pellet", new RadioactiveItem(new Item.Settings(), 300 * 20, 100));
    public static final Item RTG_PELLET = register("rtg_pellet", new RadioactiveItem(new Item.Settings(), 2 * 20, 90));
    public static final Item DEPLETED_URANIUM_FUEL_ROD = register("depleted_uranium_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item DEPLETED_DUAL_URANIUM_FUEL_ROD = register("depleted_dual_uranium_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item DEPLETED_QUAD_URANIUM_FUEL_ROD = register("depleted_quad_uranium_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item DEPLETED_MOX_FUEL_ROD = register("depleted_mox_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item DEPLETED_DUAL_MOX_FUEL_ROD = register("depleted_dual_mox_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item DEPLETED_QUAD_MOX_FUEL_ROD = register("depleted_quad_mox_fuel_rod", new RadioactiveItem(new Item.Settings(), 10 * 20, 100));
    public static final Item NEAR_DEPLETED_URANIUM = register("near_depleted_uranium", new RadioactiveItem(new Item.Settings(), 15 * 20, 100));
    public static final Item RE_ENRICHED_URANIUM = register("re_enriched_uranium", new RadioactiveItem(new Item.Settings(), 30 * 20, 100));
    public static final Item URANIUM_FUEL_ROD = register("uranium_fuel_rod", new UraniumFuelRodItem(new Item.Settings(), 1, 20000, DEPLETED_URANIUM_FUEL_ROD));
    public static final Item DUAL_URANIUM_FUEL_ROD = register("dual_uranium_fuel_rod", new UraniumFuelRodItem(new Item.Settings(), 2, 20000, DEPLETED_DUAL_URANIUM_FUEL_ROD));
    public static final Item QUAD_URANIUM_FUEL_ROD = register("quad_uranium_fuel_rod", new UraniumFuelRodItem(new Item.Settings(), 4, 20000, DEPLETED_QUAD_URANIUM_FUEL_ROD));
    public static final Item MOX_FUEL_ROD = register("mox_fuel_rod", new MoxFuelRodItem(new Item.Settings(), 1, 10000, DEPLETED_MOX_FUEL_ROD));
    public static final Item DUAL_MOX_FUEL_ROD = register("dual_mox_fuel_rod", new MoxFuelRodItem(new Item.Settings(), 2, 10000, DEPLETED_DUAL_MOX_FUEL_ROD));
    public static final Item QUAD_MOX_FUEL_ROD = register("quad_mox_fuel_rod", new MoxFuelRodItem(new Item.Settings(), 4, 10000, DEPLETED_QUAD_MOX_FUEL_ROD));
    public static final Item LITHIUM_FUEL_ROD = register("lithium_fuel_rod", new LithiumFuelRodItem(new Item.Settings(), 10000));
    public static final Item TRITIUM_FUEL_ROD = register("tritium_fuel_rod", new RadioactiveItem(new Item.Settings(), 60 * 20, 100));
    public static final Item DEPLETED_ISOTOPE_FUEL_ROD = register("depleted_isotope_fuel_rod", new DepletedIsotopeFuelRodItem(new Item.Settings(), 10000));
    public static final Item COIL = register("coil", new Item(new Item.Settings()));
    public static final Item ELECTRIC_MOTOR = register("electric_motor", new Item(new Item.Settings()));

    // Crop-ish / misc (often referenced in IC2 experimental configs)
    public static final Item COFFEE_BEANS = register("coffee_beans", new Item(new Item.Settings()));
    public static final Item COFFEE_POWDER = register("coffee_powder", new Item(new Item.Settings()));
    public static final Item GRIN_POWDER = register("grin_powder", new Item(new Item.Settings()));
    public static final Item WEED = register("weed", new Item(new Item.Settings()));

    // --- Materials (IL resources; split into separate IDs in IL) ---
    public static final Item IRIDIUM_SHARD = register("iridium_shard", new Item(new Item.Settings()));

    // --- Reactor components (IC2) ---
    public static final Item HEAT_EXCHANGER = register("heat_exchanger", new HeatExchangerItem(new Item.Settings(), 2500, 12, 4));
    public static final Item REACTOR_HEAT_EXCHANGER = register("reactor_heat_exchanger", new HeatExchangerItem(new Item.Settings(), 5000, 0, 72));
    public static final Item COMPONENT_HEAT_EXCHANGER = register("component_heat_exchanger", new HeatExchangerItem(new Item.Settings(), 5000, 36, 0));
    public static final Item ADVANCED_HEAT_EXCHANGER = register("advanced_heat_exchanger", new HeatExchangerItem(new Item.Settings(), 10000, 24, 8));

    public static final Item HEAT_VENT = register("heat_vent", new ReactorVentItem(new Item.Settings(), 1000, 6, 0));
    public static final Item REACTOR_HEAT_VENT = register("reactor_heat_vent", new ReactorVentItem(new Item.Settings(), 1000, 5, 5));
    public static final Item COMPONENT_HEAT_VENT = register("component_heat_vent", new ReactorVentSpreadItem(new Item.Settings(), 4));
    public static final Item ADVANCED_HEAT_VENT = register("advanced_heat_vent", new ReactorVentItem(new Item.Settings(), 1000, 12, 0));
    public static final Item OVERCLOCKED_HEAT_VENT = register("overclocked_heat_vent", new ReactorVentItem(new Item.Settings(), 1000, 20, 36));
    public static final Item HEAT_STORAGE = register("heat_storage", new ReactorHeatStorageItem(new Item.Settings(), 10000));
    public static final Item TRI_HEAT_STORAGE = register("tri_heat_storage", new ReactorHeatStorageItem(new Item.Settings(), 30000));
    public static final Item HEX_HEAT_STORAGE = register("hex_heat_storage", new ReactorHeatStorageItem(new Item.Settings(), 60000));
    public static final Item REACTOR_PLATING = register("reactor_plating", new ReactorPlatingItem(new Item.Settings(), 1000, 0.95f));
    public static final Item HEAT_PLATING = register("heat_plating", new ReactorPlatingItem(new Item.Settings(), 2000, 0.99f));
    public static final Item CONTAINMENT_PLATING = register("containment_plating", new ReactorPlatingItem(new Item.Settings(), 500, 0.9f));
    public static final Item NEUTRON_REFLECTOR = register("neutron_reflector", new ReactorReflectorItem(new Item.Settings(), 30000));
    public static final Item THICK_NEUTRON_REFLECTOR = register("thick_neutron_reflector", new ReactorReflectorItem(new Item.Settings(), 120000));
    public static final Item IRIDIUM_REFLECTOR = register("iridium_reflector", new IridiumReflectorItem(new Item.Settings()));
    public static final Item RSH_CONDENSATOR = register("rsh_condensator", new ReactorCondensatorItem(new Item.Settings(), 20000));
    public static final Item LZH_CONDENSATOR = register("lzh_condensator", new ReactorCondensatorItem(new Item.Settings(), 100000));
    public static final Item CONTAINMENT_BOX = register("containment_box", new ContainmentBoxItem(new Item.Settings()));
    public static final Item TOOL_BOX = register("tool_box", new ToolboxItem(new Item.Settings()));


    public static final Item IRIDIUM = register("iridium", new Item(new Item.Settings()));
    public static final Item MIXED_METAL_INGOT = register("mixed_metal_ingot", new Item(new Item.Settings()));
    public static final Item ADVANCED_ALLOY = register("advanced_alloy", new Item(new Item.Settings()));
    public static final Item ELECTRONIC_CIRCUIT = register("electronic_circuit", new Item(new Item.Settings()));

    /** IC2: Advanced Circuit. */
    public static final Item ADVANCED_CIRCUIT = register("advanced_circuit", new Item(new Item.Settings()));

    public static final Item MFSU_UPGRADE_KIT = register("mfsu_upgrade_kit", new MfsuUpgradeKitItem(new Item.Settings().maxCount(16)));
    public static final Item WRENCH = register("wrench", new WrenchItem(new Item.Settings()));


    // Batteries (electric items)
    public static final Item ENERGY_CRYSTAL = register("energy_crystal", new com.shipovskijkorp.industriallegacy.item.EnergyCrystalItem(new Item.Settings().maxCount(1)));
    public static final Item LAPOTRON_CRYSTAL = register("lapotron_crystal", new com.shipovskijkorp.industriallegacy.item.LapotronCrystalItem(new Item.Settings().maxCount(1)));
    public static final Item FLUID_CELL = register("fluid_cell", new UniversalFluidCellItem(new Item.Settings().maxCount(64)));
    // Armor / utility
    public static final Item NIGHTVISION_GOGGLES = register("nightvision_goggles", new NightVisionGogglesItem(new Item.Settings().maxCount(1)));
    public static final Item JETPACK_ELECTRIC = register("jetpack_electric", new ElectricJetpackItem(new Item.Settings().maxCount(1)));
    public static final Item SOLAR_HELMET = register("solar_helmet", new SolarHelmetItem(new Item.Settings().maxCount(1)));
    public static final Item STATIC_BOOTS = register("static_boots", new StaticBootsItem(new Item.Settings().maxCount(1)));
    public static final Item ENERGY_PACK = register("energy_pack", new EnergyPackItem(new Item.Settings().maxCount(1)));
    public static final Item HAZMAT_HELMET = register("hazmat_helmet", new HazmatArmorItem(net.minecraft.item.ArmorItem.Type.HELMET, new Item.Settings().maxCount(1)));
    public static final Item HAZMAT_CHESTPLATE = register("hazmat_chestplate", new HazmatArmorItem(net.minecraft.item.ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1)));
    public static final Item HAZMAT_LEGGINGS = register("hazmat_leggings", new HazmatArmorItem(net.minecraft.item.ArmorItem.Type.LEGGINGS, new Item.Settings().maxCount(1)));
    public static final Item RUBBER_BOOTS = register("rubber_boots", new HazmatArmorItem(net.minecraft.item.ArmorItem.Type.BOOTS, new Item.Settings().maxCount(1)));

    // IC2 bronze armor
    public static final Item BRONZE_HELMET = register("bronze_helmet", new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.HELMET, new Item.Settings().maxCount(1)));
    public static final Item BRONZE_CHESTPLATE = register("bronze_chestplate", new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1)));
    public static final Item BRONZE_LEGGINGS = register("bronze_leggings", new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.LEGGINGS, new Item.Settings().maxCount(1)));
    public static final Item BRONZE_BOOTS = register("bronze_boots", new ArmorItem(ModArmorMaterials.BRONZE, ArmorItem.Type.BOOTS, new Item.Settings().maxCount(1)));

    // IC2 Composite Vest
    public static final Item ALLOY_CHESTPLATE = register("alloy_chestplate", new ArmorItem(ModArmorMaterials.ALLOY, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1)));

    // NanoSuit armor (IC2 Exp)
    public static final Item NANO_HELMET = register("nano_helmet", new NanoHelmetItem(new Item.Settings().maxCount(1)));
    public static final Item NANO_CHESTPLATE = register("nano_chestplate", new NanoArmorItem(net.minecraft.item.ArmorItem.Type.CHESTPLATE, new Item.Settings().maxCount(1)));
    public static final Item NANO_LEGGINGS = register("nano_leggings", new NanoArmorItem(net.minecraft.item.ArmorItem.Type.LEGGINGS, new Item.Settings().maxCount(1)));
    public static final Item NANO_BOOTS = register("nano_boots", new NanoArmorItem(net.minecraft.item.ArmorItem.Type.BOOTS, new Item.Settings().maxCount(1)));

    // QuantumSuit armor (IC2 Exp)
    public static final Item QUANTUM_HELMET = register("quantum_helmet", new QuantumHelmetItem(new Item.Settings().maxCount(1)));
    public static final Item QUANTUM_CHESTPLATE = register("quantum_chestplate", new QuantumChestplateItem(new Item.Settings().maxCount(1)));
    public static final Item QUANTUM_LEGGINGS = register("quantum_leggings", new QuantumLeggingsItem(new Item.Settings().maxCount(1)));
    public static final Item QUANTUM_BOOTS = register("quantum_boots", new QuantumBootsItem(new Item.Settings().maxCount(1)));

    /** IC2: Nano Saber (electric sword). */
    public static final Item NANO_SABER = register("nano_saber", new NanoSaberItem(new Item.Settings().maxCount(1)));
    /** IC2: Mining Laser (electric mining tool). */
    public static final Item MINING_LASER = register("mining_laser", new MiningLaserItem(new Item.Settings().maxCount(1)));
    // IC2 bronze tools
    public static final Item BRONZE_SWORD = register("bronze_sword", new SwordItem(BronzeToolMaterial.INSTANCE, 5, -2.4f, new Item.Settings()));
    public static final Item BRONZE_PICKAXE = register("bronze_pickaxe", new PickaxeItem(BronzeToolMaterial.INSTANCE, 1, -2.8f, new Item.Settings()));
    public static final Item BRONZE_SHOVEL = register("bronze_shovel", new ShovelItem(BronzeToolMaterial.INSTANCE, 1.5f, -3.0f, new Item.Settings()));
    public static final Item BRONZE_HOE = register("bronze_hoe", new HoeItem(BronzeToolMaterial.INSTANCE, 0, -3.0f, new Item.Settings()));
    public static final Item BRONZE_AXE = register("bronze_axe", new AxeItem(BronzeToolMaterial.INSTANCE, 8.0f, -3.1f, new Item.Settings()));
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
