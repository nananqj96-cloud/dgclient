package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ColorGradingModule extends Module {
    private final SliderSetting vibrance = this.addSetting(new SliderSetting("Vibrance", "Vibrance", 35.0, 0.0, 100.0, 1.0F));
    private final SliderSetting contrast = this.addSetting(new SliderSetting("Contrast", "Contrast", 8.0, -20.0, 40.0, 1.0F));
    private final SliderSetting brightness = this.addSetting(new SliderSetting("Brightness", "Brightness", 3.0, -20.0, 30.0, 1.0F));
    private final SliderSetting warmth = this.addSetting(new SliderSetting("Warmth", "Warmth", 15.0, -100.0, 100.0, 1.0F));
    private final SliderSetting vignette = this.addSetting(new SliderSetting("Vignette", "Vignette", 30.0, 0.0, 100.0, 1.0F));

    public ColorGradingModule() {
        super("color-grading", "Adds color, contrast and a vignette to the world.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Color grading logic stub - core color grading functionality
        // preserved as module registration; detailed rendering disabled
    }

    public float getVibrance() {
        return this.vibrance.getFloat();
    }

    public float getContrast() {
        return this.contrast.getFloat();
    }

    public float getBrightness() {
        return this.brightness.getFloat();
    }

    public float getWarmth() {
        return this.warmth.getFloat();
    }

    public float getVignette() {
        return this.vignette.getFloat();
    }
}
