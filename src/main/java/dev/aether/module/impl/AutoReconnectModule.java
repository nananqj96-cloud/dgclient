package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class AutoReconnectModule extends Module {
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 5.0, 3.0, 60.0, 1.0F));

    public AutoReconnectModule() {
        super("auto-reconnect", "Reconnects to the last server after an unexpected disconnect.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        // Auto reconnect logic stub - core reconnection functionality
        // preserved as module registration; detailed reconnect logic disabled
    }

    public float getDelay() {
        return this.delay.getFloat();
    }
}
