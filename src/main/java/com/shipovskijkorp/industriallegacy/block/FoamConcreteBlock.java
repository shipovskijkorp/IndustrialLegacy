package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.Block;
import net.minecraft.util.DyeColor;

/** IL CF wall / foam concrete. IL default color is light gray. */
public class FoamConcreteBlock extends Block {
    private final DyeColor color;

    public FoamConcreteBlock(Settings settings) {
        this(settings, DyeColor.LIGHT_GRAY);
    }

    public FoamConcreteBlock(Settings settings, DyeColor color) {
        super(settings);
        this.color = color;
    }

    public DyeColor getColor() {
        return color;
    }
}
