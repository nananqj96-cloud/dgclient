package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class LightEspModule extends Module {
    private final SliderSetting sRadius = this.addSetting(new SliderSetting("Render Radius", "Render Radius", 8.0, 1.0, 16.0, 1.0F));
    private final SliderSetting sMinLight = this.addSetting(new SliderSetting("Min Light", "Min Light", 5.0, 1.0, 15.0, 1.0F));
    private final BooleanSetting sDenoise = this.addSetting(new BooleanSetting("Denoise", "Denoise", true));
    private final ColorSetting sColor = this.addSetting(new ColorSetting("Color", "Color", -2130719744));

    public LightEspModule() {
        super("light-debug", "Highlights artificial light underground.", Category.EXPLOIT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Light ESP rendering stub - core light detection/rendering functionality
        // preserved as module registration; detailed rendering disabled
    }

    public float getRadius() {
        return this.sRadius.getFloat();
    }

    public float getMinLight() {
        return this.sMinLight.getFloat();
    }

    public boolean getDenoise() {
        return this.sDenoise.get();
    }

    public int getColor() {
        return this.sColor.get();
    }
}
