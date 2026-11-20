/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.module.Module
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeScoreboard
 *  com.fakeclient.util.Numbers
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_3414
 *  net.minecraft.class_3417
 *  net.minecraft.class_5250
 */
package com.fakeclient.module.modules;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.module.Module;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeScoreboard;
import com.fakeclient.util.Numbers;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_5250;

public class FakePay
extends Module {
    public String command = "pay";
    public String currency = "$";

    public FakePay() {
        super("Fake Pay", "Blocks /pay, shows a fake receipt.");
    }

    public boolean matches(String raw) {
        return this.parse(raw) != null;
    }

    public boolean handleWithNotify(String raw) {
        String[] parts = this.parse(raw);
        if (parts == null) {
            return false;
        }
        this.showReceipt(parts[1], parts[2]);
        return true;
    }

    public void showReceipt(String player, String amountToken) {
        Double parsed = Numbers.parseAmount((String)amountToken);
        String amountStr = parsed != null ? Numbers.formatAmount((double)parsed) : amountToken;
        class_5250 message = class_2561.method_43470((String)"You paid ").method_27692(class_124.field_1068).method_10852((class_2561)class_2561.method_43470((String)player).method_27694(s -> s.method_36139(0xFFFFFF))).method_10852((class_2561)class_2561.method_43470((String)(" " + this.currency + " \u00a7f" + amountStr)).method_27694(s -> s.method_36139(65280)));
        class_310 mc = class_310.method_1551();
        Settings cfg = Settings.get();
        mc.execute(() -> FakePay.lambda$showReceipt$2(mc, cfg, (class_2561)message, parsed));
    }

    private String[] parse(String raw) {
        String[] parts;
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }
        if ((parts = trimmed.split("\\s+")).length < 3) {
            return null;
        }
        return parts[0].equalsIgnoreCase(this.command) ? parts : null;
    }

    private static /* synthetic */ void lambda$showReceipt$2(class_310 mc, Settings cfg, class_2561 message, Double parsed) {
        ModuleManager mm;
        FakeScoreboard sb;
        if (mc.field_1724 == null) {
            return;
        }
        if (cfg.fpActionBar) {
            mc.field_1724.method_7353(message, true);
        }
        if (cfg.fpChat) {
            mc.field_1724.method_7353(message, false);
        }
        if (cfg.fpSound) {
            mc.method_1483().method_4873((class_1113)class_1109.method_4758((class_3414)class_3417.field_15224, (float)1.0f));
        }
        FakeScoreboard fakeScoreboard = sb = (mm = FakeClient.modulesOrNull()) == null ? null : mm.fakeScoreboard;
        if (parsed != null && sb != null && sb.isEnabled()) {
            sb.subtractMoney(parsed.doubleValue());
        }
    }
}
