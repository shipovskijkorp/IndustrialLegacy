package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ScrapBoxItem extends Item {
    private static final List<Drop> DROPS = new ArrayList<>();
    private static float totalWeight = 0.0f;

    private static void ensureDrops() {
        if (!DROPS.isEmpty()) return;

        add(Items.WOODEN_HOE, 5.01f);
        add(Blocks.DIRT.asItem(), 5.0f);
        add(Items.STICK, 4.0f);
        add(Blocks.GRASS_BLOCK.asItem(), 3.0f);
        add(Blocks.GRAVEL.asItem(), 3.0f);
        add(Blocks.NETHERRACK.asItem(), 2.0f);
        add(Items.ROTTEN_FLESH, 2.0f);
        add(Items.APPLE, 1.5f);
        add(Items.BREAD, 1.5f);
        add(ModItems.FILLED_TIN_CAN, 1.5f);
        add(Items.WOODEN_SWORD, 1.0f);
        add(Items.WOODEN_SHOVEL, 1.0f);
        add(Items.WOODEN_PICKAXE, 1.0f);
        add(Blocks.SOUL_SAND.asItem(), 1.0f);
        add(Items.OAK_SIGN, 1.0f);
        add(Items.LEATHER, 1.0f);
        add(Items.FEATHER, 1.0f);
        add(Items.BONE, 1.0f);
        add(Items.COOKED_PORKCHOP, 0.9f);
        add(Items.COOKED_BEEF, 0.9f);
        add(Blocks.PUMPKIN.asItem(), 0.9f);
        add(Items.COOKED_CHICKEN, 0.9f);
        add(Items.MINECART, 0.01f);
        add(Items.REDSTONE, 0.9f);
        add(ModItems.RUBBER, 0.8f);
        add(Items.GLOWSTONE_DUST, 0.8f);
        add(ModItems.COAL_DUST, 0.8f);
        add(ModItems.COPPER_DUST, 0.8f);
        add(ModItems.TIN_DUST, 0.8f);
        add(ModItems.IRON_DUST, 0.7f);
        add(ModItems.GOLD_DUST, 0.7f);
        add(Items.SLIME_BALL, 0.6f);
        add(Blocks.IRON_ORE.asItem(), 0.5f);
        add(Items.GOLDEN_HELMET, 0.01f);
        add(Blocks.GOLD_ORE.asItem(), 0.5f);
        add(Items.CAKE, 0.5f);
        add(Items.DIAMOND, 0.1f);
        add(Items.EMERALD, 0.05f);
        add(Items.ENDER_PEARL, 0.08f);
        add(Items.BLAZE_ROD, 0.04f);
        add(Items.EGG, 0.8f);
        add(Items.COPPER_ORE, 0.7f);
        add(ModBlocks.TIN_ORE.asItem(), 0.7f);
    }

    public ScrapBoxItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            ItemStack drop = getDrop(world);
            boolean dropped = user.dropItem(drop, false) != null;
            if (dropped && !user.getAbilities().creativeMode) {
                stack.decrement(1);
            }
            return dropped ? TypedActionResult.success(stack) : TypedActionResult.pass(stack);
        }
        return TypedActionResult.success(stack);
    }

    public static ItemStack getDrop(World world) {
        ensureDrops();
        if (DROPS.isEmpty()) return ItemStack.EMPTY;
        float value = world.random.nextFloat() * totalWeight;
        for (Drop drop : DROPS) {
            if (value < drop.upperBound) return new ItemStack(drop.item);
        }
        return new ItemStack(DROPS.get(DROPS.size() - 1).item);
    }

    private static void add(Item item, float weight) {
        if (item == Items.AIR || weight <= 0.0f) return;
        totalWeight += weight;
        DROPS.add(new Drop(item, totalWeight));
    }

    private record Drop(Item item, float upperBound) {}
}
