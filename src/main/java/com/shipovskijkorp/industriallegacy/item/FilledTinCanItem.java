package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * IC2-like filled tin can.
 *
 * A stack of filled cans restores one hunger point per can consumed and returns
 * the same number of empty tin cans to the player inventory.
 */
public final class FilledTinCanItem extends Item {
    public FilledTinCanItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient || !user.canConsume(false)) {
            return TypedActionResult.pass(stack);
        }

        ItemStack result = consumeFromStack(user, stack);
        if (result == stack) {
            return TypedActionResult.pass(stack);
        }
        user.setStackInHand(hand, result);
        return TypedActionResult.success(result, false);
    }

    /**
     * Consume as many cans from the provided stack as needed to fill hunger.
     * Returns the updated stack or the original stack if nothing happened.
     */
    public static ItemStack consumeFromStack(PlayerEntity player, ItemStack stack) {
        int missing = Math.max(0, 20 - player.getHungerManager().getFoodLevel());
        int amount = Math.min(stack.getCount(), missing);
        if (amount <= 0) {
            return stack;
        }

        ItemStack empties = new ItemStack(ModItems.TIN_CAN, amount);
        if (!player.getInventory().insertStack(empties.copy())) {
            return stack;
        }

        player.getHungerManager().add(amount, amount);
        ItemStack out = stack.copy();
        out.decrement(amount);
        return out;
    }
}
