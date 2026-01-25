package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.RubberLogBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class TreetapItem extends Item {
    public TreetapItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        BlockHitResult hit = (BlockHitResult) ctx.getHitResult();
        BlockState state = world.getBlockState(hit.getBlockPos());

        if (state.isOf(ModBlocks.RUBBER_LOG)
                && state.contains(RubberLogBlock.RESIN)
                && state.get(RubberLogBlock.RESIN)) {

            PlayerEntity player = ctx.getPlayer();

            ItemStack drop = new ItemStack(ModItems.STICKY_RESIN, world.random.nextInt(3) + 1);

            if (player != null) {
                if (!player.getInventory().insertStack(drop.copy())) {
                    player.dropItem(drop, false);
                }
            } else {
                Block.dropStack(world, hit.getBlockPos(), drop);
            }

            world.setBlockState(hit.getBlockPos(), state.with(RubberLogBlock.RESIN, false), 3);

            world.playSound(null, hit.getBlockPos(),
                    SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.7f, 1.0f);

            ItemStack stack = ctx.getStack();
            if (player != null) {
                stack.damage(1, player, p -> p.sendToolBreakStatus(ctx.getHand()));
            }

            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}
