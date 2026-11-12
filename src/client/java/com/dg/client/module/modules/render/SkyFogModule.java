package com.dg.client.module.modules.render;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.util.ColorUtil;

/**
 * Client-side fog & horizon customizer with a signature DGClient neon emerald/white preset
 * or complete fog removal for maximum anarchy visibility.
 */
public final class SkyFogModule extends Module {
    public enum Preset {
        DG_EMERALD,
        MISTY_WHITE,
        CUSTOM,
        CLEAR_FOG
    }

    public final EnumSetting<Preset> preset = addSetting(new EnumSetting<>(
            "Preset", "Aesthetic fog color & distance preset", Preset.DG_EMERALD));
    public final ColorSetting customColor = addSetting(new ColorSetting(
            "Fog Color", "Custom client-side fog & horizon color", 0xFF143522)
            .visibleWhen(() -> preset.getValue() == Preset.CUSTOM));
    public final DoubleSetting fogStartRatio = addSetting(new DoubleSetting(
            "Fog Start", "Start distance multiplier relative to render distance", 0.45, 0.05, 2.0, 0.05)
            .visibleWhen(() -> preset.getValue() != Preset.CLEAR_FOG));
    public final DoubleSetting fogEndRatio = addSetting(new DoubleSetting(
            "Fog End", "End distance multiplier relative to render distance", 1.15, 0.2, 3.0, 0.05)
            .visibleWhen(() -> preset.getValue() != Preset.CLEAR_FOG));
    public final DoubleSetting density = addSetting(new DoubleSetting(
            "Density", "Fog color blend strength", 0.85, 0.0, 1.0, 0.05));
    public final BooleanSetting removeLavaWaterFog = addSetting(new BooleanSetting(
            "Clear Liquid Fog", "Improves visibility inside lava and water", true));

    public SkyFogModule() {
        super("SkyFog", "Client-side fog color, density, distance, and green/white aesthetic preset", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return preset.getValue().name();
    }

    public int getActiveFogColor() {
        return switch (preset.getValue()) {
            case DG_EMERALD -> 0xFF102E1C;
            case MISTY_WHITE -> 0xFFDCEEE4;
            case CUSTOM -> customColor.getColor();
            case CLEAR_FOG -> ColorUtil.PURE_WHITE;
        };
    }
}
