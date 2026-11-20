/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_437
 */
package com.fakeclient.module;

import java.util.function.Supplier;
import net.minecraft.class_437;

public abstract class Module {
    private final String name;
    private final String description;
    private String category = "General";
    private boolean enabled;
    private boolean maintenance;
    private int key = -1;
    private Supplier<class_437> editor;

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void toggle() {
        this.setEnabled(!this.enabled);
    }

    public boolean isUnderMaintenance() {
        return this.maintenance;
    }

    protected void setMaintenance(boolean value) {
        this.maintenance = value;
        if (value) {
            this.setEnabled(false);
        }
    }

    public void setEnabled(boolean value) {
        if (this.maintenance) {
            value = false;
        }
        if (value == this.enabled) {
            return;
        }
        this.enabled = value;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    protected void setEditor(Supplier<class_437> editor) {
        this.editor = editor;
    }

    public boolean hasEditor() {
        return this.editor != null;
    }

    public class_437 createEditor() {
        return this.editor == null ? null : this.editor.get();
    }

    public void tick() {
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
