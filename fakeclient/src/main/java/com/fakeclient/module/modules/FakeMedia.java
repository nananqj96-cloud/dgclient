/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.FakeMediaScreen
 *  com.fakeclient.module.Module
 *  net.minecraft.class_11719
 *  net.minecraft.class_11719$class_11721
 *  net.minecraft.class_124
 *  net.minecraft.class_2561
 *  net.minecraft.class_2583
 *  net.minecraft.class_2960
 */
package com.fakeclient.module.modules;

import com.fakeclient.Settings;
import com.fakeclient.gui.FakeMediaScreen;
import com.fakeclient.module.Module;
import net.minecraft.class_11719;
import net.minecraft.class_124;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;

public class FakeMedia
extends Module {
    public static final class_2960 FONT = class_2960.method_60655((String)"fakeclient", (String)"media");
    public static final String ICON = "\ue900";
    private static final class_2583 ICON_STYLE = class_2583.field_24360.method_27704((class_11719)new class_11719.class_11721(FONT)).method_10977(class_124.field_1068);

    public FakeMedia() {
        super("Fake Media", "Wear the media camera above your head, under a name you choose.");
        this.setEditor(() -> new FakeMediaScreen(this));
    }

    public static class_2561 iconText() {
        return class_2561.method_43470((String)ICON).method_10862(ICON_STYLE);
    }

    public static String customName() {
        String n = Settings.get().mediaName;
        return n == null ? "" : n.trim();
    }

    public static void setCustomName(String name) {
        Settings s = Settings.get();
        s.mediaName = name == null ? "" : name.trim();
        s.save();
    }

    public static class_2561 decorate(class_2561 name) {
        String custom = FakeMedia.customName();
        class_2561 shown = custom.isEmpty() ? name : class_2561.method_43470((String)custom);
        return class_2561.method_43473().method_10852(FakeMedia.iconText()).method_10852(shown);
    }
}
