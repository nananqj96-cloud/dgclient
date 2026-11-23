package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TimeChangerModule extends Module {
    private final SliderSetting time = this.addSetting(new SliderSetting("Time", "Time", 0.0, -20000.0, 20000.0, 1.0F));

    public TimeChangerModule() {
        super("time-changer", "Makes you able to set a custom time.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Time changer logic stub - core time setting functionality
        // preserved as module registration; detailed logic disabled
    }

    public float getTime() {
        return this.time.getFloat();
    }
}
