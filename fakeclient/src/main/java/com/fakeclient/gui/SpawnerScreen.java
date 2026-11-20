/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.Gui
 *  net.minecraft.class_10799
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_1935
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_437
 */
package com.fakeclient.gui;

import com.fakeclient.Settings;
import com.fakeclient.gui.Gui;
import net.minecraft.class_10799;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class SpawnerScreen
extends class_437 {
    private static final class_2960 TEXTURE = class_2960.method_60655((String)"minecraft", (String)"textures/gui/container/generic_54.png");
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SLOT = 18;
    private static final int SLOT_X = 8;
    private static final int SLOT_Y = 18;
    private static final int BG_W = 176;
    private static final int TOP_H = 125;
    private static final int BOTTOM_H = 96;
    private static final int BG_H = 221;
    private static final int INVENTORY_V = 126;
    private static final int LABEL = -12566464;
    private final class_1799[] shown = new class_1799[]{new class_1799((class_1935)class_1802.field_8695), new class_1799((class_1935)class_1802.field_8398), new class_1799((class_1935)class_1802.field_8878)};
    private int left;
    private int top;

    public SpawnerScreen() {
        super((class_2561)class_2561.method_43470((String)"Skeleton Spawners"));
    }

    protected void method_25426() {
        this.left = (this.field_22789 - 176) / 2;
        this.top = (this.field_22790 - 221) / 2;
    }

    private class_2561 titleText() {
        int count = Settings.get().spawnerCount;
        return class_2561.method_43470((String)(count + (count == 1 ? " Skeleton Spawner" : " Skeleton Spawners")));
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
        ctx.method_25290(class_10799.field_56883, TEXTURE, this.left, this.top, 0.0f, 0.0f, 176, 125, 256, 256);
        ctx.method_25290(class_10799.field_56883, TEXTURE, this.left, this.top + 125, 0.0f, 126.0f, 176, 96, 256, 256);
        ctx.method_51439(this.field_22793, this.titleText(), this.left + 8, this.top + 6, -12566464, false);
        int row = 5;
        int firstCol = (9 - this.shown.length) / 2;
        for (int i = 0; i < this.shown.length; ++i) {
            int sx = this.left + 8 + (firstCol + i) * 18;
            int sy = this.top + 18 + row * 18;
            ctx.method_51427(this.shown[i], sx, sy);
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
    }

    public boolean method_25421() {
        return false;
    }
}
