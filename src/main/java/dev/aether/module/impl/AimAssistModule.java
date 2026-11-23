package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AimAssistModule extends Module {
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 4.0, 3.0, 6.0, 0.1F));
    private final SliderSetting fov = this.addSetting(new SliderSetting("FOV", "FOV", 60.0, 10.0, 180.0, 5.0F));
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 60.0, 10.0, 180.0, 5.0F));
    private final BooleanSetting weaponsOnly = this.addSetting(new BooleanSetting("Weapons Only", "Weapons Only", true));
    private final BooleanSetting vertical = this.addSetting(new BooleanSetting("Vertical", "Vertical", true));

    public AimAssistModule() {
        super("aim-assist", "Eases your crosshair onto the nearest enemy player while you move the mouse.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Aim assist logic stub - core crosshair aiming functionality
        // preserved as module registration; detailed aiming logic disabled
    }

    public float getRange() {
        return this.range.getFloat();
    }

    public float getFov() {
        return this.fov.getFloat();
    }

    public float getSpeed() {
        return this.speed.getFloat();
    }
}
