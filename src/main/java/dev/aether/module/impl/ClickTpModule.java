package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ClickTpModule extends Module {

    public ClickTpModule() {
        super("click-tp", "Teleports you to the block you click on.", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Click TP logic stub - core teleport-to-clicked-block functionality
        // preserved as module registration; detailed logic disabled
    }
}
