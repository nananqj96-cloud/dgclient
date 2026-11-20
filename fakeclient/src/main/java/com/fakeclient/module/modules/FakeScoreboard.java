/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.gui.FakeScoreboardScreen
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.util.Numbers
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_266
 *  net.minecraft.class_268
 *  net.minecraft.class_269
 *  net.minecraft.class_274
 *  net.minecraft.class_274$class_275
 *  net.minecraft.class_310
 *  net.minecraft.class_5250
 *  net.minecraft.class_5251
 *  net.minecraft.class_638
 *  net.minecraft.class_8646
 *  net.minecraft.class_9014
 *  net.minecraft.class_9015
 *  net.minecraft.class_9020
 *  net.minecraft.class_9022
 */
package com.fakeclient.module.modules;

import com.fakeclient.FakeClient;
import com.fakeclient.gui.FakeScoreboardScreen;
import com.fakeclient.module.Module;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.util.Numbers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_266;
import net.minecraft.class_268;
import net.minecraft.class_269;
import net.minecraft.class_274;
import net.minecraft.class_310;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import net.minecraft.class_638;
import net.minecraft.class_8646;
import net.minecraft.class_9014;
import net.minecraft.class_9015;
import net.minecraft.class_9020;
import net.minecraft.class_9022;

public class FakeScoreboard
extends Module {
    private static final String OBJECTIVE_NAME = "fakeclient_sb";
    private static final String TEAM_PREFIX = "fakeclient_";
    private static final long UPDATE_INTERVAL_MS = 1000L;
    public String title = "Nxdih";
    public String money = "64.9M";
    public String star = "20";
    public String sword = "4";
    public String skull = "10";
    public String clock = "4h 34m";
    public boolean overrideBalance = true;
    private final List<String> teamNames = new ArrayList<String>();
    private final Random rng = new Random();
    private double exact;
    private class_266 customObjective;
    private class_266 originalObjective;
    private boolean savedOriginal;
    private boolean pendingApply;
    private long lastUpdate;
    private static final Pattern BALANCE = Pattern.compile("(?i)^\\s*you have\\s*\\$\\s*[\\d,]+(?:\\.\\d+)?\\s*$");
    private static final Pattern INCOMING_PAY = Pattern.compile("(?i)^\\s*(\\S+)\\s+paid you\\s*\\$\\s*([\\d,.]+[kmbt]?)\\s*$");
    private static final String[] ICONS = new String[]{"$", "\u2605", "\u2694", "\u2620", "\u231a"};
    private static final int[] ICON_COLORS = new int[]{0x55FF55, 0xAA55FF, 0xFF5555, 0xFFAA00, 0xFFD000};

    public FakeScoreboard() {
        super("Fake Scoreboard", "Fake custom sidebar scoreboard.");
        this.setEditor(() -> new FakeScoreboardScreen(this));
        this.exact = this.randomized(this.money);
    }

    protected void onEnable() {
        this.pendingApply = true;
        this.tryApply();
    }

    protected void onDisable() {
        this.restore();
        this.pendingApply = false;
    }

    public void tick() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null || mc.field_1724 == null) {
            this.savedOriginal = false;
            this.customObjective = null;
            this.teamNames.clear();
            this.pendingApply = true;
            return;
        }
        if (this.pendingApply) {
            this.tryApply();
        } else if (System.currentTimeMillis() - this.lastUpdate >= 1000L) {
            this.apply();
        }
    }

    public String exactMoney() {
        double v = this.exact;
        return v == Math.floor(v) ? String.format(Locale.US, "%,d", (long)v) : String.format(Locale.US, "%,.2f", v);
    }

    public void setMoney(String shorthand) {
        this.money = shorthand == null ? "" : shorthand;
        this.exact = this.randomized(this.money);
        this.refreshNow();
    }

    private double randomized(String shorthand) {
        Double base = Numbers.parseAmount((String)shorthand);
        if (base == null || base < 0.0) {
            return 0.0;
        }
        double unit = Numbers.precisionOf((String)shorthand);
        double noise = unit > 1.0 ? Math.floor(this.rng.nextDouble() * unit) : 0.0;
        return base + noise;
    }

    public static class_2561 rewriteBalance(class_2561 message) {
        FakeScoreboard sb;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeScoreboard fakeScoreboard = sb = mm == null ? null : mm.fakeScoreboard;
        if (sb == null || !sb.isEnabled() || !sb.overrideBalance || message == null) {
            return message;
        }
        if (!BALANCE.matcher(message.getString()).matches()) {
            return message;
        }
        return class_2561.method_43470((String)"You have ").method_27694(s -> s.method_36139(0xFFFFFF)).method_10852((class_2561)class_2561.method_43470((String)"$").method_27694(s -> s.method_36139(0x55FF55))).method_10852((class_2561)class_2561.method_43470((String)(" " + sb.exactMoney())).method_27694(s -> s.method_36139(0xFFFFFF)));
    }

    public static void observeIncomingPayment(class_2561 message) {
        FakeScoreboard sb;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeScoreboard fakeScoreboard = sb = mm == null ? null : mm.fakeScoreboard;
        if (sb == null || !sb.isEnabled() || message == null) {
            return;
        }
        Matcher m = INCOMING_PAY.matcher(message.getString().trim());
        if (!m.matches()) {
            return;
        }
        Double amount = Numbers.parseAmount((String)m.group(2));
        if (amount != null && amount > 0.0) {
            sb.addMoney(amount);
        }
    }

    public void refreshNow() {
        class_310 mc = class_310.method_1551();
        if (this.isEnabled() && mc.field_1687 != null && mc.field_1724 != null) {
            this.apply();
        }
    }

    public void subtractMoney(double amount) {
        this.addMoney(-amount);
    }

    public void addMoney(double amount) {
        this.exact = Math.max(0.0, this.exact + amount);
        this.money = Numbers.formatAmount((double)this.exact);
        this.refreshNow();
    }

    private void tryApply() {
        class_310 mc = class_310.method_1551();
        if (mc.field_1687 == null || mc.field_1724 == null) {
            return;
        }
        if (!this.savedOriginal) {
            this.originalObjective = mc.field_1687.method_8428().method_1189(class_8646.field_45157);
            this.savedOriginal = true;
        }
        this.apply();
        this.pendingApply = false;
    }

    private void apply() {
        class_638 world = class_310.method_1551().field_1687;
        if (world == null) {
            return;
        }
        class_269 scoreboard = world.method_8428();
        this.cleanupTeams(scoreboard);
        if (this.customObjective != null) {
            try {
                scoreboard.method_1194(this.customObjective);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        this.customObjective = scoreboard.method_1168(OBJECTIVE_NAME, class_274.field_1468, (class_2561)this.colored(this.title, 0xFFFFFF), class_274.class_275.field_1472, false, (class_9022)class_9020.field_47557);
        scoreboard.method_1158(class_8646.field_45157, this.customObjective);
        List<class_5250> lines = this.lines();
        for (int i = 0; i < lines.size(); ++i) {
            String teamName = TEAM_PREFIX + i;
            this.teamNames.add(teamName);
            class_268 existing = scoreboard.method_1153(teamName);
            if (existing != null) {
                scoreboard.method_1191(existing);
            }
            class_268 team = scoreboard.method_1171(teamName);
            team.method_1138((class_2561)lines.get(i));
            String holderName = "\u00a7" + Integer.toHexString(i);
            class_9015 holder = class_9015.method_55422((String)holderName);
            scoreboard.method_1155(holder, this.customObjective);
            class_9014 score = scoreboard.method_1180(holder, this.customObjective);
            score.method_55410(lines.size() - i);
            scoreboard.method_1172(holderName, team);
        }
        this.lastUpdate = System.currentTimeMillis();
    }

    private void restore() {
        class_638 world = class_310.method_1551().field_1687;
        if (world == null) {
            this.savedOriginal = false;
            this.customObjective = null;
            this.teamNames.clear();
            return;
        }
        try {
            class_269 scoreboard = world.method_8428();
            this.cleanupTeams(scoreboard);
            if (this.customObjective != null) {
                try {
                    scoreboard.method_1194(this.customObjective);
                }
                catch (Exception exception) {
                    // empty catch block
                }
                this.customObjective = null;
            }
            try {
                scoreboard.method_1158(class_8646.field_45157, this.originalObjective);
            }
            catch (Exception e) {
                scoreboard.method_1158(class_8646.field_45157, null);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.originalObjective = null;
        this.savedOriginal = false;
    }

    private void cleanupTeams(class_269 scoreboard) {
        for (String name : this.teamNames) {
            try {
                class_268 team = scoreboard.method_1153(name);
                if (team == null) continue;
                scoreboard.method_1191(team);
            }
            catch (Exception exception) {}
        }
        this.teamNames.clear();
    }

    private List<class_5250> lines() {
        String[] values = new String[]{this.money.toUpperCase(), this.star, this.sword, this.skull, this.clock};
        ArrayList<class_5250> list = new ArrayList<class_5250>();
        for (int i = 0; i < ICONS.length; ++i) {
            list.add(this.colored(ICONS[i], ICON_COLORS[i]).method_10852((class_2561)this.colored(" " + values[i], 0xFFFFFF)));
        }
        return list;
    }

    private class_5250 colored(String s, int rgb) {
        return class_2561.method_43470((String)s).method_10862(class_2583.field_24360.method_27703(class_5251.method_27717((int)rgb)));
    }
}
