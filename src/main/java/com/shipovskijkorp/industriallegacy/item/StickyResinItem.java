package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Sticky resin places a resin/latex sheet when used on the top face of a block, like IL. */
public class StickyResinItem extends Item {
    public StickyResinItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getSide() != Direction.UP) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos().up();
        BlockState sheet = ModBlocks.RESIN_SHEET.getDefaultState();
        ItemPlacementContext placementContext = new ItemPlacementContext(context);

        if (!world.getBlockState(pos).canReplace(placementContext) || !sheet.canPlaceAt(world, pos)) {
            return ActionResult.PASS;
        }

        if (!world.isClient) {
            world.setBlockState(pos, sheet, net.minecraft.block.Block.NOTIFY_ALL);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                context.getStack().decrement(1);
            }
        }

        return ActionResult.success(world.isClient);
    }
}
