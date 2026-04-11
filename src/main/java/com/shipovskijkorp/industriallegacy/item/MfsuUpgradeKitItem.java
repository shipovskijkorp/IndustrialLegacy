package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.MfeBlock;
import com.shipovskijkorp.industriallegacy.block.MfsuBlock;
import com.shipovskijkorp.industriallegacy.block.entity.MfeBlockEntity;
import com.shipovskijkorp.industriallegacy.block.entity.MfsuBlockEntity;
import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** IC2-like MFE -> MFSU upgrade kit. */
public class MfsuUpgradeKitItem extends Item {
    public MfsuUpgradeKitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(net.minecraft.item.ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof MfeBlock)) {
            return ActionResult.PASS;
        }
        if (!(world.getBlockEntity(pos) instanceof MfeBlockEntity oldBe)) {
            return ActionResult.PASS;
        }

        NbtCompound nbt = oldBe.createNbt();

        BlockState newState = ModBlocks.MFSU.getDefaultState().with(MfsuBlock.FACING, state.get(MfeBlock.FACING));
        world.setBlockState(pos, newState, 3);

        if (world.getBlockEntity(pos) instanceof MfsuBlockEntity newBe) {
            newBe.readNbt(nbt);
            newBe.markDirty();
            if (world instanceof ServerWorld sw) {
                sw.getChunkManager().markForUpdate(pos);
            }
        }

        PlayerEntity player = context.getPlayer();
        if (player == null || !player.getAbilities().creativeMode) {
            ItemStack stack = context.getStack();
            stack.decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
