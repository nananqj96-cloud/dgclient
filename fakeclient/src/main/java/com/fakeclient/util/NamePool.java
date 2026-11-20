/*
 * Decompiled with CFR 0.152.
 */
package com.fakeclient.util;

import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class NamePool {
    private static final int SIZE = 1000;
    private static final int MAX_LEN = 16;
    private static final String[] FIRST = new String[]{"Shadow", "Dark", "Ender", "Diamond", "Nether", "Frost", "Blaze", "Void", "Storm", "Iron", "Ghost", "Cyber", "Toxic", "Rapid", "Swift", "Silent", "Crimson", "Astro", "Lunar", "Solar", "Neon", "Turbo", "Hyper", "Mega", "Ultra", "Prime", "Elite", "Royal", "Savage", "Rogue", "Mystic", "Phantom", "Titan", "Vortex", "Zenith", "Quantum", "Arctic", "Ember", "Onyx", "Cobalt", "Venom", "Raven", "Falcon", "Nova", "Pixel", "Chrome", "Static", "Echo"};
    private static final String[] SECOND = new String[]{"Slayer", "Craft", "Miner", "Gamer", "Wolf", "Fox", "Hawk", "Blade", "Storm", "Byte", "King", "Lord", "Knight", "Hunter", "Reaper", "Striker", "Sniper", "Ninja", "Samurai", "Dragon", "Phoenix", "Viper", "Tiger", "Panda", "Bear", "Shark", "Spirit", "Bolt", "Flame", "Core", "Rush", "Edge", "Wave", "Beam", "Forge", "Pulse"};
    private static List<String> cached;

    private NamePool() {
    }

    public static List<String> get() {
        if (cached == null) {
            cached = NamePool.build();
        }
        return cached;
    }

    private static List<String> build() {
        ArrayList<String> out = new ArrayList<String>(1000);
        HashSet<CallSite> seen = new HashSet<CallSite>(2000);
        Random rng = new Random(4199465246L);
        int guard = 60000;
        while (out.size() < 1000 && guard-- > 0) {
            String name;
            String a = FIRST[rng.nextInt(FIRST.length)];
            String b = SECOND[rng.nextInt(SECOND.length)];
            if ((name = (switch (rng.nextInt(6)) {
                case 0 -> a + b;
                case 1 -> a + b + (10 + rng.nextInt(90));
                case 2 -> (a + "_" + b).toLowerCase(Locale.ROOT);
                case 3 -> "x" + a + b + "x";
                case 4 -> a + "_" + b;
                default -> a + b + (100 + rng.nextInt(900));
            })).length() > 16 || !seen.add((CallSite)((Object)name))) continue;
            out.add(name);
        }
        return out;
    }
}
