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

                        entries.add(ModItems.RUBBER);
                        entries.add(ModItems.STICKY_RESIN);
                        entries.add(ModItems.SULFUR);

                        entries.add(ModItems.SILVER_INGOT);
                        entries.add(ModItems.TIN_INGOT);
                        entries.add(ModItems.LEAD_INGOT);
                        entries.add(ModItems.BRONZE_INGOT);


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