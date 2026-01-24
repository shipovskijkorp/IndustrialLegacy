package com.shipovskijkorp.industriallegacy.energy;

/**
 * Legacy compatibility wrapper. Prefer {@link com.shipovskijkorp.industriallegacy.energy.util.EuUtil}.
 */
@Deprecated
public final class EuUtil {
    private EuUtil() {}

    public static long powerFromTier(int tier) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.powerFromTier(tier);
    }

    public static double powerFromTierD(int tier) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.powerFromTierD(tier);
    }

    public static int tierFromPower(double power) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.tierFromPower(power);
    }
}
