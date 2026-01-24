package com.shipovskijkorp.industriallegacy.energy;

/**
 * EU math helpers matching IC2 tier math.
 */
public final class EuUtil {
    private EuUtil() {}

    /**
     * Equivalent of IC2's EnergyNet.getPowerFromTier(tier).
     *
     * <p>Returned value is the nominal packet size for the tier.</p>
     */
    public static double powerFromTierD(int tier) {
        if (tier < 0) tier = 0;

        // IC2 uses fast integer math up to tier 13.
        if (tier < 14) {
            return (double) (8L << (tier * 2));
        }

        // Beyond that, IC2 uses doubles.
        double v = 8.0 * Math.pow(4.0, (double) tier);
        if (!Double.isFinite(v) || v >= Double.MAX_VALUE) {
            return Double.MAX_VALUE;
        }
        return v;
    }

    /**
     * Long variant for convenience; clamps to {@link Long#MAX_VALUE}.
     */
    public static long powerFromTier(int tier) {
        double v = powerFromTierD(tier);
        if (v >= (double) Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        // Keep behavior deterministic.
        return (long) Math.floor(v);
    }

    /**
     * Equivalent of IC2's EnergyNet.getTierFromPower(power).
     */
    public static int tierFromPower(double power) {
        if (power <= 0.0) return 0;

        // ceil(log(power/8)/log(4))
        double p = Math.max(1.0, power);
        double t = Math.log(p / 8.0) / Math.log(4.0);
        int tier = (int) Math.ceil(t);
        return Math.max(0, tier);
    }

    public static int tierFromPower(long power) {
        return tierFromPower((double) power);
    }
}
