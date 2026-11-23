package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SpeedModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Vanilla", "Strafe", "Vanilla"));
    private final SliderSetting vanillaSpeed = this.addSetting(new SliderSetting("Vanilla Speed", "Vanilla Speed", 5.6, 0.0, 20.0, 0.1F));
    private final SliderSetting strafeSpeed = this.addSetting(new SliderSetting("Strafe Speed", "Strafe Speed", 1.6, 0.0, 3.0, 0.1F));
    private final BooleanSetting speedLimit = this.addSetting(new BooleanSetting("Speed Limit", "Speed Limit", false));
    private final SliderSetting timer = this.addSetting(new SliderSetting("Timer", "Timer", 1.0, 0.01, 10.0, 0.01F));
    private final BooleanSetting inLiquids = this.addSetting(new BooleanSetting("In Liquids", "In Liquids", false));
    private final BooleanSetting whenSneaking = this.addSetting(new BooleanSetting("When Sneaking", "When Sneaking", false));
    private final BooleanSetting onlyOnGround = this.addSetting(new BooleanSetting("Only On Ground", "Only On Ground", false));

    public SpeedModule() {
        super("speed", "Modifies your movement speed when moving on the ground.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Speed logic stub - core speed modification functionality
        // preserved as module registration; detailed movement logic disabled
    }

    public String getMode() {
        return this.mode.get();
    }

    public float getVanillaSpeed() {
        return this.vanillaSpeed.getFloat();
    }

    public float getStrafeSpeed() {
        return this.strafeSpeed.getFloat();
    }

    public float getTimer() {
        return this.timer.getFloat();
    }
}
