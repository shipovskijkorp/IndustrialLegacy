package com.shipovskijkorp.industriallegacy.energy;

/**
 * Legacy compatibility wrapper for EU tier math.
 *
 * <p>New code should use {@link com.shipovskijkorp.industriallegacy.energy.util.EuUtil}.</p>
 */
public final class EuUtil {
    private EuUtil() {}

    public static double powerFromTierD(int tier) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.powerFromTierD(tier);
    }

    public static long powerFromTier(int tier) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.powerFromTier(tier);
    }

    public static int tierFromPower(double power) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.tierFromPower(power);
    }

    public static int tierFromPower(long power) {
        return com.shipovskijkorp.industriallegacy.energy.util.EuUtil.tierFromPower(power);
    }
}
