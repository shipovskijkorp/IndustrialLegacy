package com.shipovskijkorp.industriallegacy.registry;

import com.shipovskijkorp.industriallegacy.IndustrialLegacy;
import com.shipovskijkorp.industriallegacy.item.CableItem;
import com.shipovskijkorp.industriallegacy.item.CableKind;
import com.shipovskijkorp.industriallegacy.item.CableVariants;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
    private ModItemGroups() {}

    public static final ItemGroup MAIN = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(IndustrialLegacy.MOD_ID, "main"),
            FabricItemGroup.builder()
                    // Icon: copper cable (uninsulated)
                    .icon(() -> CableItem.createStack(ModItems.CABLE, CableKind.COPPER, 0))
                    .displayName(Text.translatable("itemGroup.industrial_legacy.main"))
                    .entries((ctx, entries) -> {
                        // Blocks
                        entries.add(ModBlocks.GENERATOR);
                        entries.add(ModBlocks.BATBOX);
                        entries.add(ModBlocks.IRON_FURNACE);
                        entries.add(ModBlocks.MACHINE_CASING);
                        entries.add(ModBlocks.MACERATOR);

                        entries.add(ModBlocks.LEAD_ORE);
                        entries.add(ModBlocks.TIN_ORE);
                        entries.add(ModBlocks.URANIUM_ORE);

                        entries.add(ModBlocks.RUBBER_LOG);
                        entries.add(ModBlocks.RUBBER_LEAVES);
                        entries.add(ModBlocks.RUBBER_SAPLING);

                        // Add all cable variants (14) with correct NBT (kind/insulation + derived variant)
                        for (ItemStack stack : CableVariants.createAll(ModItems.CABLE)) {
                            entries.add(stack);
                        }

                        // Items
                        entries.add(ModItems.FORGE_HAMMER);
                        entries.add(ModItems.CUTTER);
                        entries.add(ModItems.TREETAP);
                        entries.add(ModItems.RE_BATTERY);
                        entries.add(ModItems.ELECTRONIC_CIRCUIT);

                        entries.add(ModItems.RUBBER);
                        entries.add(ModItems.STICKY_RESIN);
                        entries.add(ModItems.SULFUR);

                        // Dusts (IC2-style)
                        entries.add(ModItems.BRONZE_DUST);
                        entries.add(ModItems.CLAY_DUST);
                        entries.add(ModItems.COAL_DUST);
                        entries.add(ModItems.COAL_FUEL_DUST);
                        entries.add(ModItems.COPPER_DUST);
                        entries.add(ModItems.DIAMOND_DUST);
                        entries.add(ModItems.ENERGIUM_DUST);
                        entries.add(ModItems.GOLD_DUST);
                        entries.add(ModItems.IRON_DUST);
                        entries.add(ModItems.LAPIS_DUST);
                        entries.add(ModItems.LEAD_DUST);
                        entries.add(ModItems.LITHIUM_DUST);
                        entries.add(ModItems.OBSIDIAN_DUST);
                        entries.add(ModItems.SILICON_DIOXIDE);
                        entries.add(ModItems.SILVER_DUST);
                        entries.add(ModItems.STONE_DUST);
                        entries.add(ModItems.TIN_DUST);
                        entries.add(ModItems.TIN_HYDRATED_DUST);

                        // Small dusts (tiny piles)
                        entries.add(ModItems.SMALL_BRONZE_DUST);
                        entries.add(ModItems.SMALL_COPPER_DUST);
                        entries.add(ModItems.SMALL_GOLD_DUST);
                        entries.add(ModItems.SMALL_IRON_DUST);
                        entries.add(ModItems.SMALL_LAPIS_DUST);
                        entries.add(ModItems.SMALL_LEAD_DUST);
                        entries.add(ModItems.SMALL_LITHIUM_DUST);
                        entries.add(ModItems.SMALL_OBSIDIAN_DUST);
                        entries.add(ModItems.SMALL_SILVER_DUST);
                        entries.add(ModItems.SMALL_SULFUR_DUST);
                        entries.add(ModItems.SMALL_TIN_DUST);

                        entries.add(ModItems.SILVER_INGOT);
                        entries.add(ModItems.TIN_INGOT);
                        entries.add(ModItems.LEAD_INGOT);
                        entries.add(ModItems.BRONZE_INGOT);

                        // Crushed ores (macerator output)
                        entries.add(ModItems.CRUSHED_IRON_ORE);
                        entries.add(ModItems.CRUSHED_GOLD_ORE);
                        entries.add(ModItems.CRUSHED_COPPER_ORE);
                        entries.add(ModItems.CRUSHED_TIN_ORE);
                        entries.add(ModItems.CRUSHED_LEAD_ORE);
                        entries.add(ModItems.CRUSHED_URANIUM_ORE);


                        // Materials
                        entries.add(ModItems.IRIDIUM);
                        entries.add(ModItems.MIXED_METAL_INGOT);
                        entries.add(ModItems.ADVANCED_ALLOY);

                        // Plates
                        entries.add(ModItems.BRONZE_PLATE);
                        entries.add(ModItems.COPPER_PLATE);
                        entries.add(ModItems.GOLD_PLATE);
                        entries.add(ModItems.IRON_PLATE);
                        entries.add(ModItems.LAPIS_PLATE);
                        entries.add(ModItems.LEAD_PLATE);
                        entries.add(ModItems.OBSIDIAN_PLATE);
                        entries.add(ModItems.STEEL_PLATE);
                        entries.add(ModItems.TIN_PLATE);
                        entries.add(ModItems.IRIDIUM_PLATE);

                        // Dense plates
                        entries.add(ModItems.DENSE_BRONZE_PLATE);
                        entries.add(ModItems.DENSE_COPPER_PLATE);
                        entries.add(ModItems.DENSE_GOLD_PLATE);
                        entries.add(ModItems.DENSE_IRON_PLATE);
                        entries.add(ModItems.DENSE_LAPIS_PLATE);
                        entries.add(ModItems.DENSE_LEAD_PLATE);
                        entries.add(ModItems.DENSE_OBSIDIAN_PLATE);
                        entries.add(ModItems.DENSE_STEEL_PLATE);
                        entries.add(ModItems.DENSE_TIN_PLATE);

                        // Casings
                        entries.add(ModItems.BRONZE_CASING);
                        entries.add(ModItems.COPPER_CASING);
                        entries.add(ModItems.GOLD_CASING);
                        entries.add(ModItems.IRON_CASING);
                        entries.add(ModItems.LEAD_CASING);
                        entries.add(ModItems.STEEL_CASING);
                        entries.add(ModItems.TIN_CASING);


                        entries.add(ModItems.DEBUG_WRENCH);
                    })
                    .build()
    );


    public static void register() {
        // classload triggers static init
    }
}