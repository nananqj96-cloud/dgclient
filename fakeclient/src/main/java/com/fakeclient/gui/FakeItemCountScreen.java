/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.module.modules.FakeItemCount
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_437
 */
package com.fakeclient.gui;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.Gui;
import com.fakeclient.module.modules.FakeItemCount;
import com.fakeclient.util.Sfx;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_437;

public class FakeItemCountScreen
extends class_437 {
    private static final int PANEL_W = 236;
    private static final int HEADER_H = 34;
    private static final int MARGIN = 16;
    private static final int FIELD_H = 18;
    private static final int BTN_H = 20;
    private static final int ROW_H = 37;
    private static final int TOGGLE_BG = 0x242424;
    private static final int ON_BG = 0x6E6E6E;
    private static final int OFF_BG = 0x444444;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -6578010;
    private final FakeItemCount module;
    private class_342 countField;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int toggleY;
    private int rowY;
    private int buttonsY;

    public FakeItemCountScreen(FakeItemCount module) {
        super((class_2561)class_2561.method_43470((String)"Fake Item Count"));
        this.module = module;
    }

    protected void method_25426() {
        this.panelH = 159;
        this.panelX = (this.field_22789 - 236) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 16;
        this.innerW = 204;
        this.toggleY = this.panelY + 34 + 12;
        this.rowY = this.toggleY + 20 + 14;
        this.buttonsY = this.rowY + 37 + 6;
        this.countField = new class_342(this.field_22793, this.contentX, this.rowY + 11, this.innerW, 18, (class_2561)class_2561.method_43473());
        this.countField.method_1880(7);
        this.countField.method_1852(String.valueOf(Settings.get().itemMultiplier));
        this.method_37063(this.countField);
    }

    private void save() {
        Settings s = Settings.get();
        try {
            int count = Integer.parseInt(this.countField.method_1882().trim().replaceAll("[^0-9]", ""));
            if (count >= 1) {
                s.itemMultiplier = count;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        s.save();
        this.countField.method_1852(String.valueOf(s.itemMultiplier));
    }

    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
        Gui.backdrop((class_332)ctx, (int)this.field_22789, (int)this.field_22790);
    }

    protected void method_57734(class_332 ctx) {
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        this.method_25420(ctx, mouseX, mouseY, delta);
        Gui.panel((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)236, (int)this.panelH);
        Gui.header((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)236, (int)34);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Fake Item Count"), (int)(this.panelX + 118), (int)(this.panelY + 12), (int)-1);
        boolean on = this.module.isEnabled();
        boolean toggleHover = Gui.inside((double)mouseX, (double)mouseY, (int)this.contentX, (int)this.toggleY, (int)this.innerW, (int)20);
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)this.toggleY, (int)this.innerW, (int)20, (int)10, (int)0x1AFFFFFF);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)(on ? "Enabled" : "Disabled")), (int)(this.contentX + 10), (int)(this.toggleY + 5), (int)-1);
        int pillW = 44;
        int pillX = this.contentX + this.innerW - 8 - pillW;
        int pillY = this.toggleY + 3;
        Gui.glass((class_332)ctx, (int)pillX, (int)pillY, (int)pillW, (int)14, (int)(on ? 0x6E6E6E : 0x444444), (boolean)toggleHover);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)(on ? "ON" : "OFF")), (int)(pillX + pillW / 2), (int)(pillY + 3), (int)-1);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Count to show on every stack  (e.g. 64)"), (int)this.contentX, (int)this.rowY, (int)-6578010);
        super.method_25394(ctx, mouseX, mouseY, delta);
        int half = (this.innerW - 8) / 2;
        boolean saveHover = Gui.inside((double)mouseX, (double)mouseY, (int)this.contentX, (int)this.buttonsY, (int)half, (int)20);
        boolean closeHover = Gui.inside((double)mouseX, (double)mouseY, (int)(this.contentX + half + 8), (int)this.buttonsY, (int)half, (int)20);
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)this.buttonsY, (int)half, (int)20, (int)10, (int)0x1AFFFFFF);
        Gui.roundedRect((class_332)ctx, (int)(this.contentX + half + 8), (int)this.buttonsY, (int)half, (int)20, (int)10, (int)0x1AFFFFFF);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Save"), (int)(this.contentX + half / 2), (int)(this.buttonsY + 5), (int)-1);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Close"), (int)(this.contentX + half + 8 + half / 2), (int)(this.buttonsY + 5), (int)-1);
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        if (click.method_74245() == 0) {
            int half = (this.innerW - 8) / 2;
            if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.toggleY, (int)this.innerW, (int)20)) {
                this.module.toggle();
                FakeClient.MODULES.saveState();
                Sfx.click();
                return true;
            }
            if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.buttonsY, (int)half, (int)20)) {
                this.save();
                return true;
            }
            if (Gui.inside((double)mx, (double)my, (int)(this.contentX + half + 8), (int)this.buttonsY, (int)half, (int)20)) {
                this.save();
                this.method_25419();
                return true;
            }
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25421() {
        return false;
    }
}
