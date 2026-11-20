/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.gui.Gui
 *  net.minecraft.class_332
 *  org.joml.Matrix3x2fStack
 */
package com.fakeclient.gui;

import com.fakeclient.gui.Gui;
import net.minecraft.class_332;
import org.joml.Matrix3x2fStack;

public final class Icons {
    public static final int SIZE = 16;
    private static final int HOLE = -671088640;

    private Icons() {
    }

    public static int box(int side) {
        return (side - 16) / 2;
    }

    public static void gear(class_332 ctx, int x, int y, int color, int bg, float turn) {
        boolean rotated;
        Matrix3x2fStack mat = ctx.method_51448();
        boolean bl = rotated = Math.abs(turn) > 1.0E-4f;
        if (rotated) {
            mat.pushMatrix();
            mat.translate((float)x + 8.0f, (float)y + 8.0f);
            mat.rotate(turn);
            mat.translate((float)(-x) - 8.0f, (float)(-y) - 8.0f);
        }
        ctx.method_25294(x + 6, y, x + 10, y + 4, color);
        ctx.method_25294(x + 6, y + 12, x + 10, y + 16, color);
        ctx.method_25294(x, y + 6, x + 4, y + 10, color);
        ctx.method_25294(x + 12, y + 6, x + 16, y + 10, color);
        ctx.method_25294(x + 2, y + 2, x + 5, y + 5, color);
        ctx.method_25294(x + 11, y + 2, x + 14, y + 5, color);
        ctx.method_25294(x + 2, y + 11, x + 5, y + 14, color);
        ctx.method_25294(x + 11, y + 11, x + 14, y + 14, color);
        Gui.roundedRect((class_332)ctx, (int)(x + 3), (int)(y + 3), (int)10, (int)10, (int)5, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 6), (int)(y + 6), (int)4, (int)4, (int)2, (int)-671088640);
        if (rotated) {
            mat.popMatrix();
        }
    }

    public static void folder(class_332 ctx, int x, int y, int color, int bg, float lift) {
        int dy = Math.round(Gui.clamp01((float)lift) * 2.0f);
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 2 - dy), (int)6, (int)3, (int)1, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 4), (int)14, (int)10, (int)3, (int)color);
        ctx.method_25294(x + 3, y + 7, x + 13, y + 8, -671088640);
    }

    public static void search(class_332 ctx, int x, int y, int color, int bg) {
        Gui.roundedRect((class_332)ctx, (int)(x + 3), (int)(y + 3), (int)8, (int)8, (int)4, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 5), (int)(y + 5), (int)4, (int)4, (int)2, (int)-671088640);
        for (int i = 0; i < 3; ++i) {
            ctx.method_25294(x + 9 + i, y + 9 + i, x + 11 + i, y + 11 + i, color);
        }
    }

    public static void grid(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 2), (int)(y + 2), (int)5, (int)5, (int)2, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 9), (int)(y + 2), (int)5, (int)5, (int)2, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 2), (int)(y + 9), (int)5, (int)5, (int)2, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 9), (int)(y + 9), (int)5, (int)5, (int)2, (int)color);
    }

    public static void eye(class_332 ctx, int x, int y, int color, int bg) {
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 4), (int)14, (int)8, (int)4, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 3), (int)(y + 6), (int)10, (int)4, (int)2, (int)-671088640);
        Gui.roundedRect((class_332)ctx, (int)(x + 6), (int)(y + 5), (int)5, (int)6, (int)2, (int)color);
    }

    public static void camera(class_332 ctx, int x, int y, int color, int bg) {
        Gui.roundedRect((class_332)ctx, (int)(x + 4), (int)(y + 2), (int)7, (int)3, (int)1, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 4), (int)14, (int)10, (int)3, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 6), (int)(y + 7), (int)5, (int)5, (int)2, (int)-671088640);
    }

    public static void dot(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 4), (int)(y + 4), (int)8, (int)8, (int)4, (int)color);
    }

    public static void note(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 4), (int)14, (int)9, (int)2, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 3), (int)(y + 6), (int)10, (int)5, (int)2, (int)-671088640);
        Gui.roundedRect((class_332)ctx, (int)(x + 6), (int)(y + 6), (int)4, (int)5, (int)2, (int)color);
    }

    public static void chat(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 2), (int)14, (int)10, (int)3, (int)color);
        ctx.method_25294(x + 4, y + 12, x + 8, y + 13, color);
        ctx.method_25294(x + 4, y + 13, x + 6, y + 14, color);
        ctx.method_25294(x + 4, y + 14, x + 5, y + 15, color);
        ctx.method_25294(x + 4, y + 6, x + 6, y + 8, -671088640);
        ctx.method_25294(x + 7, y + 6, x + 9, y + 8, -671088640);
        ctx.method_25294(x + 10, y + 6, x + 12, y + 8, -671088640);
    }

    public static void board(class_332 ctx, int x, int y, int color) {
        for (int i = 0; i < 3; ++i) {
            int ry = y + 3 + i * 4;
            Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)ry, (int)2, (int)2, (int)1, (int)color);
            Gui.roundedRect((class_332)ctx, (int)(x + 5), (int)ry, (int)(i == 1 ? 7 : 10), (int)2, (int)1, (int)color);
        }
    }

    public static void stack(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 8), (int)11, (int)3, (int)1, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 1), (int)(y + 12), (int)11, (int)3, (int)1, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 7), (int)(y + 1), (int)8, (int)6, (int)3, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 9), (int)(y + 3), (int)4, (int)2, (int)1, (int)-671088640);
    }

    public static void cage(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 2), (int)(y + 2), (int)12, (int)12, (int)3, (int)color);
        Gui.roundedRect((class_332)ctx, (int)(x + 4), (int)(y + 4), (int)8, (int)8, (int)2, (int)-671088640);
        ctx.method_25294(x + 7, y + 4, x + 9, y + 12, color);
        ctx.method_25294(x + 4, y + 7, x + 12, y + 9, color);
    }

    public static void swap(class_332 ctx, int x, int y, int color) {
        Gui.roundedRect((class_332)ctx, (int)(x + 2), (int)(y + 4), (int)10, (int)2, (int)1, (int)color);
        ctx.method_25294(x + 10, y + 2, x + 12, y + 4, color);
        ctx.method_25294(x + 11, y + 3, x + 14, y + 7, color);
        Gui.roundedRect((class_332)ctx, (int)(x + 4), (int)(y + 10), (int)10, (int)2, (int)1, (int)color);
        ctx.method_25294(x + 4, y + 12, x + 6, y + 14, color);
        ctx.method_25294(x + 2, y + 9, x + 5, y + 13, color);
    }

    public static void module(class_332 ctx, String name, int x, int y, int color, int bg) {
        switch (name) {
            case "Fake Pay": {
                Icons.note(ctx, x, y, color);
                break;
            }
            case "Pay Simulator": {
                Icons.chat(ctx, x, y, color);
                break;
            }
            case "Fake Scoreboard": {
                Icons.board(ctx, x, y, color);
                break;
            }
            case "Fake Item Count": {
                Icons.stack(ctx, x, y, color);
                break;
            }
            case "Fake Spawner": {
                Icons.cage(ctx, x, y, color);
                break;
            }
            case "Fake Media": {
                Icons.camera(ctx, x, y, color, bg);
                break;
            }
            case "Texture Changer": {
                Icons.swap(ctx, x, y, color);
                break;
            }
            default: {
                Icons.dot(ctx, x, y, color);
            }
        }
    }

    public static void category(class_332 ctx, String cat, int x, int y, int color, int bg) {
        switch (cat) {
            case "All": {
                Icons.grid(ctx, x, y, color);
                break;
            }
            case "Payments": {
                Icons.note(ctx, x, y, color);
                break;
            }
            case "Visuals": {
                Icons.eye(ctx, x, y, color, bg);
                break;
            }
            case "Media": {
                Icons.camera(ctx, x, y, color, bg);
                break;
            }
            default: {
                Icons.dot(ctx, x, y, color);
            }
        }
    }
}
