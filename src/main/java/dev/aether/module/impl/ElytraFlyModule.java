package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ElytraFlyModule extends Module {
    public final ModeSetting flightMode = this.addSetting(new ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Pitch40", "Bounce"));
    public final BooleanSetting autoTakeOff = this.addSetting(new BooleanSetting("Auto Take Off", false));
    public final SliderSetting fallMultiplier = this.addSetting(new SliderSetting("Fall Multiplier", 0.01, 0.0, 2.0, 0.01F));
    public final SliderSetting horizontalSpeed = this.addSetting(new SliderSetting("Horizontal Speed", 1.0, 0.0, 10.0, 0.1F));
    public final SliderSetting verticalSpeed = this.addSetting(new SliderSetting("Vertical Speed", 1.0, 0.0, 10.0, 0.1F));
    public final BooleanSetting acceleration = this.addSetting(new BooleanSetting("Acceleration", false));
    public final SliderSetting accelerationStep = this.addSetting(new SliderSetting("Acceleration Step", 1.0, 0.1, 5.0, 0.1F));
    public final SliderSetting accelerationMin = this.addSetting(new SliderSetting("Acceleration Start", 0.0, 0.0, 10.0, 0.1F));
    public final BooleanSetting stopInWater = this.addSetting(new BooleanSetting("Stop In Water", true));
    public final BooleanSetting dontGoIntoUnloadedChunks = this.addSetting(new BooleanSetting("No Unloaded Chunks", true));

    public ElytraFlyModule() {
        super("elytra-fly", "Gives you more control over your elytra.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Elytra fly logic stub - core flight control functionality
        // preserved as module registration; detailed flight logic disabled
    }

    public String getFlightMode() {
        return this.flightMode.get();
    }

    public float getFallMultiplier() {
        return this.fallMultiplier.getFloat();
    }

    public float getHorizontalSpeed() {
        return this.horizontalSpeed.getFloat();
    }

    public float getVerticalSpeed() {
        return this.verticalSpeed.getFloat();
    }
}
