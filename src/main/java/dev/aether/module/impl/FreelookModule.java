package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FreelookModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Player", "Player", "Camera"));
    private final BooleanSetting togglePerspective = this.addSetting(new BooleanSetting("Toggle Perspective", "Toggle Perspective", true));
    private final SliderSetting sensitivity = this.addSetting(new SliderSetting("Camera Sensitivity", "Camera Sensitivity", 8.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting arrows = this.addSetting(new BooleanSetting("Arrows Control Opposite", "Arrows Control Opposite", true));
    private final SliderSetting arrowSpeed = this.addSetting(new SliderSetting("Arrow Speed", "Arrow Speed", 4.0, 0.0, 10.0, 0.5F));

    public FreelookModule() {
        super("freelook", "Turns the camera and the player separately.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        // Freelook logic stub - core camera/player separation functionality
        // preserved as module registration; detailed camera logic disabled
    }

    public String getMode() {
        return this.mode.get();
    }

    public float getSensitivity() {
        return this.sensitivity.getFloat();
    }

    public float getArrowSpeed() {
        return this.arrowSpeed.getFloat();
    }
}
