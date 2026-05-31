package com.shipovskijkorp.industriallegacy.item.tool;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** IL Experimental electric hoe: 50 EU/use, 10 000 EU capacity, tier 1, iron-level. */
public final class ElectricHoeItem extends AbstractElectricToolItem {
    public ElectricHoeItem(Settings settings) {
        super(settings, 50L, 2, 10_000L, 100L, 1, 16.0f);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        ItemStack stack = ctx.getStack();

        if (ctx.getSide() == Direction.DOWN || !world.getBlockState(pos.up()).isAir() || !canUse(stack, operationEnergyCost)) {
            return ActionResult.PASS;
        }

        BlockState target = getTilledState(world.getBlockState(pos));
        if (target == null) return ActionResult.PASS;

        world.playSound(ctx.getPlayer(), pos, SoundEvents.ITEM_HOE_TILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        if (!world.isClient) {
            world.setBlockState(pos, target, Block.NOTIFY_ALL);
            if (ctx.getPlayer() != null) useEnergy(stack, ctx.getPlayer(), operationEnergyCost);
        }
        return ActionResult.success(world.isClient);
    }

    private static BlockState getTilledState(BlockState state) {
        if (state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.DIRT_PATH)
                || state.isOf(Blocks.PODZOL)
                || state.isOf(Blocks.MYCELIUM)) {
            return Blocks.FARMLAND.getDefaultState();
        }
        if (state.isOf(Blocks.COARSE_DIRT) || state.isOf(Blocks.ROOTED_DIRT)) {
            return Blocks.DIRT.getDefaultState();
        }
        return null;
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return state.isOf(Blocks.HAY_BLOCK)
                || state.isOf(Blocks.DRIED_KELP_BLOCK)
                || state.isOf(Blocks.NETHER_WART_BLOCK)
                || state.isOf(Blocks.WARPED_WART_BLOCK)
                || state.isOf(Blocks.SPONGE)
                || state.isOf(Blocks.WET_SPONGE);
    }
}
