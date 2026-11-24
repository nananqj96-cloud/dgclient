package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class CustomCrosshairModule extends Module {
    public final ModeSetting style = this.addSetting(new ModeSetting("Style", "Style", "Cross", "Cross", "T", "X", "Dot", "Circle"));
    private final SliderSetting size = this.addSetting(new SliderSetting("Size", "Size", 6.0, 1.0, 20.0, 1.0F));
    private final SliderSetting gap = this.addSetting(new SliderSetting("Gap", "Gap", 2.0, 0.0, 16.0, 1.0F));
    private final SliderSetting thickness = this.addSetting(new SliderSetting("Thickness", "Thickness", 2.0, 1.0, 5.0, 1.0F));
    private final BooleanSetting centerDot = this.addSetting(new BooleanSetting("Center Dot", "Center Dot", false));
    private final BooleanSetting outline = this.addSetting(new BooleanSetting("Outline", "Outline", true));
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Color", "Custom", "Custom", "Accent"));
    private final ColorSetting color = this.addSetting(new ColorSetting("Custom Color", "Custom Color", -657929));
    private final BooleanSetting targetTint = this.addSetting(new BooleanSetting("Target Tint", "Target Tint", false));
    private final ColorSetting targetColor = this.addSetting(new ColorSetting("Target Color", "Target Color", -47814));
    private float scale = 1.0F;

    public CustomCrosshairModule() {
        super("custom-crosshair", "Draws a configurable crosshair in place of the vanilla one.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        this.scale = 1.0F;
    }

    public String getStyle() {
        return this.style.get();
    }

    public float getSize() {
        return this.size.getFloat();
    }

    public float getGap() {
        return this.gap.getFloat();
    }

    public float getThickness() {
        return this.thickness.getFloat();
    }

    public int getColor() {
        return this.color.get();
    }

    public int getTargetColor() {
        return this.targetColor.get();
    }
}
