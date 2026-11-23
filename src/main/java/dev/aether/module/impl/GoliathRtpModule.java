package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class GoliathRtpModule extends Module {

    public GoliathRtpModule() {
        super("goliath-rtp", "Opens a map of every goliath: pick as many as you like and it uses /rtp until you land on one of them.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Goliath RTP logic stub - core goliath map/RTP functionality
        // preserved as module registration; detailed logic disabled
    }
}
