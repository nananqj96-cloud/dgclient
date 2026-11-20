/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.module.modules.PaySimulator
 *  com.fakeclient.util.Numbers
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 */
package com.fakeclient.gui;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import com.fakeclient.gui.Gui;
import com.fakeclient.module.modules.PaySimulator;
import com.fakeclient.util.Numbers;
import com.fakeclient.util.Sfx;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public class PaySimulatorScreen
extends class_437 {
    private static final int PANEL_W = 380;
    private static final int HEADER_H = 38;
    private static final int MARGIN = 16;
    private static final int FIELD_H = 18;
    private static final int LABEL_H = 11;
    private static final int PAIR_H = 37;
    private static final int TOP_ROW_H = 26;
    private static final int BTN_H = 22;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int GHOST = -9143937;
    private static final int ROW_BG = -15527149;
    private final PaySimulator module;
    private class_342 minAmountField;
    private class_342 maxAmountField;
    private class_342 minDelayField;
    private class_342 maxDelayField;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int colW;
    private int masterY;
    private int wholeY;
    private int payersY;
    private int amountY;
    private int delayY;
    private int buttonsY;

    public PaySimulatorScreen(PaySimulator module) {
        super((class_2561)class_2561.method_43470((String)"Pay Simulator"));
        this.module = module;
    }

    protected void method_25426() {
        this.panelH = 270;
        this.panelX = (this.field_22789 - 380) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 16;
        this.innerW = 348;
        this.colW = (this.innerW - 10) / 2;
        this.masterY = this.panelY + 38 + 12;
        this.wholeY = this.masterY + 26 + 4;
        this.payersY = this.wholeY + 26 + 4;
        this.amountY = this.payersY + 26 + 12;
        this.delayY = this.amountY + 37;
        this.buttonsY = this.delayY + 37 + 10;
        Settings s = Settings.get();
        this.minAmountField = this.field(this.contentX, this.amountY + 11, this.colW, PaySimulator.comma((double)s.paySimMin), 16);
        this.maxAmountField = this.field(this.contentX + this.colW + 10, this.amountY + 11, this.colW, PaySimulator.comma((double)s.paySimMax), 16);
        this.minDelayField = this.field(this.contentX, this.delayY + 11, this.colW, PaySimulatorScreen.trimNum(s.paySimMinDelay), 8);
        this.maxDelayField = this.field(this.contentX + this.colW + 10, this.delayY + 11, this.colW, PaySimulatorScreen.trimNum(s.paySimMaxDelay), 8);
    }

    private class_342 field(int x, int y, int w, String value, int maxLen) {
        class_342 f = new class_342(this.field_22793, x, y, w, 18, (class_2561)class_2561.method_43473());
        f.method_1880(maxLen);
        f.method_1852(value == null ? "" : value);
        this.method_37063(f);
        return f;
    }

    private static String trimNum(double v) {
        return v == Math.floor(v) ? String.valueOf((long)v) : String.valueOf(v);
    }

    private void save() {
        Settings s = Settings.get();
        Double minA = Numbers.parseAmount((String)this.minAmountField.method_1882());
        Double maxA = Numbers.parseAmount((String)this.maxAmountField.method_1882());
        if (minA != null && minA >= 0.0) {
            s.paySimMin = minA;
        }
        if (maxA != null && maxA >= 0.0) {
            s.paySimMax = maxA;
        }
        Double minD = Numbers.parseAmount((String)this.minDelayField.method_1882());
        Double maxD = Numbers.parseAmount((String)this.maxDelayField.method_1882());
        if (minD != null && minD >= 1.0) {
            s.paySimMinDelay = minD;
        }
        if (maxD != null && maxD >= 1.0) {
            s.paySimMaxDelay = maxD;
        }
        s.save();
        this.minAmountField.method_1852(PaySimulator.comma((double)s.paySimMin));
        this.maxAmountField.method_1852(PaySimulator.comma((double)s.paySimMax));
        this.minDelayField.method_1852(PaySimulatorScreen.trimNum(s.paySimMinDelay));
        this.maxDelayField.method_1852(PaySimulatorScreen.trimNum(s.paySimMaxDelay));
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
        Gui.backdrop((class_332)ctx, (int)this.field_22789, (int)this.field_22790);
    }

    protected void method_57734(class_332 ctx) {
        if (Settings.get().menuBlur) {
            super.method_57734(ctx);
        }
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        this.method_25420(ctx, mouseX, mouseY, delta);
        Gui.panel((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)380, (int)this.panelH);
        Gui.roundedGradient((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)378, (int)37, (int)17, (int)0xFFFFFFF, (int)0x3FFFFFF);
        ctx.method_25294(this.panelX + 1, this.panelY + 38, this.panelX + 380 - 1, this.panelY + 38 + 1, 0xFFFFFFF);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Pay Simulator"), (int)(this.panelX + 190), (int)(this.panelY + 15), (int)-1);
        this.toggleRow(ctx, mouseX, mouseY, this.masterY, "Enabled", this.module.isEnabled(), "master");
        this.toggleRow(ctx, mouseX, mouseY, this.wholeY, "Whole numbers only", Settings.get().paySimWholeNumbers, "whole");
        int pool = PaySimulator.poolSize();
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)this.payersY, (int)this.innerW, (int)26, (int)10, (int)0xFFFFFFF);
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(this.payersY + 1), (int)(this.innerW - 2), (int)24, (int)9, (int)-15527149, (int)-15987700);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Payers"), (int)(this.contentX + 12), (int)(this.payersY + 8), (int)-3419946);
        class_2561 status = Gui.font((String)(pool + (PaySimulator.poolIsReal() ? " players online" : " random names")));
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)status, (int)(this.contentX + this.innerW - 12 - this.field_22793.method_27525((class_5348)status)), (int)(this.payersY + 8), (int)(pool == 0 ? -9143937 : -1));
        this.label(ctx, this.contentX, this.amountY, "Minimum amount");
        this.label(ctx, this.contentX + this.colW + 10, this.amountY, "Maximum amount");
        this.label(ctx, this.contentX, this.delayY, "Minimum delay (s)");
        this.label(ctx, this.contentX + this.colW + 10, this.delayY, "Maximum delay (s)");
        super.method_25394(ctx, mouseX, mouseY, delta);
        int third = (this.innerW - 12) / 3;
        this.button(ctx, mouseX, mouseY, this.contentX, third, "Test", "test");
        this.button(ctx, mouseX, mouseY, this.contentX + third + 6, third, "Save", "save");
        this.button(ctx, mouseX, mouseY, this.contentX + (third + 6) * 2, this.innerW - (third + 6) * 2, "Close", "close");
    }

    private void label(class_332 ctx, int x, int y, String s) {
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)s), (int)(x + 1), (int)y, (int)-3419946);
    }

    private void toggleRow(class_332 ctx, int mouseX, int mouseY, int y, String text, boolean value, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)this.contentX, (int)y, (int)this.innerW, (int)26);
        float h = Anim.bool((String)("pstop_" + id), (boolean)hover, (float)15.0f);
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)y, (int)this.innerW, (int)26, (int)14, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h));
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(y + 1), (int)(this.innerW - 2), (int)24, (int)9, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)h), (int)-15987700);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)text), (int)(this.contentX + 12), (int)(y + 8), (int)Gui.lerp((int)-3419946, (int)-1, (float)h));
        Gui.toggleSwitch((class_332)ctx, (int)(this.contentX + this.innerW - 12 - 36), (int)(y + 3), (int)36, (int)19, (float)Anim.bool((String)("pssw_" + id), (boolean)value, (float)16.0f), (float)h);
    }

    private void button(class_332 ctx, int mouseX, int mouseY, int x, int w, String text, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)this.buttonsY, (int)w, (int)22);
        float a = Anim.bool((String)("psbtn_" + id), (boolean)hover, (float)16.0f);
        Gui.roundedRect((class_332)ctx, (int)x, (int)this.buttonsY, (int)w, (int)22, (int)11, (int)Gui.lerp((int)0x1AFFFFFF, (int)0x52FFFFFF, (float)a));
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(this.buttonsY + 1), (int)(w - 2), (int)20, (int)10, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)a));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)text), (int)(x + w / 2), (int)(this.buttonsY + 6), (int)Gui.lerp((int)-3419946, (int)-1, (float)a));
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.masterY, (int)this.innerW, (int)26)) {
            this.module.toggle();
            FakeClient.MODULES.saveState();
            Sfx.click();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.wholeY, (int)this.innerW, (int)26)) {
            Settings s = Settings.get();
            s.paySimWholeNumbers = !s.paySimWholeNumbers;
            s.save();
            Sfx.click();
            return true;
        }
        int third = (this.innerW - 12) / 3;
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.buttonsY, (int)third, (int)22)) {
            this.save();
            this.module.fireNow();
            Sfx.click();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)(this.contentX + third + 6), (int)this.buttonsY, (int)third, (int)22)) {
            this.save();
            Sfx.click();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)(this.contentX + (third + 6) * 2), (int)this.buttonsY, (int)(this.innerW - (third + 6) * 2), (int)22)) {
            this.save();
            this.method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25421() {
        return false;
    }
}
