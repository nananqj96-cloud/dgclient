/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.suggestion.Suggestion
 *  com.mojang.brigadier.suggestion.Suggestions
 *  net.minecraft.class_2596
 *  net.minecraft.class_2805
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 */
package com.fakeclient.util;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.class_2596;
import net.minecraft.class_2805;
import net.minecraft.class_310;
import net.minecraft.class_634;

public final class OnlineHarvest {
    public static final int REQUEST_ID = 8019712;
    private static final String[] PROBES = new String[]{"/msg ", "/pay ", "/tell ", "/w "};
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789_";
    private static final int REQUEST_TICKS = 40;
    private static final int FIRST_DELAY_TICKS = 100;
    private static final Set<String> NAMES = new LinkedHashSet<String>();
    private static final Set<String> SWEEP = new LinkedHashSet<String>();
    private static int ticks = -60;
    private static int probe = 0;
    private static int letter = 0;

    private OnlineHarvest() {
    }

    public static void tick() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) {
            return;
        }
        if (++ticks < 40) {
            return;
        }
        ticks = 0;
        class_634 net = mc.method_1562();
        if (net == null) {
            return;
        }
        if (NAMES.isEmpty() && SWEEP.isEmpty() && letter == 0) {
            probe = (probe + 1) % PROBES.length;
        }
        net.method_52787((class_2596)new class_2805(8019712, PROBES[probe] + ALPHABET.charAt(letter)));
        if (++letter >= ALPHABET.length()) {
            letter = 0;
            OnlineHarvest.endSweep();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void endSweep() {
        Set<String> set = NAMES;
        synchronized (set) {
            if (!SWEEP.isEmpty()) {
                NAMES.clear();
                NAMES.addAll(SWEEP);
            }
            SWEEP.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void accept(Suggestions suggestions) {
        if (suggestions == null) {
            return;
        }
        Set<String> set = NAMES;
        synchronized (set) {
            for (Suggestion s : suggestions.getList()) {
                String text = s.getText();
                if (!OnlineHarvest.looksLikeName(text)) continue;
                SWEEP.add(text);
                NAMES.add(text);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<String> names() {
        ArrayList<String> out;
        class_310 mc = class_310.method_1551();
        String self = mc.field_1724 == null ? null : mc.field_1724.method_7334().name();
        Set<String> set = NAMES;
        synchronized (set) {
            out = new ArrayList<String>(NAMES);
        }
        if (self != null) {
            out.removeIf(n -> n.equalsIgnoreCase(self));
        }
        return out;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static int count() {
        Set<String> set = NAMES;
        synchronized (set) {
            return NAMES.size();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void forget() {
        Set<String> set = NAMES;
        synchronized (set) {
            NAMES.clear();
            SWEEP.clear();
        }
        ticks = -60;
        probe = 0;
        letter = 0;
    }

    private static boolean looksLikeName(String s) {
        if (s == null || s.isEmpty() || s.length() > 16) {
            return false;
        }
        String lower = s.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lower.length(); ++i) {
            boolean ok;
            char c = lower.charAt(i);
            boolean bl = ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_';
            if (ok) continue;
            return false;
        }
        return true;
    }
}
