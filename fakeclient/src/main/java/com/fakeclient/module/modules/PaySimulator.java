/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.PaySimulatorScreen
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeScoreboard
 *  com.fakeclient.util.FakeOnline
 *  com.fakeclient.util.NamePool
 *  com.fakeclient.util.Numbers
 *  com.fakeclient.util.OnlineHarvest
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_5250
 */
package com.fakeclient.module.modules;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.PaySimulatorScreen;
import com.fakeclient.module.Module;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeScoreboard;
import com.fakeclient.util.FakeOnline;
import com.fakeclient.util.NamePool;
import com.fakeclient.util.Numbers;
import com.fakeclient.util.OnlineHarvest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_5250;

public class PaySimulator
extends Module {
    private static final int WHITE = 0xFFFFFF;
    private static final int GREEN = 0x55FF55;
    private final Random rng = new Random();
    private int ticksLeft;
    private static final int RECENT_MEMORY = 12;
    private final List<String> recent = new ArrayList<String>();

    public PaySimulator() {
        super("Pay Simulator", "Fake incoming payments at random intervals.");
        this.setEditor(() -> new PaySimulatorScreen(this));
    }

    protected void onEnable() {
        this.schedule();
    }

    public void tick() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 == null || mc.field_1687 == null) {
            return;
        }
        if (--this.ticksLeft > 0) {
            return;
        }
        this.fire(mc);
        this.schedule();
    }

    public void fireNow() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null) {
            this.fire(mc);
        }
    }

    private void fire(class_310 mc) {
        FakeScoreboard sb;
        String name;
        Settings s = Settings.get();
        double lo = Math.min(s.paySimMin, s.paySimMax);
        double hi = Math.max(s.paySimMin, s.paySimMax);
        double amount = lo + this.rng.nextDouble() * (hi - lo);
        if (s.paySimWholeNumbers) {
            amount = Numbers.roundToWholeUnit((double)amount);
        }
        if ((name = this.pickName()) == null) {
            return;
        }
        if (!OnlineHarvest.names().contains(name)) {
            FakeOnline.add((String)name);
        }
        class_5250 message = class_2561.method_43470((String)name).method_27694(st -> st.method_36139(0xFFFFFF)).method_10852((class_2561)class_2561.method_43470((String)" paid you ").method_27694(st -> st.method_36139(0xFFFFFF))).method_10852((class_2561)class_2561.method_43470((String)"$").method_27694(st -> st.method_36139(0x55FF55))).method_10852((class_2561)class_2561.method_43470((String)(" " + PaySimulator.shorthand(amount))).method_27694(st -> st.method_36139(0xFFFFFF)));
        mc.field_1724.method_7353((class_2561)message, false);
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeScoreboard fakeScoreboard = sb = mm == null ? null : mm.fakeScoreboard;
        if (sb != null && sb.isEnabled()) {
            sb.addMoney(amount);
        }
    }

    private void schedule() {
        Settings s = Settings.get();
        double lo = Math.max(1.0, Math.min(s.paySimMinDelay, s.paySimMaxDelay));
        double hi = Math.max(lo, Math.max(s.paySimMinDelay, s.paySimMaxDelay));
        double seconds = lo + this.rng.nextDouble() * (hi - lo);
        this.ticksLeft = Math.max(1, (int)Math.round(seconds * 20.0));
    }

    private String pickName() {
        boolean real;
        List pool = OnlineHarvest.names();
        boolean bl = real = !pool.isEmpty();
        if (!real) {
            pool = NamePool.get();
        }
        if (pool.isEmpty()) {
            return null;
        }
        String name = (String)pool.get(this.rng.nextInt(pool.size()));
        for (int tries = 0; tries < 8 && this.recent.contains(name); ++tries) {
            name = (String)pool.get(this.rng.nextInt(pool.size()));
        }
        this.recent.add(name);
        while (this.recent.size() > 12) {
            this.recent.remove(0);
        }
        return name;
    }

    public static int poolSize() {
        int real = OnlineHarvest.count();
        return real > 0 ? real : NamePool.get().size();
    }

    public static boolean poolIsReal() {
        return OnlineHarvest.count() > 0;
    }

    public static String comma(double v) {
        return String.format(Locale.US, "%,d", Math.round(Math.max(0.0, v)));
    }

    public static String shorthand(double v) {
        return Numbers.formatAmount((double)Math.max(0.0, v)).toUpperCase(Locale.ROOT);
    }
}
