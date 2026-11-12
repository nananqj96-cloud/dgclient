package com.dg.client.module;

/**
 * Primary ClickGUI category columns in exact theme layout order.
 */
public enum ModuleCategory {
    RENDER("Render"),
    WORLD("World"),
    DETECT("Detect"),
    MOVEMENT("Movement"),
    FUN("Fun"),
    MISC("Misc"),
    HUD("HUD");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
