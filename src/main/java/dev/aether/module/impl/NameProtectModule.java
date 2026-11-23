package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class NameProtectModule extends Module {
    private final ModeSetting others = this.addSetting(new ModeSetting("Others", "Keep", "Keep", "Same Label", "Numbered"));
    private final ModeSetting othersLabel = this.addSetting(new ModeSetting("Others Label", "Player", "Player", "Anonymous", "Unknown"));

    public NameProtectModule() {
        super("name-protect", "Shows aliases instead of player names on your screen.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Name protect logic stub - core name replacement functionality
        // preserved as module registration; detailed name matching disabled
    }

    public String getOthers() {
        return this.others.get();
    }

    public String getOthersLabel() {
        return this.othersLabel.get();
    }
}
