package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.sound.BlockSoundGroup;

/**
 * IC2 Experimental Luminator (simplified physical block):
 * - always emits light 15
 * - non-opaque (glass-like)
 *
 * Energy storage/charging mechanics are intentionally omitted for now.
 * This is mainly used as a crafting component (e.g. Nightvision Goggles), like in many IC2 packs.
 */
public class LuminatorBlock extends Block {
    public LuminatorBlock() {
        super(AbstractBlock.Settings.create()
                .strength(0.2f)
                .sounds(BlockSoundGroup.GLASS)
                .luminance(state -> 15)
                .nonOpaque());
    }
}
