/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.module.modules.FakeMedia
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
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
import com.fakeclient.module.modules.FakeMedia;
import com.fakeclient.util.Sfx;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public class FakeMediaScreen
extends class_437 {
    private static final int PANEL_W = 380;
    private static final int HEADER_H = 38;
    private static final int MARGIN = 16;
    private static final int FIELD_H = 18;
    private static final int LABEL_H = 11;
    private static final int TOP_ROW_H = 26;
    private static final int BTN_H = 22;
    private static final int NAME_MAX = 16;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int FAINT = -6578010;
    private static final int GHOST = -9143937;
    private static final int ROW_BG = -15527149;
    private final FakeMedia module;
    private class_342 nameField;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int masterY;
    private int iconY;
    private int nameY;
    private int previewY;
    private int buttonsY;

    public FakeMediaScreen(FakeMedia module) {
        super((class_2561)class_2561.method_43470((String)"Fake Media"));
        this.module = module;
    }

    protected void method_25426() {
        this.panelH = 235;
        this.panelX = (this.field_22789 - 380) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 16;
        this.innerW = 348;
        this.masterY = this.panelY + 38 + 12;
        this.iconY = this.masterY + 26 + 4;
        this.nameY = this.iconY + 26 + 12;
        this.previewY = this.nameY + 11 + 18 + 12;
        this.buttonsY = this.previewY + 26 + 12;
        this.nameField = new class_342(this.field_22793, this.contentX, this.nameY + 11, this.innerW, 18, (class_2561)class_2561.method_43473());
        this.nameField.method_1880(16);
        this.nameField.method_1852(FakeMedia.customName());
        this.method_37063(this.nameField);
    }

    private void save() {
        FakeMedia.setCustomName((String)this.nameField.method_1882());
        this.nameField.method_1852(FakeMedia.customName());
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
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Fake Media"), (int)(this.panelX + 190), (int)(this.panelY + 15), (int)-1);
        this.toggleRow(ctx, mouseX, mouseY, this.masterY, "Enabled", this.module.isEnabled(), "master");
        this.plate(ctx, this.iconY, 26);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Icon"), (int)(this.contentX + 12), (int)(this.iconY + 8), (int)-3419946);
        class_2561 cam = FakeMedia.iconText();
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)cam, (int)(this.contentX + this.innerW - 12 - this.field_22793.method_27525((class_5348)cam)), (int)(this.iconY + 8), (int)-1);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Name shown beside the icon"), (int)(this.contentX + 1), (int)this.nameY, (int)-3419946);
        super.method_25394(ctx, mouseX, mouseY, delta);
        this.plate(ctx, this.previewY, 26);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Preview"), (int)(this.contentX + 12), (int)(this.previewY + 8), (int)-9143937);
        class_2561 preview = this.previewText();
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)preview, (int)(this.contentX + this.innerW - 12 - this.field_22793.method_27525((class_5348)preview)), (int)(this.previewY + 8), (int)-1);
        int half = (this.innerW - 8) / 2;
        this.button(ctx, mouseX, mouseY, this.contentX, half, "Save", "save");
        this.button(ctx, mouseX, mouseY, this.contentX + half + 8, half, "Close", "close");
    }

    private class_2561 previewText() {
        String typed = this.nameField.method_1882().trim();
        class_310 mc = class_310.method_1551();
        String fallback = mc.field_1724 == null ? "you" : mc.field_1724.method_7334().name();
        return FakeMedia.decorate((class_2561)class_2561.method_43470((String)(typed.isEmpty() ? fallback : typed)));
    }

    private void plate(class_332 ctx, int y, int h) {
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)y, (int)this.innerW, (int)h, (int)10, (int)0xFFFFFFF);
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(y + 1), (int)(this.innerW - 2), (int)(h - 2), (int)9, (int)-15527149, (int)-15987700);
    }

    private void toggleRow(class_332 ctx, int mouseX, int mouseY, int y, String text, boolean value, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)this.contentX, (int)y, (int)this.innerW, (int)26);
        float h = Anim.bool((String)("fmtop_" + id), (boolean)hover, (float)15.0f);
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)y, (int)this.innerW, (int)26, (int)14, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h));
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(y + 1), (int)(this.innerW - 2), (int)24, (int)9, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)h), (int)-15987700);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)text), (int)(this.contentX + 12), (int)(y + 8), (int)Gui.lerp((int)-3419946, (int)-1, (float)h));
        Gui.toggleSwitch((class_332)ctx, (int)(this.contentX + this.innerW - 12 - 36), (int)(y + 3), (int)36, (int)19, (float)Anim.bool((String)("fmsw_" + id), (boolean)value, (float)16.0f), (float)h);
    }

    private void button(class_332 ctx, int mouseX, int mouseY, int x, int w, String text, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)x, (int)this.buttonsY, (int)w, (int)22);
        float a = Anim.bool((String)("fmbtn_" + id), (boolean)hover, (float)16.0f);
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
        int half = (this.innerW - 8) / 2;
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.buttonsY, (int)half, (int)22)) {
            this.save();
            Sfx.click();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)(this.contentX + half + 8), (int)this.buttonsY, (int)half, (int)22)) {
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
