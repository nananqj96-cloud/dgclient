package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class BoatFlyModule extends Module {
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", 10.0, 0.0, 50.0, 0.5F));
    private final SliderSetting verticalSpeed = this.addSetting(new SliderSetting("Vertical Speed", 6.0, 0.0, 20.0, 0.5F));
    private final SliderSetting fallSpeed = this.addSetting(new SliderSetting("Fall Speed", 0.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting antiKick = this.addSetting(new BooleanSetting("Anti Fly Kick", true));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 40.0, 1.0, 80.0, 1.0F));
    private final BooleanSetting cancelServerPackets = this.addSetting(new BooleanSetting("Cancel Server Packets", false));

    public BoatFlyModule() {
        super("boat-fly", "Transforms your boat (or any ridden vehicle) into a plane.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Boat fly logic stub - core vehicle flying functionality
        // preserved as module registration; detailed flight logic disabled
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
