package com.shipovskijkorp.industriallegacy.item.tool;

import com.shipovskijkorp.industriallegacy.item.WrenchItem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/** IC2 Experimental electric wrench: 100 EU per wrench damage, 12 000 EU capacity, tier 1. */
public final class ElectricWrenchItem extends AbstractElectricToolItem {
    private static final long EU_PER_WRENCH_DAMAGE = 100L;
    private static final int ROTATE_DAMAGE = 1;
    private static final int REMOVE_DAMAGE = 10;

    public ElectricWrenchItem(Settings settings) {
        super(settings, EU_PER_WRENCH_DAMAGE, 0, 12_000L, 250L, 1, 12.0f);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        ItemStack stack = ctx.getStack();
        if (!canUse(stack, EU_PER_WRENCH_DAMAGE * ROTATE_DAMAGE)) {
            return ActionResult.FAIL;
        }

        boolean canRemove = canUse(stack, EU_PER_WRENCH_DAMAGE * REMOVE_DAMAGE);
        WrenchItem.WrenchResult result = WrenchItem.wrenchBlock(ctx, canRemove);
        if (result == WrenchItem.WrenchResult.NOTHING) {
            return ActionResult.FAIL;
        }

        if (!ctx.getWorld().isClient && ctx.getPlayer() != null) {
            int wrenchDamage = result == WrenchItem.WrenchResult.ROTATED ? ROTATE_DAMAGE : REMOVE_DAMAGE;
            useEnergy(stack, ctx.getPlayer(), EU_PER_WRENCH_DAMAGE * wrenchDamage);
        }
        return ActionResult.success(ctx.getWorld().isClient);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, net.minecraft.util.math.BlockPos pos, LivingEntity miner) {
        if (!world.isClient && WrenchItem.isWrenchMineable(state)) {
            useEnergy(stack, miner, EU_PER_WRENCH_DAMAGE);
        }
        return true;
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return WrenchItem.isWrenchMineable(state);
    }
}
