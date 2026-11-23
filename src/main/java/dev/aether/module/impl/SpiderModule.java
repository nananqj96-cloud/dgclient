package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpiderModule extends Module {
    private final SliderSetting climbSpeed = this.addSetting(new SliderSetting("Climb Speed", "Climb Speed", 0.2, 0.0, 10.0, 0.1F));

    public SpiderModule() {
        super("spider", "Allows you to climb walls like a spider.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Spider climbing logic stub - core wall-climb functionality
        // preserved as module registration; detailed movement logic disabled
    }

    public float getClimbSpeed() {
        return this.climbSpeed.getFloat();
    }
}
