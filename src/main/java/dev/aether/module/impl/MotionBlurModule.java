package dev.aether.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class MotionBlurModule extends Module {
    public static volatile MotionBlurModule active;
    private final SliderSetting strength = this.addSetting(new SliderSetting("Strength", "Strength", 50.0, 10.0, 90.0, 5.0F));

    public MotionBlurModule() {
        super("motion-blur", "Adds motion blur when you turn.", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        if (RenderSystem.isOnRenderThread()) {
            MotionBlurPass.release();
        } else {
            Minecraft.getInstance().execute(MotionBlurModule::releaseIfOff);
        }
    }

    private static void releaseIfOff() {
        if (active == null) {
            MotionBlurPass.release();
        }
    }

    public static void afterWorld(boolean worldRendered) {
        MotionBlurModule module = active;
        if (module != null) {
            MotionBlurPass.apply(worldRendered, module.strength.get() / 100.0F);
        }
    }
}
