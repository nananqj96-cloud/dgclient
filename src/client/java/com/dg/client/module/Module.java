package com.dg.client.module;

import com.dg.client.DGClient;
import com.dg.client.core.DGLogger;
import com.dg.client.core.NotificationManager;
import com.dg.client.setting.KeybindSetting;
import com.dg.client.setting.Setting;
import com.dg.client.setting.SettingGroup;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all DGClient modules.
 * Manages lifecycle hooks, event bus registration, settings groups, JSON persistence,
 * and UI animation state.
 */
public abstract class Module {
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final List<SettingGroup> settingGroups = new ArrayList<>();
    private final SettingGroup generalGroup = new SettingGroup("General");
    private final KeybindSetting keybind;

    private boolean enabled;
    private boolean visibleInArrayList = true;

    // UI animation state (0..1)
    private float toggleAnim = 0.0f;
    private float expandAnim = 0.0f;
    private float arraySlideAnim = 0.0f;
    private boolean settingsOpen = false;

    protected Module(String name, String description, ModuleCategory category) {
        this(name, description, category, KeybindSetting.NONE);
    }

    protected Module(String name, String description, ModuleCategory category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.settingGroups.add(generalGroup);
        this.keybind = new KeybindSetting("Keybind", "Key used to toggle " + name, defaultKey);
    }

    protected final SettingGroup createGroup(String groupName) {
        SettingGroup group = new SettingGroup(groupName);
        settingGroups.add(group);
        return group;
    }

    protected final <S extends Setting<?>> S addSetting(S setting) {
        return generalGroup.add(setting);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public KeybindSetting getKeybind() {
        return keybind;
    }

    public List<SettingGroup> getSettingGroups() {
        return Collections.unmodifiableList(settingGroups);
    }

    public List<Setting<?>> getAllSettings() {
        List<Setting<?>> all = new ArrayList<>();
        for (SettingGroup group : settingGroups) {
            all.addAll(group.getSettings());
        }
        return all;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled, true);
    }

    public void setEnabled(boolean state) {
        setEnabled(state, true);
    }

    public void setEnabled(boolean state, boolean notify) {
        if (this.enabled == state) {
            return;
        }
        this.enabled = state;
        if (state) {
            try {
                DGClient.getInstance().getEventBus().subscribe(this);
                onEnable();
                if (notify && shouldToastToggle()) {
                    DGClient.getInstance().getNotificationManager().push(
                            NotificationManager.Type.MODULE_ON,
                            name,
                            "Enabled"
                    );
                }
            } catch (Throwable t) {
                this.enabled = false;
                DGClient.getInstance().getEventBus().unsubscribe(this);
                DGLogger.error("Module failed during onEnable: " + name, t);
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        name + " Disabled",
                        "Error in onEnable: " + t.getClass().getSimpleName()
                );
            }
        } else {
            DGClient.getInstance().getEventBus().unsubscribe(this);
            try {
                onDisable();
            } catch (Throwable t) {
                DGLogger.error("Module error during onDisable: " + name, t);
            }
            if (notify && shouldToastToggle()) {
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.MODULE_OFF,
                        name,
                        "Disabled"
                );
            }
        }
    }

    protected boolean shouldToastToggle() {
        return mc != null && mc.world != null && category != ModuleCategory.HUD;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /**
     * Optional metadata tag shown next to the module name in the HUD ArrayList.
     */
    public String getDisplayInfo() {
        return null;
    }

    public boolean isVisibleInArrayList() {
        return visibleInArrayList && category != ModuleCategory.HUD;
    }

    public void setVisibleInArrayList(boolean visibleInArrayList) {
        this.visibleInArrayList = visibleInArrayList;
    }

    public boolean isSettingsOpen() {
        return settingsOpen;
    }

    public void setSettingsOpen(boolean settingsOpen) {
        this.settingsOpen = settingsOpen;
    }

    public float getToggleAnim() {
        return toggleAnim;
    }

    public void setToggleAnim(float toggleAnim) {
        this.toggleAnim = toggleAnim;
    }

    public float getExpandAnim() {
        return expandAnim;
    }

    public void setExpandAnim(float expandAnim) {
        this.expandAnim = expandAnim;
    }

    public float getArraySlideAnim() {
        return arraySlideAnim;
    }

    public void setArraySlideAnim(float arraySlideAnim) {
        this.arraySlideAnim = arraySlideAnim;
    }

    public void resetSettings() {
        for (Setting<?> setting : getAllSettings()) {
            setting.reset();
        }
        keybind.reset();
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        obj.addProperty("visibleInArrayList", visibleInArrayList);
        obj.add("keybind", keybind.toJson());
        JsonObject settingsObj = new JsonObject();
        for (SettingGroup group : settingGroups) {
            for (Setting<?> setting : group.getSettings()) {
                settingsObj.add(group.getName() + "::" + setting.getName(), setting.toJson());
            }
        }
        obj.add("settings", settingsObj);
        return obj;
    }

    public void fromJson(JsonObject obj) {
        if (obj == null) {
            return;
        }
        if (obj.has("visibleInArrayList")) {
            this.visibleInArrayList = obj.get("visibleInArrayList").getAsBoolean();
        }
        if (obj.has("keybind")) {
            this.keybind.fromJson(obj.get("keybind"));
        }
        if (obj.has("settings") && obj.get("settings").isJsonObject()) {
            JsonObject settingsObj = obj.getAsJsonObject("settings");
            for (SettingGroup group : settingGroups) {
                for (Setting<?> setting : group.getSettings()) {
                    String scopedKey = group.getName() + "::" + setting.getName();
                    JsonElement val = settingsObj.has(scopedKey) ? settingsObj.get(scopedKey) : settingsObj.get(setting.getName());
                    if (val != null) {
                        try {
                            setting.fromJson(val);
                        } catch (Throwable t) {
                            DGLogger.warn("Failed loading setting {} on {}: {}", setting.getName(), name, t.getMessage());
                        }
                    }
                }
            }
        }
        if (obj.has("enabled")) {
            boolean shouldEnable = obj.get("enabled").getAsBoolean();
            setEnabled(shouldEnable, false);
        }
    }
}
