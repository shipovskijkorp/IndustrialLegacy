package com.shipovskijkorp.industriallegacy.item;

import com.shipovskijkorp.industriallegacy.energy.util.EuUtil;

import java.util.Locale;

/**
 * Cable kinds with IL 1.12.2-style parameters (non-classic).
 *
 * <p>Values are mirrored from IL {@code CableType} defaults and related
 * breakdown/absorption rules in {@code TileEntityCable}.</p>
 */
public enum CableKind {
    // maxInsulation, thickness, loss, capacity, tier
    COPPER(1, 0.25f, 0.2, 128, 1),
    TIN(1, 0.25f, 0.2, 32, 0),
    GOLD(2, 0.1875f, 0.4, 512, 2),
    IRON(3, 0.375f, 0.8, 2048, 3),
    GLASS(0, 0.25f, 0.025, 8192, 5),
    DETECTOR(0, 0.5f, 0.5, 8192, 5),
    SPLITTER(0, 0.5f, 0.5, 8192, 5);

    /** Maximum insulation level stored in NBT. */
    public final int maxInsulation;

    /** Base rendered thickness (0..1 block units). */
    public final float thickness;

    /** Conduction loss per block. */
    public final double loss;

    /** EU packet capacity (conductor breakdown is {@code capacity + 1}). */
    public final int capacity;

    /** IL-ish tier (approx.). */
    public final int tier;

    CableKind(int maxInsulation, float thickness, double loss, int capacity, int tier) {
        this.maxInsulation = maxInsulation;
        this.thickness = thickness;
        this.loss = loss;
        this.capacity = capacity;
        this.tier = tier;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static CableKind fromId(String id) {
        if (id == null) return COPPER;
        for (CableKind k : values()) {
            if (k.id().equals(id)) return k;
        }
        return COPPER;
    }

    public int clampInsulation(int insulation) {
        return Math.max(0, Math.min(maxInsulation, insulation));
    }

    /** IL: {@code capacity + 1}. */
    public double getConductorBreakdownEnergy() {
        return (double) capacity + 1.0;
    }

    /** IL constant. */
    public double getInsulationBreakdownEnergy() {
        return 9001.0;
    }

    /**
     * IL rules (non-classic):
     * <ul>
     *   <li>If the cable has no insulation, absorption is effectively infinite.</li>
     *   <li>For tin cables: {@code powerFromTier(insulation)}.</li>
     *   <li>For all others: {@code powerFromTier(insulation + 1)}.</li>
     * </ul>
     */
    public double getInsulationEnergyAbsorption(int insulation) {
        if (maxInsulation == 0) {
            return 2.147483647E9; // Integer.MAX_VALUE as double
        }
        int ins = clampInsulation(insulation);
        if (this == TIN) {
            return (double) EuUtil.powerFromTier(ins);
        }
        return (double) EuUtil.powerFromTier(ins + 1);
    }
}
