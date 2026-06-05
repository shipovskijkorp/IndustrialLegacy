package com.shipovskijkorp.industriallegacy.energy.heat;

import net.minecraft.util.math.Direction;

/** IC2 Experimental compatible heat source interface. */
public interface IHeatSource {
    int getConnectionBandwidth(Direction side);

    int drawHeat(Direction side, int request, boolean simulate);
}
