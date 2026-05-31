package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.block.RubberLogBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** IL Experimental electric treetap: 50 EU/use, 10 000 EU capacity, tier 1. */
public final class ElectricTreetapItem extends AbstractElectricToolItem {
    public ElectricTreetapItem(Settings settings) {
        super(settings, 50L, 0, 10_000L, 100L, 1, 1.0f);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        ItemStack stack = ctx.getStack();
        PlayerEntity player = ctx.getPlayer();

        BlockState state = world.getBlockState(pos);
        if (!state.isOf(ModBlocks.RUBBER_LOG)
                || !state.contains(RubberLogBlock.RESIN)
                || !state.get(RubberLogBlock.RESIN)
                || !canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack drop = new ItemStack(ModItems.STICKY_RESIN, world.random.nextInt(3) + 1);
        if (player != null) {
            if (!player.getInventory().insertStack(drop.copy())) {
                player.dropItem(drop, false);
            }
        } else {
            Block.dropStack(world, pos, drop);
        }

        world.setBlockState(pos, state.with(RubberLogBlock.RESIN, false), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.7f, 1.0f);
        if (player != null) useEnergy(stack, player, operationEnergyCost);
        return ActionResult.CONSUME;
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return false;
    }
}
