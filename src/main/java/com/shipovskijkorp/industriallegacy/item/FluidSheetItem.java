package com.shipovskijkorp.industriallegacy.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** IC2 misc_resource water_sheet/lava_sheet style flat fluid placer. */
public class FluidSheetItem extends Item {
    private final Block fluidBlock;

    public FluidSheetItem(Settings settings, Block fluidBlock) {
        super(settings);
        this.fluidBlock = fluidBlock;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState targetState = world.getBlockState(pos);
        if (!targetState.isReplaceable()) {
            pos = pos.offset(context.getSide());
        }

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (player != null && !player.canPlaceOn(pos, context.getSide(), stack)) {
            return ActionResult.FAIL;
        }

        if (!world.getBlockState(pos).isReplaceable()) {
            return ActionResult.FAIL;
        }

        BlockState placedState = fluidBlock.getDefaultState();
        if (!world.setBlockState(pos, placedState, Block.NOTIFY_ALL)) {
            return ActionResult.FAIL;
        }

        SoundEvent placeSound = placedState.getSoundGroup().getPlaceSound();
        world.playSound(player, pos, placeSound, SoundCategory.BLOCKS,
                (placedState.getSoundGroup().getVolume() + 1.0f) / 2.0f,
                placedState.getSoundGroup().getPitch() * 0.8f);

        if (player == null || !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        return ActionResult.success(world.isClient);
    }
}
