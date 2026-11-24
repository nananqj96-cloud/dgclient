package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class DiscordPresenceModule extends Module {
    private final BooleanSetting showServerIp = this.addSetting(new BooleanSetting("Show Server IP", "Show Server IP", true));
    private final BooleanSetting discordButton = this.addSetting(new BooleanSetting("Discord Button", "Discord Button", true));

    public DiscordPresenceModule() {
        super("discord-presence", "Shows Anubis Client and the server you are on in your Discord status.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        // Discord presence logic stub - core Discord status functionality
        // preserved as module registration; detailed IPC logic disabled
    }

    public boolean getShowServerIp() {
        return this.showServerIp.get();
    }

    public boolean getDiscordButton() {
        return this.discordButton.get();
    }
}
