package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoSellModule extends Module {

    public AutoSellModule() {
        super("auto-sell", "Keeps the /sell menu open and keeps selling everything that is not on your Never Sell list.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // AutoSellModule logic stub - core functionality
        // preserved as module registration; detailed logic disabled
    }
}
