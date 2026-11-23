package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class BlockNotifierModule extends Module {
    private final BooleanSetting sound = this.addSetting(new BooleanSetting("Sound", true));

    public BlockNotifierModule() {
        super("block-notifier", "Shows a toast with the block and its coordinates when one of your chosen blocks shows up.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Block notifier logic stub - core block detection/notification functionality
        // preserved as module registration; detailed logic disabled
    }

    public boolean getSound() {
        return this.sound.get();
    }
}
