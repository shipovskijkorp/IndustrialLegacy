package com.shipovskijkorp.industriallegacy.block.entity.upgrade;

import com.shipovskijkorp.industriallegacy.item.UniversalFluidCellItem;

/** Minimal internal tank bridge used by IC2-style fluid ejector/pulling upgrades. */
public interface UpgradeableFluidMachine {
    /** Fill an internal input tank from an upgrade transfer. Returns accepted mB. */
    int fillFromUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate);

    /** Drain an internal output tank from an upgrade transfer. Returns drained mB. */
    int drainForUpgrade(UniversalFluidCellItem.CellFluid fluid, int amountMb, boolean simulate);

    /** Preferred fluid to drain when an upgrade has no explicit filter. */
    UniversalFluidCellItem.CellFluid getPreferredDrainFluidForUpgrade();
}
