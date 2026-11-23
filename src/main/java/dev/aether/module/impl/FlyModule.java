package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FlyModule extends Module {
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Abilities", "Abilities", "Velocity"));
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 0.1, 0.0, 10.0, 0.05F));
    private final BooleanSetting verticalSpeedMatch = this.addSetting(new BooleanSetting("Vertical Speed Match", "Vertical Speed Match", false));
    private final BooleanSetting noSneak = this.addSetting(new BooleanSetting("No Sneak", "No Sneak", false));
    private final ModeSetting antiKickMode = this.addSetting(new ModeSetting("Anti Kick Mode", "Anti Kick Mode", "Packet", "Normal", "Packet", "None"));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Anti Kick Delay", "Anti Kick Delay", 20.0, 1.0, 200.0, 1.0F));
    private final SliderSetting offTime = this.addSetting(new SliderSetting("Anti Kick Off Time", "Anti Kick Off Time", 1.0, 1.0, 20.0, 1.0F));

    public FlyModule() {
        super("fly", "Lets you fly.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Fly logic stub - core flight functionality
        // preserved as module registration; detailed flight logic disabled
    }

    public String getMode() {
        return this.mode.get();
    }

    public float getSpeed() {
        return this.speed.getFloat();
    }

    public String getAntiKickMode() {
        return this.antiKickMode.get();
    }
}
