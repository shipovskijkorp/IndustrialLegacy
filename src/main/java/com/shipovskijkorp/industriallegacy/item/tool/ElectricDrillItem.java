package com.shipovskijkorp.industriallegacy.item.tool;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;

/** IC2 mining drill / diamond drill semantics: pickaxe + shovel electric tool. */
public class ElectricDrillItem extends AbstractElectricToolItem {
    public ElectricDrillItem(Settings settings, long operationEnergyCost, int harvestLevel,
                             long capacityEu, long transferLimitEu, int tier, float efficiency) {
        super(settings, operationEnergyCost, harvestLevel, capacityEu, transferLimitEu, tier, efficiency);
    }

    @Override
    protected boolean isEffectiveOn(BlockState state) {
        return state.isIn(BlockTags.PICKAXE_MINEABLE)
                || state.isIn(BlockTags.SHOVEL_MINEABLE)
                || state.isOf(Blocks.OBSIDIAN)
                || state.isOf(Blocks.REDSTONE_ORE)
                || state.isOf(Blocks.DEEPSLATE_REDSTONE_ORE);
    }
}
