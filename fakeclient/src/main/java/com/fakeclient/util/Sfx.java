/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  net.minecraft.class_1109
 *  net.minecraft.class_1113
 *  net.minecraft.class_310
 *  net.minecraft.class_3417
 *  net.minecraft.class_6880
 */
package com.fakeclient.util;

import com.fakeclient.Settings;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_310;
import net.minecraft.class_3417;
import net.minecraft.class_6880;

public final class Sfx {
    private Sfx() {
    }

    public static void click() {
        if (!Settings.get().menuSound) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc != null) {
            mc.method_1483().method_4872((class_1113)class_1109.method_47978((class_6880)class_3417.field_15015, (float)1.0f), 0);
        }
    }
}
