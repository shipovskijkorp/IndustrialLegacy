package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import com.shipovskijkorp.industriallegacy.item.EnergyMachineBlockItem;
import com.shipovskijkorp.industriallegacy.item.armor.BiogasJetpackItem;
import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;
import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final ItemGroup MAIN = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(IndustrialLegacy.MOD_ID, "main"),
            FabricItemGroup.builder()
                    // Icon: copper cable (uninsulated)
                    .icon(() -> {
                        ItemStack stack = new ItemStack(ModItems.MINING_LASER);
                        ElectricItemManager.setEnergy(stack, ElectricItemManager.getCapacity(stack));
                        return stack;
                    })
                    .displayName(Text.translatable("itemGroup.industrial_legacy.main"))
                    .entries((ctx, entries) -> {
                        // Single tab, but with a stable, readable order (no random registry ordering).
                        // Anything missed is appended at the end, sorted by id.

                        Set<net.minecraft.item.Item> added = new HashSet<>();

                        java.util.function.Consumer<ItemConvertible> add = (it) -> {
                            net.minecraft.item.Item item = it.asItem();
                            if (added.add(item)) {
                                entries.add(it);
                            }
                        };

                        java.util.function.Consumer<ItemConvertible> addCharged = (it) -> {
                            ItemStack stack = new ItemStack(it);
                            if (stack.getItem() instanceof com.shipovskijkorp.industriallegacy.energy.item.IElectricItem) {
                                ElectricItemManager.setEnergy(stack, ElectricItemManager.getCapacity(stack));
                                entries.add(stack);
                            }
                        };

                        java.util.function.Consumer<ItemConvertible> addChargedBlock = (it) -> {
                            ItemStack stack = new ItemStack(it);
                            if (stack.getItem() instanceof EnergyMachineBlockItem energyBlockItem && energyBlockItem.isChargeable()) {
                                energyBlockItem.setStoredEnergy(stack, energyBlockItem.getChargeCapacity());
                                entries.add(stack);
                            }
                        };

                        // ------------------------------
                        // Machines / energy blocks
                        // ------------------------------
                        add.accept(ModBlocks.GENERATOR);
                        add.accept(ModBlocks.GEO_GENERATOR);
                        add.accept(ModBlocks.SOLAR_PANEL);
                        add.accept(ModBlocks.RT_GENERATOR);
                        add.accept(ModBlocks.SEMIFLUID_GENERATOR);
                        add.accept(ModBlocks.BATBOX);
                        add.accept(ModBlocks.CESU);
                        add.accept(ModBlocks.MFE);
                        add.accept(ModBlocks.MFSU);
                        add.accept(ModBlocks.CHARGEPAD_BATBOX);
                        add.accept(ModBlocks.CHARGEPAD_CESU);
                        add.accept(ModBlocks.CHARGEPAD_MFE);
                        add.accept(ModBlocks.CHARGEPAD_MFSU);
                        add.accept(ModBlocks.LV_TRANSFORMER);
                        addChargedBlock.accept(ModBlocks.BATBOX);
                        addChargedBlock.accept(ModBlocks.CESU);
                        addChargedBlock.accept(ModBlocks.MFE);
                        addChargedBlock.accept(ModBlocks.MFSU);
                        addChargedBlock.accept(ModBlocks.CHARGEPAD_BATBOX);
                        addChargedBlock.accept(ModBlocks.CHARGEPAD_CESU);
                        addChargedBlock.accept(ModBlocks.CHARGEPAD_MFE);
                        addChargedBlock.accept(ModBlocks.CHARGEPAD_MFSU);
                        add.accept(ModBlocks.IRON_FURNACE);
                        add.accept(ModBlocks.NUCLEAR_REACTOR);
                        add.accept(ModBlocks.REACTOR_CHAMBER);
                        add.accept(ModBlocks.ELECTRIC_FURNACE);
                        add.accept(ModBlocks.INDUCTION_FURNACE);
                        add.accept(ModBlocks.MACERATOR);
                        add.accept(ModBlocks.COMPRESSOR);
                        add.accept(ModBlocks.EXTRACTOR);
                        add.accept(ModBlocks.RECYCLER);
                        add.accept(ModBlocks.METAL_FORMER);
                        add.accept(ModBlocks.SOLID_CANNER);
                        add.accept(ModBlocks.CANNER);
                        add.accept(ModBlocks.FLUID_BOTTLER);
                        add.accept(ModBlocks.PUMP);
                        add.accept(ModBlocks.SOLAR_DISTILLER);
                        add.accept(ModBlocks.THERMAL_CENTRIFUGE);
                        add.accept(ModBlocks.ORE_WASHING_PLANT);
                        add.accept(ModBlocks.MACHINE_CASING);
                        add.accept(ModBlocks.LEAD_BLOCK);
                        add.accept(ModBlocks.BRONZE_BLOCK);
                        add.accept(ModBlocks.TIN_BLOCK);
                        add.accept(ModBlocks.STEEL_BLOCK);
                        add.accept(ModBlocks.SILVER_BLOCK);
                        add.accept(ModBlocks.IRON_FENCE);
                        add.accept(ModBlocks.WOODEN_STORAGE_BOX);
                        add.accept(ModBlocks.IRON_STORAGE_BOX);
                        add.accept(ModBlocks.BRONZE_STORAGE_BOX);
                        add.accept(ModBlocks.STEEL_STORAGE_BOX);
                        add.accept(ModBlocks.IRIDIUM_STORAGE_BOX);

                        // Reinforced building block
                        add.accept(ModBlocks.REINFORCED_GLASS);
                        add.accept(ModBlocks.RESIN_SHEET);
                        add.accept(ModBlocks.RUBBER_SHEET);
                        add.accept(ModBlocks.WOOL_SHEET);
                        add.accept(ModBlocks.SCAFFOLD);
                        add.accept(ModBlocks.REINFORCED_SCAFFOLD);
                        add.accept(ModBlocks.IRON_SCAFFOLD);
                        add.accept(ModBlocks.REINFORCED_IRON_SCAFFOLD);

                        // Lighting
                        add.accept(ModBlocks.LUMINATOR);

                        // ------------------------------
                        // World resources / rubber tree
                        // ------------------------------
                        add.accept(ModBlocks.TIN_ORE);
                        add.accept(ModBlocks.LEAD_ORE);
                        add.accept(ModBlocks.URANIUM_ORE);
                        add.accept(ModBlocks.SILVER_ORE);
                        add.accept(ModBlocks.LITHIUM_ORE);

                        add.accept(ModBlocks.DEEPSLATE_TIN_ORE);
                        add.accept(ModBlocks.DEEPSLATE_LEAD_ORE);
                        add.accept(ModBlocks.DEEPSLATE_URANIUM_ORE);
                        add.accept(ModBlocks.DEEPSLATE_SILVER_ORE);
                        add.accept(ModBlocks.DEEPSLATE_LITHIUM_ORE);

                        add.accept(ModBlocks.RUBBER_LOG);
                        add.accept(ModBlocks.RUBBER_LEAVES);
                        add.accept(ModBlocks.RUBBER_SAPLING);

                        // ------------------------------
                        // Cables (variants only)
                        // ------------------------------
                        for (ItemStack stack : CableVariants.createAll(ModItems.CABLE)) {
                            entries.add(stack);
                        }
                        added.add(ModItems.CABLE); // prevent adding the blank cable item later

                        // ------------------------------
                        // Tools / electric items
                        // ------------------------------
                        add.accept(ModItems.TREETAP);
                        add.accept(ModItems.FORGE_HAMMER);
                        add.accept(ModItems.CUTTER);
                        add.accept(ModItems.WRENCH);
                        add.accept(ModItems.POWER_UNIT);
                        add.accept(ModItems.ELECTRIC_TREETAP);
                        add.accept(ModItems.ELECTRIC_HOE);
                        add.accept(ModItems.ELECTRIC_WRENCH);
                        add.accept(ModItems.PAINTER);
                        add.accept(ModItems.PAINTER_WHITE);
                        add.accept(ModItems.PAINTER_ORANGE);
                        add.accept(ModItems.PAINTER_MAGENTA);
                        add.accept(ModItems.PAINTER_LIGHT_BLUE);
                        add.accept(ModItems.PAINTER_YELLOW);
                        add.accept(ModItems.PAINTER_LIME);
                        add.accept(ModItems.PAINTER_PINK);
                        add.accept(ModItems.PAINTER_GRAY);
                        add.accept(ModItems.PAINTER_LIGHT_GRAY);
                        add.accept(ModItems.PAINTER_CYAN);
                        add.accept(ModItems.PAINTER_PURPLE);
                        add.accept(ModItems.PAINTER_BLUE);
                        add.accept(ModItems.PAINTER_BROWN);
                        add.accept(ModItems.PAINTER_GREEN);
                        add.accept(ModItems.PAINTER_RED);
                        add.accept(ModItems.PAINTER_BLACK);
                        add.accept(ModItems.CONTAINMENT_BOX);
                        add.accept(ModItems.TOOL_BOX);

                        add.accept(ModItems.RE_BATTERY);
                        add.accept(ModItems.ADVANCED_RE_BATTERY);
                        add.accept(ModItems.CHARGING_RE_BATTERY);
                        add.accept(ModItems.ADVANCED_CHARGING_RE_BATTERY);
                        add.accept(ModItems.ENERGY_CRYSTAL);
                        add.accept(ModItems.LAPOTRON_CRYSTAL);
                        add.accept(ModItems.CHARGING_ENERGY_CRYSTAL);
                        add.accept(ModItems.CHARGING_LAPOTRON_CRYSTAL);
                        add.accept(ModItems.FLUID_CELL);
                        add.accept(ModItems.MINING_PIPE);
                        for (UniversalFluidCellItem.CellFluid fluid : UniversalFluidCellItem.CellFluid.values()) {
                            if (fluid != UniversalFluidCellItem.CellFluid.EMPTY) {
                                entries.add(UniversalFluidCellItem.createStack(fluid));
                            }
                        }
                        for (ModFluids.Ic2FluidEntry fluid : ModFluids.entries()) {
                            add.accept(fluid.item());
                        }
                        add.accept(ModItems.BATPACK);
                        add.accept(ModItems.ADVANCED_BATPACK);
                        add.accept(ModItems.JETPACK);
                        add.accept(ModItems.JETPACK_ELECTRIC);
                        add.accept(ModItems.SOLAR_HELMET);
                        add.accept(ModItems.STATIC_BOOTS);
                        add.accept(ModItems.ENERGY_PACK);
                        addCharged.accept(ModItems.RE_BATTERY);
                        addCharged.accept(ModItems.ADVANCED_RE_BATTERY);
                        addCharged.accept(ModItems.CHARGING_RE_BATTERY);
                        addCharged.accept(ModItems.ADVANCED_CHARGING_RE_BATTERY);
                        addCharged.accept(ModItems.ENERGY_CRYSTAL);
                        addCharged.accept(ModItems.LAPOTRON_CRYSTAL);
                        addCharged.accept(ModItems.CHARGING_ENERGY_CRYSTAL);
                        addCharged.accept(ModItems.CHARGING_LAPOTRON_CRYSTAL);
                        addCharged.accept(ModItems.BATPACK);
                        addCharged.accept(ModItems.ADVANCED_BATPACK);
                        entries.add(BiogasJetpackItem.createFilledStack());
                        addCharged.accept(ModItems.JETPACK_ELECTRIC);
                        addCharged.accept(ModItems.ENERGY_PACK);

                        // Night Vision
                        add.accept(ModItems.NIGHTVISION_GOGGLES);
                        add.accept(ModItems.NANO_SABER);
                        add.accept(ModItems.MINING_LASER);
                        addCharged.accept(ModItems.NIGHTVISION_GOGGLES);
                        addCharged.accept(ModItems.NANO_SABER);
                        add.accept(ModItems.DRILL);
                        add.accept(ModItems.DIAMOND_DRILL);
                        add.accept(ModItems.IRIDIUM_DRILL);
                        add.accept(ModItems.CHAINSAW);
                        addCharged.accept(ModItems.MINING_LASER);
                        addCharged.accept(ModItems.DRILL);
                        addCharged.accept(ModItems.DIAMOND_DRILL);
                        addCharged.accept(ModItems.IRIDIUM_DRILL);
                        addCharged.accept(ModItems.CHAINSAW);
                        addCharged.accept(ModItems.ELECTRIC_TREETAP);
                        addCharged.accept(ModItems.ELECTRIC_HOE);
                        addCharged.accept(ModItems.ELECTRIC_WRENCH);

                        // Hazmat / utility armor
                        add.accept(ModItems.HAZMAT_HELMET);
                        add.accept(ModItems.HAZMAT_CHESTPLATE);
                        add.accept(ModItems.HAZMAT_LEGGINGS);
                        add.accept(ModItems.RUBBER_BOOTS);
                        add.accept(ModItems.BRONZE_HELMET);
                        add.accept(ModItems.BRONZE_CHESTPLATE);
                        add.accept(ModItems.BRONZE_LEGGINGS);
                        add.accept(ModItems.BRONZE_BOOTS);
                        add.accept(ModItems.ALLOY_CHESTPLATE);
                        add.accept(ModItems.BRONZE_SWORD);
                        add.accept(ModItems.BRONZE_PICKAXE);
                        add.accept(ModItems.BRONZE_SHOVEL);
                        add.accept(ModItems.BRONZE_HOE);
                        add.accept(ModItems.BRONZE_AXE);

                        // NanoSuit armor
                        add.accept(ModItems.NANO_HELMET);
                        add.accept(ModItems.NANO_CHESTPLATE);
                        add.accept(ModItems.NANO_LEGGINGS);
                        add.accept(ModItems.NANO_BOOTS);
                        addCharged.accept(ModItems.NANO_HELMET);
                        addCharged.accept(ModItems.NANO_CHESTPLATE);
                        addCharged.accept(ModItems.NANO_LEGGINGS);
                        addCharged.accept(ModItems.NANO_BOOTS);

                        // QuantumSuit armor
                        add.accept(ModItems.QUANTUM_HELMET);
                        add.accept(ModItems.QUANTUM_CHESTPLATE);
                        add.accept(ModItems.QUANTUM_LEGGINGS);
                        add.accept(ModItems.QUANTUM_BOOTS);
                        addCharged.accept(ModItems.QUANTUM_HELMET);
                        addCharged.accept(ModItems.QUANTUM_CHESTPLATE);
                        addCharged.accept(ModItems.QUANTUM_LEGGINGS);
                        addCharged.accept(ModItems.QUANTUM_BOOTS);

                        // ------------------------------
                        // Components / crafting items
                        // ------------------------------
                        add.accept(ModItems.STICKY_RESIN);
                        add.accept(ModItems.RUBBER);
                        add.accept(ModItems.SULFUR);
                        add.accept(ModItems.IODINE);
                        add.accept(ModItems.WATER_SHEET);
                        add.accept(ModItems.LAVA_SHEET);
                        add.accept(ModItems.ELECTRONIC_CIRCUIT);

                        // Circuits
                        add.accept(ModItems.ADVANCED_CIRCUIT);
                        add.accept(ModItems.MFSU_UPGRADE_KIT);

                        // Reactor components
                        add.accept(ModItems.HEAT_EXCHANGER);
                        add.accept(ModItems.REACTOR_HEAT_EXCHANGER);
                        add.accept(ModItems.COMPONENT_HEAT_EXCHANGER);
                        add.accept(ModItems.ADVANCED_HEAT_EXCHANGER);
add.accept(ModItems.HEAT_VENT);
add.accept(ModItems.REACTOR_HEAT_VENT);
add.accept(ModItems.COMPONENT_HEAT_VENT);
add.accept(ModItems.ADVANCED_HEAT_VENT);
add.accept(ModItems.OVERCLOCKED_HEAT_VENT);
add.accept(ModItems.HEAT_STORAGE);
add.accept(ModItems.TRI_HEAT_STORAGE);
add.accept(ModItems.HEX_HEAT_STORAGE);
add.accept(ModItems.REACTOR_PLATING);
add.accept(ModItems.HEAT_PLATING);
add.accept(ModItems.CONTAINMENT_PLATING);
add.accept(ModItems.NEUTRON_REFLECTOR);
add.accept(ModItems.THICK_NEUTRON_REFLECTOR);
add.accept(ModItems.IRIDIUM_REFLECTOR);
add.accept(ModItems.RSH_CONDENSATOR);
add.accept(ModItems.LZH_CONDENSATOR);

                        add.accept(ModItems.CF_POWDER);
                        add.accept(ModItems.PLANT_BALL);
                        add.accept(ModItems.TIN_CAN);
                        add.accept(ModItems.FILLED_TIN_CAN);
                        add.accept(ModItems.SCRAP);
                        add.accept(ModItems.SCRAP_BOX);
                        add.accept(ModItems.FERTILIZER);
                        add.accept(ModItems.SLAG);
                        add.accept(ModItems.COIL);
                        add.accept(ModItems.ELECTRIC_MOTOR);

                        // Carbon chain (if present)
                        add.accept(ModItems.CARBON_FIBRE);
                        add.accept(ModItems.CARBON_MESH);
                        add.accept(ModItems.CARBON_PLATE);
                        add.accept(ModItems.IRON_ROD);
                        add.accept(ModItems.BRONZE_ROD);
                        add.accept(ModItems.STEEL_ROD);
                        add.accept(ModItems.FUEL_ROD);
                        add.accept(ModItems.URANIUM_FUEL_ROD);
                        add.accept(ModItems.DUAL_URANIUM_FUEL_ROD);
                        add.accept(ModItems.QUAD_URANIUM_FUEL_ROD);
                        add.accept(ModItems.MOX_FUEL_ROD);
                        add.accept(ModItems.DUAL_MOX_FUEL_ROD);
                        add.accept(ModItems.QUAD_MOX_FUEL_ROD);
                        add.accept(ModItems.LITHIUM_FUEL_ROD);
                        add.accept(ModItems.TRITIUM_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_ISOTOPE_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_URANIUM_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_DUAL_URANIUM_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_QUAD_URANIUM_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_MOX_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_DUAL_MOX_FUEL_ROD);
                        add.accept(ModItems.DEPLETED_QUAD_MOX_FUEL_ROD);

                        // ------------------------------
                        // Ingots
                        // ------------------------------
                        add.accept(ModItems.TIN_INGOT);
                        add.accept(ModItems.LEAD_INGOT);
                        add.accept(ModItems.SILVER_INGOT);
                        add.accept(ModItems.BRONZE_INGOT);
                        add.accept(ModItems.STEEL_INGOT);
                        add.accept(ModItems.IRIDIUM);

                        // ------------------------------
                        // Dusts (common)
                        // ------------------------------
                        add.accept(ModItems.COPPER_DUST);
                        add.accept(ModItems.TIN_DUST);
                        add.accept(ModItems.LEAD_DUST);
                        add.accept(ModItems.SILVER_DUST);
                        add.accept(ModItems.IRON_DUST);
                        add.accept(ModItems.GOLD_DUST);
                        add.accept(ModItems.BRONZE_DUST);

                        add.accept(ModItems.COAL_DUST);
                        add.accept(ModItems.CLAY_DUST);
                        add.accept(ModItems.LAPIS_DUST);
                        add.accept(ModItems.OBSIDIAN_DUST);
                        add.accept(ModItems.DIAMOND_DUST);

                        add.accept(ModItems.LITHIUM_DUST);
                        add.accept(ModItems.MILK_DUST);
                        add.accept(ModItems.NETHERRACK_DUST);
                        add.accept(ModItems.SILICON_DIOXIDE);

                        add.accept(ModItems.STONE_DUST);
                        add.accept(ModItems.TIN_HYDRATED_DUST);
                        add.accept(ModItems.COAL_FUEL_DUST);
                        add.accept(ModItems.ENERGIUM_DUST);

                        // ------------------------------
                        // Small dusts
                        // ------------------------------
                        add.accept(ModItems.SMALL_COPPER_DUST);
                        add.accept(ModItems.SMALL_TIN_DUST);
                        add.accept(ModItems.SMALL_LEAD_DUST);
                        add.accept(ModItems.SMALL_SILVER_DUST);
                        add.accept(ModItems.SMALL_IRON_DUST);
                        add.accept(ModItems.SMALL_GOLD_DUST);
                        add.accept(ModItems.SMALL_BRONZE_DUST);

                        add.accept(ModItems.SMALL_LAPIS_DUST);
                        add.accept(ModItems.SMALL_OBSIDIAN_DUST);
                        add.accept(ModItems.SMALL_LITHIUM_DUST);
                        add.accept(ModItems.SMALL_EMERALD_DUST);
                        add.accept(ModItems.SMALL_SULFUR_DUST);

                        // ------------------------------
                        // Crushed ores
                        // ------------------------------
                        add.accept(ModItems.CRUSHED_COPPER_ORE);
                        add.accept(ModItems.CRUSHED_TIN_ORE);
                        add.accept(ModItems.CRUSHED_LEAD_ORE);
                        add.accept(ModItems.CRUSHED_SILVER_ORE);
                        add.accept(ModItems.CRUSHED_IRON_ORE);
                        add.accept(ModItems.CRUSHED_GOLD_ORE);
                        add.accept(ModItems.CRUSHED_URANIUM_ORE);
                        add.accept(ModItems.PURIFIED_COPPER_ORE);
                        add.accept(ModItems.PURIFIED_TIN_ORE);
                        add.accept(ModItems.PURIFIED_LEAD_ORE);
                        add.accept(ModItems.PURIFIED_SILVER_ORE);
                        add.accept(ModItems.PURIFIED_IRON_ORE);
                        add.accept(ModItems.PURIFIED_GOLD_ORE);
                        add.accept(ModItems.PURIFIED_URANIUM_ORE);
                        add.accept(ModItems.URANIUM_238);
                        add.accept(ModItems.SMALL_URANIUM_235);
                        add.accept(ModItems.SMALL_URANIUM_238);
                        add.accept(ModItems.SMALL_PLUTONIUM);
                        add.accept(ModItems.URANIUM_235);
                        add.accept(ModItems.PLUTONIUM);
                        add.accept(ModItems.URANIUM);
                        add.accept(ModItems.MOX);
                        add.accept(ModItems.URANIUM_PELLET);
                        add.accept(ModItems.MOX_PELLET);
                        add.accept(ModItems.RTG_PELLET);
                        add.accept(ModItems.NEAR_DEPLETED_URANIUM);
                        add.accept(ModItems.RE_ENRICHED_URANIUM);

                        // ------------------------------
                        // Plates
                        // ------------------------------
                        add.accept(ModItems.COPPER_PLATE);
                        add.accept(ModItems.TIN_PLATE);
                        add.accept(ModItems.LEAD_PLATE);
                        add.accept(ModItems.IRON_PLATE);
                        add.accept(ModItems.GOLD_PLATE);
                        add.accept(ModItems.BRONZE_PLATE);
                        add.accept(ModItems.STEEL_PLATE);

                        add.accept(ModItems.LAPIS_PLATE);
                        add.accept(ModItems.OBSIDIAN_PLATE);
                        add.accept(ModItems.IRIDIUM_PLATE);

                        // ------------------------------
                        // Dense plates
                        // ------------------------------
                        add.accept(ModItems.DENSE_COPPER_PLATE);
                        add.accept(ModItems.DENSE_TIN_PLATE);
                        add.accept(ModItems.DENSE_LEAD_PLATE);
                        add.accept(ModItems.DENSE_IRON_PLATE);
                        add.accept(ModItems.DENSE_GOLD_PLATE);
                        add.accept(ModItems.DENSE_BRONZE_PLATE);
                        add.accept(ModItems.DENSE_STEEL_PLATE);

                        add.accept(ModItems.DENSE_LAPIS_PLATE);
                        add.accept(ModItems.DENSE_OBSIDIAN_PLATE);

                        // ------------------------------
                        // Casings
                        // ------------------------------
                        add.accept(ModItems.COPPER_CASING);
                        add.accept(ModItems.TIN_CASING);
                        add.accept(ModItems.LEAD_CASING);
                        add.accept(ModItems.IRON_CASING);
                        add.accept(ModItems.GOLD_CASING);
                        add.accept(ModItems.BRONZE_CASING);
                        add.accept(ModItems.STEEL_CASING);

                        // ------------------------------
                        // Crop / misc (if present)
                        // ------------------------------
                        add.accept(ModItems.COFFEE_BEANS);
                        add.accept(ModItems.COFFEE_POWDER);
                        add.accept(ModItems.GRIN_POWDER);
                        add.accept(ModItems.WEED);
                        add.accept(ModItems.BOBS_YER_UNCLE_RANKS_BERRY);

                        // Advanced alloys (IC2 chain)
                        add.accept(ModItems.MIXED_METAL_INGOT);
                        add.accept(ModItems.ADVANCED_ALLOY);

                        // ------------------------------
                        // Fallback: anything else registered under this modid (sorted by id)
                        // ------------------------------
                        List<Identifier> ids = new ArrayList<>();
                        for (Identifier id : Registries.ITEM.getIds()) {
                            if (IndustrialLegacy.MOD_ID.equals(id.getNamespace())) {
                                ids.add(id);
                            }
                        }
                        ids.sort(Comparator.comparing(Identifier::getPath));

                        for (Identifier id : ids) {
                            net.minecraft.item.Item item = Registries.ITEM.get(id);
                            if (added.add(item)) {
                                entries.add(item);
                            }
                        }
                    })
                    .build()
    );

    public static void register() {
        // classload triggers static init
    }
}
