package com.dg.client.module.modules.hud;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;

/**
 * Master HUD controller with toggles for every draggable HUD component.
 */
public final class HudModule extends Module {
    public final BooleanSetting watermark = addSetting(new BooleanSetting(
            "Watermark", "Animated glowing DGClient watermark and version pill", true));
    public final BooleanSetting arrayList = addSetting(new BooleanSetting(
            "ArrayList", "Slide-in sorted list of enabled modules with neon side bar", true));
    public final BooleanSetting coordinates = addSetting(new BooleanSetting(
            "Coordinates", "XYZ coordinates with live Overworld/Nether conversion", true));
    public final BooleanSetting telemetry = addSetting(new BooleanSetting(
            "Telemetry", "FPS, Ping, TPS, Speed (m/s), Direction, Biome, and World Time", true));
    public final BooleanSetting armorHud = addSetting(new BooleanSetting(
            "Armor HUD", "Equipped armor icons with glowing durability bars", true));
    public final BooleanSetting potionHud = addSetting(new BooleanSetting(
            "Potion Effects", "Active status effects with timers", true));
    public final BooleanSetting totemCounter = addSetting(new BooleanSetting(
            "Totem Counter", "Inventory + offhand Totem of Undying count", true));
    public final BooleanSetting toasts = addSetting(new BooleanSetting(
            "Toast Alerts", "Top-right slide-in notifications for toggles & detections", true));
    public final DoubleSetting hudGlow = addSetting(new DoubleSetting(
            "HUD Glow", "Glow intensity on HUD cards and bars", 0.9, 0.0, 2.0, 0.05));

    public HudModule() {
        super("HUD", "Renders draggable neon green & white in-game HUD overlays", ModuleCategory.HUD);
        setVisibleInArrayList(false);
        setEnabled(true, false);
    }

    @Override
    protected boolean shouldToastToggle() {
        return false;
    }
}
