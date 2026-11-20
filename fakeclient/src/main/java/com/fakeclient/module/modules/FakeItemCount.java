/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.Settings
 *  com.fakeclient.gui.FakeItemCountScreen
 *  com.fakeclient.module.Module
 */
package com.fakeclient.module.modules;

import com.fakeclient.Settings;
import com.fakeclient.gui.FakeItemCountScreen;
import com.fakeclient.module.Module;

public class FakeItemCount
extends Module {
    private static final int MAX_COUNT = 9999999;

    public FakeItemCount() {
        super("Fake Item Count", "Show a set count on every stack.");
        this.setEditor(() -> new FakeItemCountScreen(this));
    }

    public int fake(int real) {
        return Math.min(9999999, Math.max(1, Settings.get().itemMultiplier));
    }
}
