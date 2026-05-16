package com.shipovskijkorp.industriallegacy.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.DyeColor;

/** IC2 CF wall / foam concrete. IC2 default color is light gray. */
public class FoamConcreteBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.of("color", DyeColor.class);

    public FoamConcreteBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(COLOR, DyeColor.LIGHT_GRAY));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }
}
