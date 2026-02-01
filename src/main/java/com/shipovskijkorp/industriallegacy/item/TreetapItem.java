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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/*
  IL-style treetap: extracts sticky resin from rubber log "resin" spots.

  <p>Important: in 1.20+ {@link ItemUsageContext#getHitResult()} is protected, so we use
  {@link ItemUsageContext#getBlockPos()} and {@link ItemUsageContext#getSide()} instead.</p>
 */
public class TreetapItem extends Item {
    public TreetapItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // Let the client play hand animation immediately.
        if (world.isClient) return ActionResult.SUCCESS;

        BlockState state = world.getBlockState(pos);

        // Our rubber log with resin available.
        if (!state.isOf(ModBlocks.RUBBER_LOG)
                || !state.contains(RubberLogBlock.RESIN)
                || !state.get(RubberLogBlock.RESIN)) {
            return ActionResult.PASS;
        }

        PlayerEntity player = ctx.getPlayer();

        // Drop 1–3 sticky resin (IL-like).
        ItemStack drop = new ItemStack(ModItems.STICKY_RESIN, world.random.nextInt(3) + 1);

        if (player != null) {
            if (!player.getInventory().insertStack(drop.copy())) {
                player.dropItem(drop, false);
            }
        } else {
            Block.dropStack(world, pos, drop);
        }

        // Mark resin as harvested.
        world.setBlockState(pos, state.with(RubberLogBlock.RESIN, false), Block.NOTIFY_ALL);

        // Sound + tool damage.
        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 0.7f, 1.0f);

        if (player != null) {
            ctx.getStack().damage(1, player, p -> p.sendToolBreakStatus(ctx.getHand()));
        }

        return ActionResult.CONSUME;
    }
}
