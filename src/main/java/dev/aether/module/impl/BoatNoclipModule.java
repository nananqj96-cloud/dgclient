package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class BoatNoclipModule extends Module {
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 10.0, 0.0, 320.0, 0.5F));
    private final SliderSetting speedInsideBlocks = this.addSetting(new SliderSetting("Speed Inside Blocks", "Speed Inside Blocks", 10.0, 0.0, 320.0, 0.5F));
    private final SliderSetting verticalSpeed = this.addSetting(new SliderSetting("Vertical Speed", "Vertical Speed", 6.0, 0.0, 320.0, 0.5F));
    private final SliderSetting fallSpeed = this.addSetting(new SliderSetting("Fall Speed", "Fall Speed", 0.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting lavaProtection = this.addSetting(new BooleanSetting("Lava Protection", "Lava Protection", true));
    private final BooleanSetting antiKick = this.addSetting(new BooleanSetting("Anti Fly Kick", "Anti Fly Kick", true));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Delay", 40.0, 1.0, 80.0, 1.0F));

    public BoatNoclipModule() {
        super("boat-noclip", "Fly your boat through blocks and entities.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Boat noclip logic stub - core boat through-blocks functionality
        // preserved as module registration; detailed movement logic disabled
    }

    public float getSpeed() {
        return this.speed.getFloat();
    }

    public float getVerticalSpeed() {
        return this.verticalSpeed.getFloat();
    }

    public float getFallSpeed() {
        return this.fallSpeed.getFloat();
    }
}
