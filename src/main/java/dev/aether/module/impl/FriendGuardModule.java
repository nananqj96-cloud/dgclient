package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class FriendGuardModule extends Module {

    public FriendGuardModule() {
        super("friend-guard", "Stops you from hitting players on your friends list.", Category.COMBAT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // FriendGuardModule logic stub - core functionality
        // preserved as module registration; detailed logic disabled
    }
}
