package com.dg.client.module.modules.render;

import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;

/**
 * Renders a geometry-conforming pulsing neon green/white aura around the item held
 * in first-person and third-person view.
 */
public final class HandItemGlowModule extends Module {
    public final BooleanSetting firstPerson = addSetting(new BooleanSetting(
            "First Person", "Applies neon aura to first-person held items", true));
    public final BooleanSetting thirdPerson = addSetting(new BooleanSetting(
            "Third Person", "Applies neon aura to third-person held items", true));
    public final ColorSetting glowColor = addSetting(new ColorSetting(
            "Glow Color", "Primary neon aura color", ColorUtil.withAlpha(ColorUtil.NEON_GREEN, 130), true));
    public final DoubleSetting intensity = addSetting(new DoubleSetting(
            "Intensity", "Opacity and shell expansion of the hand aura", 1.0, 0.2, 2.5, 0.05));
    public final DoubleSetting pulseSpeed = addSetting(new DoubleSetting(
            "Pulse Speed", "Breathing pulse rate of the aura", 1.4, 0.0, 4.0, 0.1));

    public HandItemGlowModule() {
        super("HandItemGlow", "Glowing neon green/white aura around held items in 1st & 3rd person", ModuleCategory.RENDER);
    }

    public int computeCurrentGlowColor() {
        int base = glowColor.getValue();
        double speed = pulseSpeed.getValue();
        int alpha = ColorUtil.getAlpha(base);
        if (speed > 0.01) {
            float wave = (float) ((Math.sin(System.currentTimeMillis() / 1000.0 * speed * 3.0) + 1.0) * 0.5);
            int white = ColorUtil.withAlpha(ColorUtil.PURE_WHITE, alpha);
            base = ColorUtil.lerp(base, white, wave * 0.42f);
        }
        int scaledAlpha = Math.min(220, Math.round(alpha * intensity.getValue().floatValue() * 0.65f));
        return ColorUtil.withAlpha(base, scaledAlpha);
    }
}
