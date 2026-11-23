package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AtmosphereModule extends Module {
    private final ModeSetting theme = this.addSetting(new ModeSetting("Theme", "Theme", "Egyptian", "Egyptian", "Desert", "Night"));
    private final ColorSetting nightColor = this.addSetting(new ColorSetting("Night Sky Color", "Night Sky Color", -16047568));
    private final ColorSetting sunsetColor = this.addSetting(new ColorSetting("Sunset Color", "Sunset Color", -3703914));
    private final ColorSetting auroraColor = this.addSetting(new ColorSetting("Aurora Color", "Aurora Color", -13697126));
    private final ColorSetting auroraTopColor = this.addSetting(new ColorSetting("Aurora Top Color", "Aurora Top Color", -7713281));
    private final ModeSetting skyShader = this.addSetting(new ModeSetting("Sky Shader", "Sky Shader", "Egyptian", "Egyptian", "Default"));
    private final SliderSetting shaderDaytime = this.addSetting(new SliderSetting("Daytime Strength", "Daytime Strength", 1.0, 0.0, 1.0, 0.05F));
    private final SliderSetting shaderBrightness = this.addSetting(new SliderSetting("Shader Brightness", "Shader Brightness", 1.0, 0.2, 2.0, 0.1F));
    private final SliderSetting endSky = this.addSetting(new SliderSetting("End Sky", "End Sky", 1.0, 0.0, 1.0, 0.05F));
    private final SliderSetting netherColors = this.addSetting(new SliderSetting("Nether Colors", "Nether Colors", 1.0, 0.0, 1.0, 0.05F));

    public AtmosphereModule() {
        super("atmosphere", "A custom sky with stars and an aurora.", Category.VISUALS);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Atmosphere rendering stub - core sky/stars/aurora functionality
        // preserved as module registration; detailed rendering disabled
    }

    public String getTheme() {
        return this.theme.get();
    }

    public String getSkyShader() {
        return this.skyShader.get();
    }

    public int getNightColor() {
        return this.nightColor.get();
    }

    public int getSunsetColor() {
        return this.sunsetColor.get();
    }

    public int getAuroraColor() {
        return this.auroraColor.get();
    }
}
