package com.shipovskijkorp.industriallegacy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * IL-style EU formatting helpers.
 */
public final class EnergyDisplayUtil {
    private static final String[] SI = {"", "k", "M", "G", "T", "P", "E"};

    private EnergyDisplayUtil() {}

    public static String toSiString(double value, int digits) {
        if (!Double.isFinite(value)) return "0";
        if (digits < 1) digits = 1;

        double abs = Math.abs(value);
        int exp = 0;
        while (abs >= 1000.0 && exp < SI.length - 1) {
            abs /= 1000.0;
            exp++;
        }

        double scaled = value / Math.pow(1000.0, exp);
        int intDigits = scaled == 0.0 ? 1 : Math.max(1, (int) Math.floor(Math.log10(Math.abs(scaled))) + 1);
        int fracDigits = Math.max(0, digits - intDigits);

        BigDecimal bd = BigDecimal.valueOf(scaled).setScale(fracDigits, RoundingMode.HALF_UP).stripTrailingZeros();
        String number = bd.toPlainString();
        if (number.equals("-0")) number = "0";

        return SI[exp].isEmpty() ? number : number + " " + SI[exp];
    }

    public static String formatEuValue(double value, int digits) {
        return toSiString(value, digits) + " EU";
    }

    public static String formatEuStorage(double stored, double capacity, int digits) {
        return toSiString(stored, digits) + "/" + toSiString(capacity, digits) + " EU";
    }

    public static String formatEuStorage(long stored, long capacity, int digits) {
        return formatEuStorage((double) stored, (double) capacity, digits);
    }

    public static String formatEuPerTick(double value, int digits) {
        return toSiString(value, digits) + " EU/t";
    }
}
