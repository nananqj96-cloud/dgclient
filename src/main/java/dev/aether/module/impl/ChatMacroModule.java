package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ChatMacroModule extends Module {
    private final ModeSetting slots = this.addSetting(new ModeSetting("Slots", "3", "1", "2", "3", "4", "5"));
    private final ModeSetting pressAction = this.addSetting(new ModeSetting("On Press", "Send", "Send", "Open in Chat"));
    private final SliderSetting cooldown = this.addSetting(new SliderSetting("Cooldown (s)", 1.0, 1.0, 10.0, 0.5F));
    private final KeybindSetting[] keys = new KeybindSetting[5];
    private final String[] texts = new String[5];
    private final long[] lastSent = new long[5];

    public ChatMacroModule() {
        super("chat-macro", "Sends chat messages or commands with a key press.", Category.MISC);
        int count = Integer.parseInt(this.slots.get());
        for (int i = 0; i < count && i < 5; i++) {
            this.keys[i] = this.addSetting(new KeybindSetting("Macro " + (i + 1) + " Key", -1));
            this.texts[i] = "";
        }
    }

    @Override
    public boolean onKeyPress(int keyCode) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return false;
        }
        int count = Integer.parseInt(this.slots.get());
        for (int i = 0; i < count && i < 5; i++) {
            if (this.keys[i] != null && this.keys[i].matches(keyCode)) {
                long now = System.currentTimeMillis();
                if (now - this.lastSent[i] < this.cooldown.get() * 1000.0) {
                    return true;
                }
                this.lastSent[i] = now;
                if (this.pressAction.is("Send") && this.texts[i] != null && !this.texts[i].isEmpty()) {
                    player.connection.sendChat(this.texts[i]);
                }
                return true;
            }
        }
        return false;
    }

    public void setMacroText(int slot, String text) {
        if (slot >= 0 && slot < 5) {
            this.texts[slot] = text;
        }
    }
}
