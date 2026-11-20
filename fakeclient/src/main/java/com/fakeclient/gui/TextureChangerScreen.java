/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Anim
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.gui.ItemPickerScreen
 *  com.fakeclient.module.modules.TextureChanger
 *  com.fakeclient.module.modules.TextureChanger$Swap
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_11909
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 */
package com.fakeclient.gui;
import com.fakeclient.module.modules.TextureChanger$Swap;

import com.fakeclient.FakeClient;
import com.fakeclient.Settings;
import com.fakeclient.gui.Anim;
import com.fakeclient.gui.Gui;
import com.fakeclient.gui.ItemPickerScreen;
import com.fakeclient.module.modules.TextureChanger;
import com.fakeclient.util.Sfx;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public class TextureChangerScreen
extends class_437 {
    private static final int PANEL_W = 460;
    private static final int HEADER_H = 38;
    private static final int MARGIN = 16;
    private static final int TOP_ROW_H = 26;
    private static final int ROW_H = 26;
    private static final int ROW_GAP = 3;
    private static final int BTN_H = 22;
    private static final int PILL_W = 40;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int FAINT = -6578010;
    private static final int GHOST = -9143937;
    private static final int ROW_BG = -15527149;
    private static final int OFF_BG = -15066598;
    private final TextureChanger module;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int masterY;
    private int renameY;
    private int soundY;
    private int listY;
    private int listH;
    private int buttonsY;
    private float scroll = 0.0f;
    private int maxScroll;

    public TextureChangerScreen(TextureChanger module) {
        super((class_2561)class_2561.method_43470((String)"Texture Changer"));
        this.module = module;
    }

    protected void method_25426() {
        int rows = this.module.getSwaps().size();
        int fullList = rows * 26 + Math.max(0, rows - 1) * 3;
        int chrome = 214;
        int avail = Math.max(60, this.field_22790 - 40 - chrome);
        this.listH = Math.min(fullList, avail);
        this.maxScroll = Math.max(0, fullList - this.listH);
        this.scroll = TextureChangerScreen.clamp(this.scroll, 0.0f, this.maxScroll);
        this.panelH = chrome + this.listH;
        this.panelX = (this.field_22789 - 460) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 16;
        this.innerW = 428;
        this.masterY = this.panelY + 38 + 12;
        this.renameY = this.masterY + 26 + 4;
        this.soundY = this.renameY + 26 + 4;
        this.listY = this.soundY + 26 + 4 + 10 + 12;
        this.buttonsY = this.listY + this.listH + 14;
    }

    private int[] rowRect(int i) {
        return new int[]{this.contentX, this.listY - Math.round(this.scroll) + i * 29, this.innerW, 26};
    }

    private int[] pillRect(int rx, int ry) {
        return new int[]{rx + this.innerW - 8 - 40, ry + 5, 40, 16};
    }

    private int[] thirdRect(int index) {
        int gap = 6;
        int w = (this.innerW - gap * 2) / 3;
        return new int[]{this.contentX + index * (w + gap), this.buttonsY, w, 22};
    }

    private int[] addBtnRect() {
        return this.thirdRect(0);
    }

    private int[] resetBtnRect() {
        return this.thirdRect(1);
    }

    private int[] closeBtnRect() {
        return this.thirdRect(2);
    }

    private int[] removeRect(int rx, int ry) {
        return new int[]{rx + this.innerW - 8 - 40 - 22, ry + 5, 16, 16};
    }

    private boolean inList(double mx, double my) {
        return my >= (double)this.listY && my < (double)(this.listY + this.listH) && mx >= (double)this.contentX && mx < (double)(this.contentX + this.innerW);
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
        Gui.panel((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)460, (int)this.panelH);
        Gui.roundedGradient((class_332)ctx, (int)(this.panelX + 1), (int)(this.panelY + 1), (int)458, (int)37, (int)17, (int)0xFFFFFFF, (int)0x3FFFFFF);
        ctx.method_25294(this.panelX + 1, this.panelY + 38, this.panelX + 460 - 1, this.panelY + 38 + 1, 0xFFFFFFF);
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"Texture Changer"), (int)(this.panelX + 230), (int)(this.panelY + 15), (int)-1);
        this.toggleRow(ctx, mouseX, mouseY, this.masterY, "Module enabled", this.module.isEnabled(), "master");
        this.toggleRow(ctx, mouseX, mouseY, this.renameY, "Rename swapped items", this.module.renameItems, "rename");
        this.toggleRow(ctx, mouseX, mouseY, this.soundY, "Use replacement sounds", this.module.swapSounds, "sounds");
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)"REPLACEMENTS"), (int)this.contentX, (int)(this.listY - 15), (int)-9143937);
        class_2561 count = Gui.font((String)(this.module.activeCount() + " / " + this.module.getSwaps().size() + " on"));
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)count, (int)(this.contentX + this.innerW - this.field_22793.method_27525((class_5348)count)), (int)(this.listY - 15), (int)-9143937);
        ctx.method_44379(this.contentX, this.listY, this.contentX + this.innerW, this.listY + this.listH);
        this.renderRows(ctx, mouseX, mouseY);
        ctx.method_44380();
        if (this.maxScroll > 0) {
            int trackX = this.contentX + this.innerW - 2;
            int thumbH = Math.max(20, Math.round((float)this.listH * (float)this.listH / (float)(this.listH + this.maxScroll)));
            int thumbY = this.listY + Math.round(this.scroll / (float)this.maxScroll * (float)(this.listH - thumbH));
            Gui.roundedRect((class_332)ctx, (int)trackX, (int)this.listY, (int)2, (int)this.listH, (int)1, (int)0xFFFFFFF);
            Gui.roundedRect((class_332)ctx, (int)trackX, (int)thumbY, (int)2, (int)thumbH, (int)1, (int)0x66FFFFFF);
        }
        this.button(ctx, mouseX, mouseY, this.addBtnRect(), "+  Add swap", "add");
        this.button(ctx, mouseX, mouseY, this.resetBtnRect(), "Enable all", "reset");
        this.button(ctx, mouseX, mouseY, this.closeBtnRect(), "Close", "close");
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private void renderRows(class_332 ctx, int mouseX, int mouseY) {
        List swaps = this.module.getSwaps();
        boolean master = this.module.isEnabled();
        int fromIconX = this.contentX + 8;
        int fromTextX = fromIconX + 21;
        int arrowX = this.contentX + 196;
        int toIconX = this.contentX + 216;
        int toTextX = toIconX + 21;
        int pillLeft = this.contentX + this.innerW - 8 - 40;
        int fromMaxW = arrowX - 8 - fromTextX;
        int toMaxW = pillLeft - 10 - toTextX;
        for (int i = 0; i < swaps.size(); ++i) {
            TextureChanger$Swap s = (TextureChanger$Swap)swaps.get(i);
            int[] r = this.rowRect(i);
            int rx = r[0];
            int ry = r[1];
            if (ry + 26 < this.listY || ry > this.listY + this.listH) continue;
            boolean hover = this.inList(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)rx, (int)ry, (int)this.innerW, (int)26);
            float h = Anim.bool((String)("tcrow_" + s.id), (boolean)hover, (float)15.0f);
            boolean live = master && s.enabled;
            Gui.roundedRect((class_332)ctx, (int)rx, (int)ry, (int)this.innerW, (int)26, (int)9, (int)Gui.lerp((int)0xFFFFFFF, (int)0x2AFFFFFF, (float)h));
            Gui.roundedGradient((class_332)ctx, (int)(rx + 1), (int)(ry + 1), (int)(this.innerW - 2), (int)24, (int)8, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)h), (int)-15987700);
            int iconY = ry + 5;
            ctx.method_51427(s.fromStack, fromIconX, iconY);
            ctx.method_51427(s.toStack, toIconX, iconY);
            int textY = ry + 8;
            int nameCol = live ? -1 : -3419946;
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)this.trim(s.fromName().getString(), fromMaxW)), (int)fromTextX, (int)textY, (int)(live ? -3419946 : -6578010));
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"\u2192"), (int)arrowX, (int)textY, (int)(live ? -3419946 : -9143937));
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)this.trim(s.toName().getString(), toMaxW)), (int)toTextX, (int)textY, (int)nameCol);
            if (TextureChanger.isCustom((TextureChanger$Swap)s)) {
                int[] rm = this.removeRect(rx, ry);
                boolean rh = this.inList(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)rm[0], (int)rm[1], (int)rm[2], (int)rm[3]);
                float ra = Anim.bool((String)("tcrm_" + s.id), (boolean)rh, (float)16.0f);
                Gui.roundedRect((class_332)ctx, (int)rm[0], (int)rm[1], (int)rm[2], (int)rm[3], (int)8, (int)Gui.lerp((int)0x14FFFFFF, (int)0x44FFFFFF, (float)ra));
                Gui.roundedRect((class_332)ctx, (int)(rm[0] + 1), (int)(rm[1] + 1), (int)(rm[2] - 2), (int)(rm[3] - 2), (int)7, (int)-15066598);
                int cx = rm[0] + rm[2] / 2;
                int cy = rm[1] + rm[3] / 2;
                int col = Gui.lerp((int)-6578010, (int)-1, (float)ra);
                for (int d = -3; d <= 3; ++d) {
                    ctx.method_25294(cx + d, cy + d, cx + d + 1, cy + d + 1, col);
                    ctx.method_25294(cx + d, cy - d, cx + d + 1, cy - d + 1, col);
                }
            }
            int[] p = this.pillRect(rx, ry);
            boolean ph = this.inList(mouseX, mouseY) && Gui.inside((double)mouseX, (double)mouseY, (int)p[0], (int)p[1], (int)p[2], (int)p[3]);
            float pa = Anim.bool((String)("tcpill_" + s.id), (boolean)ph, (float)16.0f);
            float on = Anim.bool((String)("tcon_" + s.id), (boolean)s.enabled, (float)16.0f);
            int accent = Gui.accent() & 0xFFFFFF;
            Gui.roundedRect((class_332)ctx, (int)p[0], (int)p[1], (int)p[2], (int)p[3], (int)8, (int)Gui.lerp((int)Gui.lerp((int)0x1AFFFFFF, (int)0x4DFFFFFF, (float)pa), (int)(0xFF000000 | accent), (float)(on * 0.8f)));
            Gui.roundedRect((class_332)ctx, (int)(p[0] + 1), (int)(p[1] + 1), (int)(p[2] - 2), (int)(p[3] - 2), (int)7, (int)Gui.lerp((int)-15066598, (int)-15066598, (float)on));
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)(s.enabled ? "ON" : "OFF")), (int)(p[0] + p[2] / 2), (int)(p[1] + 4), (int)(s.enabled ? -1 : -6578010));
        }
    }

    private void toggleRow(class_332 ctx, int mouseX, int mouseY, int y, String label, boolean value, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)this.contentX, (int)y, (int)this.innerW, (int)26);
        float h = Anim.bool((String)("tctop_" + id), (boolean)hover, (float)15.0f);
        Gui.roundedRect((class_332)ctx, (int)this.contentX, (int)y, (int)this.innerW, (int)26, (int)14, (int)Gui.lerp((int)0xFFFFFFF, (int)0x1AFFFFFF, (float)h));
        Gui.roundedGradient((class_332)ctx, (int)(this.contentX + 1), (int)(y + 1), (int)(this.innerW - 2), (int)24, (int)9, (int)Gui.lerp((int)-15527149, (int)-15066598, (float)h), (int)-15987700);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)label), (int)(this.contentX + 12), (int)(y + 8), (int)Gui.lerp((int)-3419946, (int)-1, (float)h));
        Gui.toggleSwitch((class_332)ctx, (int)(this.contentX + this.innerW - 12 - 36), (int)(y + 3), (int)36, (int)19, (float)Anim.bool((String)("tcsw_" + id), (boolean)value, (float)16.0f), (float)h);
    }

    private void button(class_332 ctx, int mouseX, int mouseY, int[] r, String label, String id) {
        boolean hover = Gui.inside((double)mouseX, (double)mouseY, (int)r[0], (int)r[1], (int)r[2], (int)r[3]);
        float a = Anim.bool((String)("tcbtn_" + id), (boolean)hover, (float)16.0f);
        Gui.roundedRect((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)(r[3] / 2), (int)Gui.lerp((int)0x1AFFFFFF, (int)0x52FFFFFF, (float)a));
        Gui.roundedRect((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)(r[3] - 2), (int)(r[3] / 2 - 1), (int)Gui.lerp((int)-15527149, (int)-15066598, (float)a));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)label), (int)(r[0] + r[2] / 2), (int)(r[1] + (r[3] - 9) / 2), (int)Gui.lerp((int)-3419946, (int)-1, (float)a));
    }

    private void openPicker() {
        this.field_22787.method_1507((class_437)new ItemPickerScreen((class_437)this, "Replace which block or item?", from -> this.field_22787.method_1507((class_437)new ItemPickerScreen((class_437)this, "Make it look like...", to -> {
            if (this.module.addCustom(from, to)) {
                this.method_25426();
            }
        }))));
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        int[] add;
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
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.renameY, (int)this.innerW, (int)26)) {
            this.module.setRenameItems(!this.module.renameItems);
            Sfx.click();
            return true;
        }
        if (Gui.inside((double)mx, (double)my, (int)this.contentX, (int)this.soundY, (int)this.innerW, (int)26)) {
            this.module.setSwapSounds(!this.module.swapSounds);
            Sfx.click();
            return true;
        }
        if (this.inList(mx, my)) {
            List swaps = this.module.getSwaps();
            for (int i = 0; i < swaps.size(); ++i) {
                int[] rm;
                int[] r = this.rowRect(i);
                if (!Gui.inside((double)mx, (double)my, (int)r[0], (int)r[1], (int)this.innerW, (int)26)) continue;
                TextureChanger$Swap s = (TextureChanger$Swap)swaps.get(i);
                if (TextureChanger.isCustom((TextureChanger$Swap)s) && Gui.inside((double)mx, (double)my, (int)(rm = this.removeRect(r[0], r[1]))[0], (int)rm[1], (int)rm[2], (int)rm[3])) {
                    this.module.removeCustom(s);
                    Sfx.click();
                    this.method_25426();
                    return true;
                }
                this.module.setSwapEnabled(s, !s.enabled);
                Sfx.click();
                return true;
            }
        }
        if (Gui.inside((double)mx, (double)my, (int)(add = this.addBtnRect())[0], (int)add[1], (int)add[2], (int)add[3])) {
            Sfx.click();
            this.openPicker();
            return true;
        }
        int[] reset = this.resetBtnRect();
        if (Gui.inside((double)mx, (double)my, (int)reset[0], (int)reset[1], (int)reset[2], (int)reset[3])) {
            this.module.enableAll();
            Sfx.click();
            return true;
        }
        int[] close = this.closeBtnRect();
        if (Gui.inside((double)mx, (double)my, (int)close[0], (int)close[1], (int)close[2], (int)close[3])) {
            this.method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.maxScroll > 0 && this.inList(mouseX, mouseY)) {
            this.scroll = TextureChangerScreen.clamp(this.scroll - (float)verticalAmount * 20.0f, 0.0f, this.maxScroll);
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean method_25421() {
        return false;
    }

    private String trim(String s, int maxW) {
        int end;
        if (s == null || s.isEmpty() || maxW <= 0) {
            return "";
        }
        if (this.field_22793.method_27525((class_5348)Gui.font((String)s)) <= maxW) {
            return s;
        }
        int ellipsis = this.field_22793.method_27525((class_5348)Gui.font((String)"..."));
        if (ellipsis >= maxW) {
            return "...";
        }
        for (end = s.length(); end > 0 && this.field_22793.method_27525((class_5348)Gui.font((String)s.substring(0, end))) + ellipsis > maxW; --end) {
        }
        return s.substring(0, end).stripTrailing() + "...";
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
