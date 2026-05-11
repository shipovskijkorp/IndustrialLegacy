package com.shipovskijkorp.industriallegacy.compat.jei;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

final class ScrapBoxJeiRecipeFactory {
    private ScrapBoxJeiRecipeFactory() {}

    static List<ScrapBoxJeiRecipe> create() {
        List<Entry> entries = new ArrayList<>();
        add(entries, Items.WOODEN_HOE, 5.01f);
        add(entries, Blocks.DIRT.asItem(), 5.0f);
        add(entries, Items.STICK, 4.0f);
        add(entries, Blocks.GRASS_BLOCK.asItem(), 3.0f);
        add(entries, Blocks.GRAVEL.asItem(), 3.0f);
        add(entries, Blocks.NETHERRACK.asItem(), 2.0f);
        add(entries, Items.ROTTEN_FLESH, 2.0f);
        add(entries, Items.APPLE, 1.5f);
        add(entries, Items.BREAD, 1.5f);
        add(entries, ModItems.FILLED_TIN_CAN, 1.5f);
        add(entries, Items.WOODEN_SWORD, 1.0f);
        add(entries, Items.WOODEN_SHOVEL, 1.0f);
        add(entries, Items.WOODEN_PICKAXE, 1.0f);
        add(entries, Blocks.SOUL_SAND.asItem(), 1.0f);
        add(entries, Items.OAK_SIGN, 1.0f);
        add(entries, Items.LEATHER, 1.0f);
        add(entries, Items.FEATHER, 1.0f);
        add(entries, Items.BONE, 1.0f);
        add(entries, Items.COOKED_PORKCHOP, 0.9f);
        add(entries, Items.COOKED_BEEF, 0.9f);
        add(entries, Blocks.PUMPKIN.asItem(), 0.9f);
        add(entries, Items.COOKED_CHICKEN, 0.9f);
        add(entries, Items.MINECART, 0.01f);
        add(entries, Items.REDSTONE, 0.9f);
        add(entries, ModItems.RUBBER, 0.8f);
        add(entries, Items.GLOWSTONE_DUST, 0.8f);
        add(entries, ModItems.COAL_DUST, 0.8f);
        add(entries, ModItems.COPPER_DUST, 0.8f);
        add(entries, ModItems.TIN_DUST, 0.8f);
        add(entries, ModItems.IRON_DUST, 0.7f);
        add(entries, ModItems.GOLD_DUST, 0.7f);
        add(entries, Items.SLIME_BALL, 0.6f);
        add(entries, Blocks.IRON_ORE.asItem(), 0.5f);
        add(entries, Items.GOLDEN_HELMET, 0.01f);
        add(entries, Blocks.GOLD_ORE.asItem(), 0.5f);
        add(entries, Items.CAKE, 0.5f);
        add(entries, Items.DIAMOND, 0.1f);
        add(entries, Items.EMERALD, 0.05f);
        add(entries, Items.ENDER_PEARL, 0.08f);
        add(entries, Items.BLAZE_ROD, 0.04f);
        add(entries, Items.EGG, 0.8f);
        add(entries, Items.COPPER_ORE, 0.7f);
        add(entries, ModBlocks.TIN_ORE.asItem(), 0.7f);

        float total = 0.0f;
        for (Entry entry : entries) total += entry.weight;
        List<ScrapBoxJeiRecipe> out = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            out.add(new ScrapBoxJeiRecipe(new ItemStack(entry.item), entry.weight / total));
        }
        return out;
    }

    private static void add(List<Entry> entries, Item item, float weight) {
        if (item != Items.AIR && weight > 0.0f) entries.add(new Entry(item, weight));
    }

    private record Entry(Item item, float weight) {}
}
