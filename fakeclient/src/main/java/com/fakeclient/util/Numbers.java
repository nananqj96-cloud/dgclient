/*
 * Decompiled with CFR 0.152.
 */
package com.fakeclient.util;

public final class Numbers {
    private Numbers() {
    }

    public static Double parseAmount(String s) {
        if (s == null) {
            return null;
        }
        if ((s = s.replace(",", "").replace("$", "").trim()).isEmpty()) {
            return null;
        }
        double mult = 1.0;
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        switch (last) {
            case 'k': {
                mult = 1000.0;
                break;
            }
            case 'm': {
                mult = 1000000.0;
                break;
            }
            case 'b': {
                mult = 1.0E9;
                break;
            }
            case 't': {
                mult = 1.0E12;
                break;
            }
        }
        if (mult != 1.0) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Double.parseDouble(s) * mult;
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static String formatAmount(double v) {
        String out;
        boolean neg = v < 0.0;
        if ((v = Math.abs(v)) < 1000.0) {
            out = Numbers.trimNumber(v);
        } else {
            int idx;
            String[] suffixes = new String[]{"", "K", "M", "B", "T"};
            for (idx = 0; v >= 1000.0 && idx < suffixes.length - 1; v /= 1000.0, ++idx) {
            }
            out = Numbers.trimNumber(v) + suffixes[idx];
        }
        return neg ? "-" + out : out;
    }

    public static double precisionOf(String s) {
        if (s == null) {
            return 1.0;
        }
        if ((s = s.replace(",", "").replace("$", "").trim()).isEmpty()) {
            return 1.0;
        }
        double mult = 1.0;
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        switch (last) {
            case 'k': {
                mult = 1000.0;
                break;
            }
            case 'm': {
                mult = 1000000.0;
                break;
            }
            case 'b': {
                mult = 1.0E9;
                break;
            }
            case 't': {
                mult = 1.0E12;
                break;
            }
        }
        String digits = mult != 1.0 ? s.substring(0, s.length() - 1) : s;
        int dot = digits.indexOf(46);
        int decimals = dot < 0 ? 0 : digits.length() - dot - 1;
        double unit = mult;
        for (int i = 0; i < decimals; ++i) {
            unit /= 10.0;
        }
        return Math.max(1.0, unit);
    }

    public static double roundToWholeUnit(double v) {
        double unit;
        boolean neg = v < 0.0;
        v = Math.abs(v);
        for (unit = 1.0; v / unit >= 1000.0 && unit < 1.0E12; unit *= 1000.0) {
        }
        double rounded = (double)Math.round(v / unit) * unit;
        return neg ? -rounded : rounded;
    }

    private static String trimNumber(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.valueOf((long)v);
        }
        return String.valueOf((double)Math.round(v * 100.0) / 100.0);
    }
}
