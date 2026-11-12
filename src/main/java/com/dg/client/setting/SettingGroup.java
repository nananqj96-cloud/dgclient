package com.dg.client.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Logical grouping of module settings rendered as an expandable section in ClickGUI.
 */
public final class SettingGroup {
    private final String name;
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean expanded = true;

    public SettingGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}
