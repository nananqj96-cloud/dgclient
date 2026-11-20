/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.gui.LicenseScreen$1
 *  com.fakeclient.license.Hwid
 *  com.fakeclient.license.License
 *  com.fakeclient.license.License$Status
 *  net.minecraft.class_11908
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 */
package com.fakeclient.gui;
import com.fakeclient.license.License$Status;

import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import com.fakeclient.gui.Gui;
import com.fakeclient.gui.LicenseScreen;
import com.fakeclient.license.Hwid;
import com.fakeclient.license.License;
import java.util.Locale;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public class LicenseScreen
extends class_437 {
    private static final int PANEL_W = 340;
    private static final int HEADER_H = 46;
    private static final int MARGIN = 18;
    private static final int FIELD_H = 20;
    private static final int ROW_H = 26;
    private static final int BTN_H = 24;
    private static final int KEY_MAX = 64;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int FAINT = -6578010;
    private static final int GHOST = -9143937;
    private static final int ROW_BG = -15527149;
    private static final int SUNKEN = -16250872;
    private static final int OK = -1;
    private static final int WARN = -6645094;
    private static final int BAD = -9539986;
    private final class_437 parent;
    private class_342 keyField;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int keyY;
    private int statusY;
    private int expiryY;
    private int buttonsY;
    private int hwidY;

    public LicenseScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"License"));
        this.parent = parent;
    }

    protected void method_25426() {
    }

    private void activate() {
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
        License lic = License.get();
        int accent = Gui.accent() & 0xFFFFFF;
        Gui.panel((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)340, (int)this.panelH);
        Gui.roundedGradient((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)338, (int)45, (int)17, (int)0xFFFFFFF, (int)0x3FFFFFF);
        ctx.method_25294(this.panelX + 1, this.panelY + 46, this.panelX + 340 - 1, this.panelY + 46 + 1, 0xFFFFFFF);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Fake Client".toUpperCase(Locale.ROOT)), (int)(this.panelX + 170), (int)(this.panelY + 13), (int)-1);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)(lic.isValid() ? "License active" : "Enter your license key")), (int)(this.panelX + 170), (int)(this.panelY + 27), (int)-9143937);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"License key"), (int)(this.contentX + 2), (int)(this.keyY - 11), (int)-3419946);
        boolean focused = this.keyField.method_25370();
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)this.keyY, (int)this.innerW, (int)20, (int)7, (int)(focused ? 0x55FFFFFF : 0x1AFFFFFF));
        Gui.roundedRect((class_332)ctx, (int)(this.contentX + 1), (int)(this.keyY + 1), (int)(this.innerW - 2), (int)18, (int)6, (int)-16250872);
        super.method_25394(ctx, mouseX, mouseY, delta);
        this.renderStatus(ctx, lic);
        this.renderExpiry(ctx, lic);
        int half = (this.innerW - 8) / 2;
        this.button(ctx, mouseX, mouseY, this.contentX, half, lic.status() == License$Status.CHECKING ? "Checking" : "Activate", "activate", true);
        this.button(ctx, mouseX, mouseY, this.contentX + half + 8, half, lic.isValid() ? "Continue" : "Close", "close", false);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)("HWID  " + Hwid.shortForm())), (int)(this.panelX + 170), (int)this.hwidY, (int)-9143937);
    }

    private void renderStatus(class_332 ctx, License lic) {
        this.plate(ctx, this.statusY);
        int dot = switch (LicenseScreen$1.$SwitchMap$com$fakeclient$license$License$Status[lic.status().ordinal()]) {
            case 1 -> -1;
            case 2, 3 -> -6645094;
            case 4 -> -6645094;
            default -> -9539986;
        };
        float a = lic.status() == License$Status.CHECKING ? 0.45f + 0.55f * (float)Math.abs(Math.sin((double)System.currentTimeMillis() / 260.0)) : 1.0f;
        Gui.roundedRect((class_332)ctx, (int)(this.contentX + 11), (int)(this.statusY + 9), (int)7, (int)7, (int)4, (int)Gui.alpha((int)dot, (float)a));
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Status"), (int)(this.contentX + 26), (int)(this.statusY + 8), (int)-3419946);
        class_2561 msg = Gui.font((String)lic.message());
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)msg, (int)(this.contentX + this.innerW - 12 - this.field_22793.method_27525((class_5348)msg)), (int)(this.statusY + 8), (int)(lic.status() == License$Status.VALID ? -1 : dot));
    }

    private void renderExpiry(class_332 ctx, License lic) {
        int color;
        Object right;
        this.plate(ctx, this.expiryY);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Valid until"), (int)(this.contentX + 12), (int)(this.expiryY + 8), (int)-3419946);
        if (!lic.isValid()) {
            right = "-";
            color = -9143937;
        } else if (!lic.hasExpiry()) {
            right = "Lifetime";
            color = -1;
        } else {
            long days = lic.daysLeft();
            right = lic.expiryText() + "   " + lic.remainingText();
            color = days <= 3L ? -6645094 : -1;
        }
        class_2561 t = Gui.font((String)right);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)t, (int)(this.contentX + this.innerW - 12 - this.field_22793.method_27525((class_5348)t)), (int)(this.expiryY + 8), (int)color);
    }

    private void plate(class_332 ctx, int y) {
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)y, (int)this.innerW, (int)26, (int)9, (int)0xFFFFFFF);
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(y + 1), (int)(this.innerW - 2), (int)24, (int)8, (int)-15527149, (int)-15987700);
    }

    private void button(class_332 ctx, int mouseX, int mouseY, int x, int w, String label, String id, boolean primary) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)this.buttonsY, (int)w, (int)24);
        float a = Anim.bool((String)("lic_" + id), (boolean)hover, (float)16.0f);
        int accent = Gui.accent() & 0xFFFFFF;
        Gui.roundedRect((class_332)ctx, (int)x, (int)this.buttonsY, (int)w, (int)24, (int)12, (int)(primary ? Gui.lerp((int)0x66FFFFFF, (int)(0xFF000000 | accent), (float)a) : Gui.lerp((int)0x1AFFFFFF, (int)0x52FFFFFF, (float)a)));
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(this.buttonsY + 1), (int)(w - 2), (int)22, (int)11, (int)Gui.lerp((int)(primary ? -15527149 : -15527149), (int)-15066598, (float)a));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)label), (int)(x + w / 2), (int)(this.buttonsY + 7), (int)Gui.lerp((int)(primary ? -1 : -3419946), (int)-1, (float)a));
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }
        int half = (this.innerW - 8) / 2;
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.buttonsY, (int)half, (int)24)) {
            this.activate();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)(this.contentX + half + 8), (int)this.buttonsY, (int)half, (int)24)) {
            this.method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25404(class_11908 input) {
        if (input.comp_4795() == 257 || input.comp_4795() == 335) {
            this.activate();
            return true;
        }
        return super.method_25404(input);
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    public boolean method_25421() {
        return false;
    }
}
