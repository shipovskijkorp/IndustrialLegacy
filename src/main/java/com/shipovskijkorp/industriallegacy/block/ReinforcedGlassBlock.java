package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.GlassBlock;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Reinforced glass (IC2-like): explosion resistant glass block.
 */
public class ReinforcedGlassBlock extends GlassBlock {
    public ReinforcedGlassBlock() {
        super(AbstractBlock.Settings.create()
                .strength(0.3F, 1200.0F) // glass hardness, obsidian-like blast resistance
                .sounds(BlockSoundGroup.GLASS)
                .nonOpaque());
    }
}
