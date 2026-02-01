package com.shipovskijkorp.industriallegacy.energy.api;

import net.minecraft.util.math.Direction;
import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;

/**
 * Minimal EU storage interface used by Industrial Legacy.
 *
 * <p>This is intentionally close to the concepts IL uses (offered energy, demanded energy,
 * packet tiers) while keeping a small surface area for the early porting phases.</p>
 */
public interface IEuEnergyStorage {
    // --- Core (legacy) API used around the project ---

    long getEuStored();

    long getEuCapacity();

    /** IL-style tier (0..). Determines the maximum accepted packet size. */
    int getSinkTier();

    /** IL-style tier (0..). Determines the maximum emitted packet size. */
    int getSourceTier();

    /** Side-aware sink tier. Default: same as {@link #getSinkTier()}. */
    default int getSinkTier(Direction side) {
        return getSinkTier();
    }

    /** Side-aware source tier. Default: same as {@link #getSourceTier()}. */
    default int getSourceTier(Direction side) {
        return getSourceTier();
    }


    /** @return accepted amount */
    long insertEu(long amount, Direction from, boolean simulate);

    /** @return extracted amount */
    long extractEu(long amount, Direction to, boolean simulate);

    boolean canInsert(Direction from);

    boolean canExtract(Direction to);

    // --- IL-like helper API (defaults keep old implementors working) ---

    /**
     * If {@code true}, the source should only emit full packets (>= tier packet size).
     * IL uses this for storage blocks (batbox/mfe/mfsu).
     */
    default boolean isFullEnergyOutput() {
        return false;
    }

    /**
     * If {@code true}, the source may emit multiple packets per tick.
     */
    default boolean sendMultipleEnergyPackets() {
        return false;
    }

    /**
     * Maximum packet count per tick when {@link #sendMultipleEnergyPackets()} is enabled.
     */
    default int getMaxEnergyPacketCount() {
        return 1;
    }

    /**
     * Offered energy (EU) from this tile for the current tick.
     *
     * <p>Default: stored energy, optionally gated by {@link #isFullEnergyOutput()}.</p>
     */
    default double getOfferedEnergy() {
        if (getEuStored() <= 0) return 0.0;

        double stored = (double) getEuStored();
        if (isFullEnergyOutput()) {
            double packet = EuUtil.powerFromTierD(getSourceTier());
            return stored >= packet ? stored : 0.0;
        }
        return stored;
    }

    /**
     * Side-aware offered energy.
     */
    default double getOfferedEnergy(Direction to) {
        return canExtract(to) ? getOfferedEnergy() : 0.0;
    }

    /**
     * Demanded energy (EU) for this tile for the current tick.
     *
     * <p>Default: free space in storage.</p>
     */
    default double getDemandedEnergy() {
        long cap = getEuCapacity();
        long stored = getEuStored();
        if (cap <= 0) return 0.0;
        return Math.max(0.0, (double) cap - (double) stored);
    }

    default double getDemandedEnergy(Direction from) {
        return canInsert(from) ? getDemandedEnergy() : 0.0;
    }

    /**
     * Inject energy like IL: returns rejected amount.
     */
    default double injectEnergy(Direction from, double amount, int voltageTier, boolean simulate) {
        if (amount <= 0.0) return 0.0;
        if (!canInsert(from)) return amount;

        // For now we accept integer EU into storage, like most IL machine storages.
        long want = (long) Math.floor(amount);
        if (want <= 0) return amount;

        long accepted = insertEu(want, from, simulate);
        double rej = amount - (double) accepted;
        return Math.max(0.0, rej);
    }

    /**
     * Draw energy from a source like IL: returns actually drawn amount.
     */
    default double drawEnergy(double amount, boolean simulate) {
        if (amount <= 0.0) return 0.0;

        long want = (long) Math.ceil(amount);
        if (want <= 0) return 0.0;

        // Direction is irrelevant for most internal storages; pick UP by default.
        long drawn = extractEu(want, Direction.UP, simulate);
        return (double) drawn;
    }
}
