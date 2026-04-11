package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.BatBoxBlock;
import com.shipovskijkorp.industriallegacy.block.CesuBlock;
import com.shipovskijkorp.industriallegacy.block.CompressorBlock;
import com.shipovskijkorp.industriallegacy.block.ElectricFurnaceBlock;
import com.shipovskijkorp.industriallegacy.block.EvTransformerBlock;
import com.shipovskijkorp.industriallegacy.block.GeneratorBlock;
import com.shipovskijkorp.industriallegacy.block.GeoGeneratorBlock;
import com.shipovskijkorp.industriallegacy.block.HvTransformerBlock;
import com.shipovskijkorp.industriallegacy.block.IronFurnaceBlock;
import com.shipovskijkorp.industriallegacy.block.LvTransformerBlock;
import com.shipovskijkorp.industriallegacy.block.MaceratorBlock;
import com.shipovskijkorp.industriallegacy.block.MetalFormerBlock;
import com.shipovskijkorp.industriallegacy.block.MfeBlock;
import com.shipovskijkorp.industriallegacy.block.MfsuBlock;
import com.shipovskijkorp.industriallegacy.block.MvTransformerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Basic IC2-style wrench.
 *
 * Rotation logic is still intentionally not implemented yet.
 * For now the wrench only acts as the correct harvesting tool for machine blocks,
 * so they break quickly and drop themselves via their loot tables.
 */
public class WrenchItem extends Item {
    private static final float MACHINE_MINING_SPEED = 12.0f;

    public WrenchItem(Settings settings) {
        super(settings.maxCount(1).maxDamage(120));
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return isWrenchMineable(state) ? MACHINE_MINING_SPEED : super.getMiningSpeedMultiplier(stack, state);
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return isWrenchMineable(state);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (!world.isClient && isWrenchMineable(state)) {
            stack.damage(1, miner, e -> e.sendEquipmentBreakStatus(net.minecraft.entity.EquipmentSlot.MAINHAND));
        }
        return true;
    }

    private static boolean isWrenchMineable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof GeneratorBlock
                || block instanceof GeoGeneratorBlock
                || block instanceof BatBoxBlock
                || block instanceof CesuBlock
                || block instanceof MfeBlock
                || block instanceof MfsuBlock
                || block instanceof LvTransformerBlock
                || block instanceof MvTransformerBlock
                || block instanceof HvTransformerBlock
                || block instanceof EvTransformerBlock
                || block instanceof MaceratorBlock
                || block instanceof CompressorBlock
                || block instanceof MetalFormerBlock
                || block instanceof ElectricFurnaceBlock
                || block instanceof IronFurnaceBlock;
    }
}
