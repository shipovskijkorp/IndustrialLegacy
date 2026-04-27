package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.item.WrenchItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** IC2 Experimental electric wrench: 100 EU/wrench damage, 12 000 EU capacity, tier 1. */
public final class ElectricWrenchItem extends AbstractElectricToolItem {
    public ElectricWrenchItem(Settings settings) {
        super(settings, 100L, 0, 12_000L, 250L, 1, 12.0f);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);
        ItemStack stack = ctx.getStack();

        if (!canUse(stack, operationEnergyCost)) return ActionResult.PASS;

        BlockState rotated = rotateLikeWrench(state, ctx.getSide());
        if (rotated == null || rotated == state) return ActionResult.PASS;

        if (!world.isClient) {
            world.setBlockState(pos, rotated, Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.BLOCKS, 0.35f, 1.8f);
            if (ctx.getPlayer() != null) useEnergy(stack, ctx.getPlayer(), operationEnergyCost);
        }
        return ActionResult.success(world.isClient);
    }

    private static BlockState rotateLikeWrench(BlockState state, Direction clickedSide) {
        if (state.contains(Properties.FACING)) {
            Direction current = state.get(Properties.FACING);
            Direction target = clickedSide;
            if (!Properties.FACING.getValues().contains(target)) return null;
            if (target == current) target = current.getOpposite();
            return state.with(Properties.FACING, target);
        }
        if (state.contains(HorizontalFacingBlock.FACING)) {
            Direction current = state.get(HorizontalFacingBlock.FACING);
            Direction target = clickedSide.getAxis().isHorizontal() ? clickedSide : current.rotateYClockwise();
            if (target == current) target = current.rotateYClockwise();
            return state.with(HorizontalFacingBlock.FACING, target);
        }
        return null;
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return WrenchItem.isWrenchMineable(state);
    }
}
