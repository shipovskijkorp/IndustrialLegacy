package com.shipovskijkorp.industriallegacy.energy.kinetic;

import net.minecraft.util.math.Direction;

/** Minimal IL-like kinetic source interface measured in KU. */
public interface IKineticSource {
    int getConnectionBandwidth(Direction side);

    int drawKineticEnergy(Direction side, int request, boolean simulate);
}
