/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Gui
 *  com.fakeclient.gui.Icons
 *  com.fakeclient.util.ItemCatalog
 *  com.fakeclient.util.ItemCatalog$Entry
 *  com.fakeclient.util.Sfx
 *  net.minecraft.class_11905
 *  net.minecraft.class_11908
 *  net.minecraft.class_11909
 *  net.minecraft.class_1792
 *  net.minecraft.class_2561
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 *  net.minecraft.class_5348
 */
package com.fakeclient.gui;

import com.fakeclient.Settings;
import com.fakeclient.gui.Gui;
import com.fakeclient.gui.Icons;
import com.fakeclient.util.ItemCatalog;
import com.fakeclient.util.ItemCatalog$Entry;
import com.fakeclient.util.Sfx;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_1792;
import net.minecraft.class_2561;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5348;

public class ItemPickerScreen
extends class_437 {
    private static final int PANEL_W = 420;
    private static final int HEADER_H = 62;
    private static final int FOOT_H = 26;
    private static final int MARGIN = 16;
    private static final int CELL = 26;
    private static final int COLS = 13;
    private static final int TEXT = -1;
    private static final int SUBTEXT = -3419946;
    private static final int MUTED = -6578010;
    private static final int GHOST = -9143937;
    private static final int SURFACE_1 = -15987700;
    private static final int SURFACE_2 = -15527149;
    private static final int SURFACE_3 = -15066598;
    private static final int BORDER = 0xFFFFFFF;
    private static final int BORDER_LIT = 0x1AFFFFFF;
    private final class_437 parent;
    private final String prompt;
    private final Consumer<class_1792> onPick;
    private String query = "";
    private List<ItemCatalog$Entry> results;
    private float scroll;
    private int maxScroll;
    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int innerW;
    private int gridY;
    private int gridH;

    public ItemPickerScreen(class_437 parent, String prompt, Consumer<class_1792> onPick) {
        super((class_2561)class_2561.method_43470((String)prompt));
        this.parent = parent;
        this.prompt = prompt;
        this.onPick = onPick;
    }

    protected void method_25426() {
        this.results = ItemCatalog.search((String)this.query);
        this.panelH = Math.min(this.field_22790 - 40, 360);
        this.panelX = (this.field_22789 - 420) / 2;
        this.panelY = (this.field_22790 - this.panelH) / 2;
        this.contentX = this.panelX + 16;
        this.innerW = 388;
        this.gridY = this.panelY + 62;
        this.gridH = this.panelH - 62 - 26;
        this.recomputeScroll();
    }

    private void recomputeScroll() {
        int rows = (this.results.size() + 13 - 1) / 13;
        this.maxScroll = Math.max(0, rows * 26 - this.gridH);
        this.scroll = Math.max(0.0f, Math.min(this.scroll, (float)this.maxScroll));
    }

    private void refilter() {
        this.results = ItemCatalog.search((String)this.query);
        this.scroll = 0.0f;
        this.recomputeScroll();
    }

    private int[] searchRect() {
        return new int[]{this.contentX, this.panelY + 30, this.innerW, 22};
    }

    private int[] cellRect(int i) {
        int col = i % 13;
        int row = i / 13;
        int gridW = 338;
        int originX = this.contentX + (this.innerW - gridW) / 2;
        return new int[]{originX + col * 26, this.gridY + row * 26 - Math.round(this.scroll), 26, 26};
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
        Gui.panel((class_332)ctx, (int)this.panelX, (int)this.panelY, (int)420, (int)this.panelH);
        ctx.method_25294(this.panelX + 1, this.gridY - 6, this.panelX + 420 - 1, this.gridY - 5, 0xFFFFFFF);
        Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.bold((String)this.prompt), (int)this.contentX, (int)(this.panelY + 14), (int)-1);
        this.renderSearch(ctx, mouseX, mouseY);
        ctx.method_44379(this.panelX + 1, this.gridY, this.panelX + 420 - 1, this.gridY + this.gridH);
        this.renderGrid(ctx, mouseX, mouseY);
        ctx.method_44380();
        if (this.maxScroll > 0) {
            int barH = Math.max(24, Math.round((float)this.gridH * (float)this.gridH / (float)(this.gridH + this.maxScroll)));
            int barY = this.gridY + Math.round(this.scroll / (float)this.maxScroll * (float)(this.gridH - barH));
            Gui.roundedRect((class_332)ctx, (int)(this.panelX + 420 - 7), (int)this.gridY, (int)3, (int)this.gridH, (int)1, (int)0x12FFFFFF);
            Gui.roundedRect((class_332)ctx, (int)(this.panelX + 420 - 7), (int)barY, (int)3, (int)barH, (int)1, (int)0x66FFFFFF);
        }
        class_2561 count = Gui.font((String)(this.results.size() + " items   \u00b7   Esc to cancel"));
        Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)count, (int)(this.panelX + 210), (int)(this.panelY + this.panelH - 17), (int)-9143937);
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    private void renderSearch(class_332 ctx, int mouseX, int mouseY) {
        int[] r = this.searchRect();
        int rad = r[3] / 2;
        Gui.roundedRect((class_332)ctx, (int)r[0], (int)r[1], (int)r[2], (int)r[3], (int)rad, (int)0x1AFFFFFF);
        Gui.roundedRect((class_332)ctx, (int)(r[0] + 1), (int)(r[1] + 1), (int)(r[2] - 2), (int)(r[3] - 2), (int)(rad - 1), (int)-16250872);
        Icons.search((class_332)ctx, (int)(r[0] + 3), (int)(r[1] + Icons.box((int)r[3])), (int)-3419946, (int)-16250872);
        int tx = r[0] + 24;
        int ty = r[1] + (r[3] - 9) / 2;
        if (this.query.isEmpty()) {
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Search items and blocks..."), (int)tx, (int)ty, (int)-9143937);
        } else {
            Gui.text((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)this.query), (int)tx, (int)ty, (int)-1);
            if (System.currentTimeMillis() / 500L % 2L == 0L) {
                int cx = tx + this.field_22793.method_27525((class_5348)Gui.font((String)this.query));
                ctx.method_25294(cx + 1, ty - 1, cx + 2, ty + 9, -1);
            }
        }
    }

    private void renderGrid(class_332 ctx, int mouseX, int mouseY) {
        if (this.results.isEmpty()) {
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)"Nothing matches that."), (int)(this.panelX + 210), (int)(this.gridY + 20), (int)-6578010);
            return;
        }
        ItemCatalog$Entry hovered = null;
        int[] hoveredRect = null;
        for (int i = 0; i < this.results.size(); ++i) {
            boolean hover;
            int[] c = this.cellRect(i);
            if (c[1] + 26 < this.gridY || c[1] > this.gridY + this.gridH) continue;
            boolean bl = hover = Gui.inside((double)mouseX, (double)mouseY, (int)c[0], (int)c[1], (int)c[2], (int)c[3]) && mouseY >= this.gridY && mouseY < this.gridY + this.gridH;
            if (hover) {
                Gui.roundedRect((class_332)ctx, (int)(c[0] + 1), (int)(c[1] + 1), (int)24, (int)24, (int)6, (int)0x1AFFFFFF);
                Gui.roundedRect((class_332)ctx, (int)(c[0] + 2), (int)(c[1] + 2), (int)22, (int)22, (int)5, (int)-15066598);
                hovered = this.results.get(i);
                hoveredRect = c;
            }
            ctx.method_51427(this.results.get(i).stack(), c[0] + 5, c[1] + 5);
        }
        if (hovered != null) {
            String label = hovered.name();
            int w = this.field_22793.method_27525((class_5348)Gui.font((String)label)) + 14;
            int bx = Math.max(this.panelX + 6, Math.min((int)(hoveredRect[0] + 13 - w / 2), this.panelX + 420 - 6 - w));
            int by = this.gridY + this.gridH - 22;
            Gui.roundedRect((class_332)ctx, (int)bx, (int)by, (int)w, (int)18, (int)9, (int)0x1AFFFFFF);
            Gui.roundedRect((class_332)ctx, (int)(bx + 1), (int)(by + 1), (int)(w - 2), (int)16, (int)8, (int)-15527149);
            Gui.centered((class_332)ctx, (class_327)this.field_22793, (class_2561)Gui.font((String)label), (int)(bx + w / 2), (int)(by + 5), (int)-1);
        }
    }

    public boolean method_25402(class_11909 click, boolean doubled) {
        double mx = click.comp_4798();
        double my = click.comp_4799();
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }
        if (my >= (double)this.gridY && my < (double)(this.gridY + this.gridH)) {
            for (int i = 0; i < this.results.size(); ++i) {
                int[] c = this.cellRect(i);
                if (!Gui.inside((double)mx, (double)my, (int)c[0], (int)c[1], (int)c[2], (int)c[3])) continue;
                Sfx.click();
                class_1792 picked = this.results.get(i).item();
                this.field_22787.method_1507(this.parent);
                this.onPick.accept(picked);
                return true;
            }
        }
        return super.method_25402(click, doubled);
    }

    public boolean method_25401(double mouseX, double mouseY, double horizontal, double vertical) {
        if (this.maxScroll > 0) {
            this.scroll = Math.max(0.0f, Math.min(this.scroll - (float)vertical * 26.0f, (float)this.maxScroll));
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontal, vertical);
    }

    public boolean method_25404(class_11908 input) {
        int code = input.comp_4795();
        if (code == 259) {
            if (!this.query.isEmpty()) {
                this.query = this.query.substring(0, this.query.length() - 1);
                this.refilter();
            }
            return true;
        }
        if (code == 256) {
            this.method_25419();
            return true;
        }
        return super.method_25404(input);
    }

    public boolean method_25400(class_11905 input) {
        char c = (char)input.comp_4793();
        if (c >= ' ') {
            this.query = this.query + c;
            this.refilter();
            return true;
        }
        return super.method_25400(input);
    }

    public void method_25419() {
        this.field_22787.method_1507(this.parent);
    }

    public boolean method_25421() {
        return false;
    }
}
