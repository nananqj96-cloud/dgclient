package dev.aether.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class ColorGradingModule extends Module {
    public static volatile ColorGradingModule active;
    private final SliderSetting vibrance = this.addSetting(new SliderSetting("Vibrance", "Vibrance", 35.0, 0.0, 100.0, 1.0F));
    private final SliderSetting contrast = this.addSetting(new SliderSetting("Contrast", "Contrast", 8.0, -20.0, 40.0, 1.0F));
    private final SliderSetting brightness = this.addSetting(new SliderSetting("Brightness", "Brightness", 3.0, -20.0, 30.0, 1.0F));
    private final SliderSetting warmth = this.addSetting(new SliderSetting("Warmth", "Warmth", 15.0, -100.0, 100.0, 1.0F));
    private final SliderSetting vignette = this.addSetting(new SliderSetting("Vignette", "Vignette", 30.0, 0.0, 100.0, 1.0F));

    public ColorGradingModule() {
        super("color-grading", "Adds color, contrast and a vignette to the world.", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        if (RenderSystem.isOnRenderThread()) {
            ColorGradingPass.release();
        } else {
            Minecraft.getInstance().execute(ColorGradingModule::releaseIfOff);
        }
    }

    private static void releaseIfOff() {
        if (active == null) {
            ColorGradingPass.release();
        }
    }

    public static void afterWorld(boolean worldRendered) {
        ColorGradingModule module = active;
        if (module != null) {
            ColorGradingPass.apply(
                worldRendered,
                module.vibrance.get() / 100.0F,
                1.0F + module.contrast.get() / 100.0F,
                1.0F + module.brightness.get() / 100.0F,
                module.warmth.get() / 100.0F,
                module.vignette.get() / 100.0F
            );
        }
    }
}
