package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.energy.item.ElectricItemManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** IC2 experimental single-use battery port. */
public class SingleUseBatteryItem extends Item {
    public static final long CAPACITY_EU = 1_200L;
    public static final int TIER = 1;

    public SingleUseBatteryItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        long energy = CAPACITY_EU;

        for (int i = 0; i < 9 && energy > 0L; i++) {
            ItemStack target = player.getInventory().main.get(i);
            if (target.isEmpty() || target == stack) continue;
            energy -= ElectricItemManager.charge(target, energy, TIER, true, false);
        }

        if (energy != CAPACITY_EU) {
            stack.decrement(1);
            return new TypedActionResult<>(ActionResult.SUCCESS, stack);
        }
        return new TypedActionResult<>(ActionResult.PASS, stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal(CAPACITY_EU + " EU"));
    }
}
