package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.RubberLogBlock;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
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

        // проверяем: это наш каучуковый лог с доступной смолой
        if (state.isOf(ModBlocks.RUBBER_LOG)
                && state.contains(RubberLogBlock.RESIN)
                && state.get(RubberLogBlock.RESIN)) {

            PlayerEntity player = ctx.getPlayer();

            // 1–3 смолы
            ItemStack drop = new ItemStack(ModItems.STICKY_RESIN,
                    world.random.nextInt(3) + 1);

            if (player != null) {
                if (!player.getInventory().insertStack(drop.copy())) {
                    player.dropItem(drop, false);
                }
            } else {
                RubberLogBlock.dropStack(world, hit.getBlockPos(), drop);
            }

            // “выкачали” смолу — flag в false
            world.setBlockState(hit.getBlockPos(),
                    state.with(RubberLogBlock.RESIN, false), 3);

            // звук
            world.playSound(null, hit.getBlockPos(),
                    SoundEvents.ITEM_BOTTLE_FILL,
                    SoundCategory.BLOCKS, 0.7f, 1.0f);

            // урон по durability
            ItemStack stack = ctx.getStack();
            if (player != null) {
                stack.damage(1, player, p -> p.sendToolBreakStatus(ctx.getHand()));
            }

            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }
}
