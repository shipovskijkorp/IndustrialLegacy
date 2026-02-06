package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.block.entity.GeneratorBlockEntity;
import com.shipovskijkorp.industriallegacy.energy.IEuEnergyStorage;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Tiny dev tool so you can verify EU logic quickly without a GUI.
 */
public class DebugWrenchItem extends Item {

    public DebugWrenchItem(Settings settings) {
        super(settings);
    }

    /**
     * Always render with enchantment glint (visual only, no enchantments).
     */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        if (!(context.getPlayer() instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }

        BlockPos pos = context.getBlockPos();
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) {
            player.sendMessage(Text.literal("No block entity"), false);
            return ActionResult.SUCCESS;
        }

        if (be instanceof IEuEnergyStorage eu) {
            player.sendMessage(Text.literal(
                    "EU: " + eu.getEuStored() + "/" + eu.getEuCapacity()
            ), false);
            player.sendMessage(Text.literal(
                    "Tiers: sink=" + eu.getSinkTier(context.getSide())
                            + " source=" + eu.getSourceTier(context.getSide())
            ), false);
        } else {
            player.sendMessage(
                    Text.literal("BE: " + be.getClass().getSimpleName()),
                    false
            );
        }

        if (be instanceof GeneratorBlockEntity gen) {
            player.sendMessage(
                    Text.literal("Fuel: " + gen.getFuel() + "/" + gen.getTotalFuel()),
                    false
            );
            player.sendMessage(
                    Text.literal("Production: " + gen.getProduction() + " EU/t"),
                    false
            );
        }

        return ActionResult.SUCCESS;
    }
}
