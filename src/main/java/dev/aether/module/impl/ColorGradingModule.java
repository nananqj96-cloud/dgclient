package dev.aether.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class ColorGradingModule extends Module {
    public static volatile ColorGradingModule active;
    private final SliderSetting vibrance = this.addSetting(new SliderSetting("Vibrance", 35.0, 0.0, 100.0, 1.0F));
    private final SliderSetting contrast = this.addSetting(new NumberSetting("Contrast", 8.0F, -20.0F, 40.0F));
    private final SliderSetting brightness = this.addSetting(new NumberSetting("Brightness", 3.0F, -20.0F, 30.0F));
    private final SliderSetting warmth = this.addSetting(new NumberSetting("Warmth", 15.0F, -100.0F, 100.0F));
    private final SliderSetting vignette = this.addSetting(new SliderSetting("Vignette", 30.0, 0.0, 100.0, 1.0F));

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
