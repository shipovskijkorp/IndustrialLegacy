package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.RubberWoodBlock;
import com.shipovskijkorp.industriallegacy.block.RubberWoodState;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import com.shipovskijkorp.industriallegacy.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * IC2 treetap (16 durability) — extracts sticky resin from rubber wood resin spots.
 *
 * Mechanics source of truth: IC2 1.12.2 ItemTreetap.
 */
public class TreetapItem extends Item {
    public TreetapItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (!state.isOf(ModBlocks.RUBBER_WOOD)) {
            return ActionResult.PASS;
        }

        // Always attempt extraction and always consume durability when used on rubber wood (IC2 behavior).
        attemptExtract(ctx.getPlayer(), world, pos, ctx.getSide(), state);

        if (!world.isClient) {
            PlayerEntity player = ctx.getPlayer();
            if (player != null) {
                ctx.getStack().damage(1, player, p -> p.sendToolBreakStatus(ctx.getHand()));
            }
        }

        return ActionResult.SUCCESS;
    }

    /**
     * @return true if resin was extracted (wet -> dry or dry proc).
     */
    private static boolean attemptExtract(PlayerEntity player, World world, BlockPos pos, Direction side, BlockState state) {
        RubberWoodState rwState = state.get(RubberWoodBlock.STATE);

        // Only side-matching resin states can be tapped.
        if (rwState.isPlain() || rwState.facing != side) {
            return false;
        }

        // Wet: always yields 1..3 resin and becomes dry.
        if (rwState.wet) {
            if (!world.isClient) {
                world.setBlockState(pos, state.with(RubberWoodBlock.STATE, rwState.getDry()), Block.NOTIFY_ALL);
                ejectResin(world, pos, side, world.getRandom().nextInt(3) + 1);
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.8f, 1.0f);
            }
            return true;
        }

        // Dry: 1/5 chance to lose the hole (becomes plain_y).
        if (!world.isClient && world.getRandom().nextInt(5) == 0) {
            world.setBlockState(pos, state.with(RubberWoodBlock.STATE, RubberWoodState.plain_y), Block.NOTIFY_ALL);
        }

        // Dry: independent 1/5 chance to yield 1 resin.
        if (world.getRandom().nextInt(5) == 0) {
            if (!world.isClient) {
                ejectResin(world, pos, side, 1);
                world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 0.8f, 1.0f);
            }
            return true;
        }

        return false;
    }

    private static void ejectResin(World world, BlockPos pos, Direction side, int quantity) {
        double ejectX = pos.getX() + 0.5 + side.getOffsetX() * 0.3;
        double ejectY = pos.getY() + 0.5 + side.getOffsetY() * 0.3;
        double ejectZ = pos.getZ() + 0.5 + side.getOffsetZ() * 0.3;

        for (int i = 0; i < quantity; ++i) {
            ItemEntity entity = new ItemEntity(world, ejectX, ejectY, ejectZ, new ItemStack(ModItems.STICKY_RESIN));
            entity.setToDefaultPickupDelay();
            world.spawnEntity(entity);
        }
    }
}
