package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class TpMacroModule extends Module {
    private final ModeSetting command = this.addSetting(new ModeSetting("Command", "tpa", "tpa", "tpahere"));
    private final SliderSetting interval = this.addSetting(new SliderSetting("Interval (s)", 1.0, 0.25, 60.0, 0.25F));
    private String target;
    private long lastSent;

    public TpMacroModule() {
        super("tp-macro", "Sends /tpa or /tpahere to one player on a timer.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.connection == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.target != null && now - this.lastSent >= this.interval.get() * 1000.0) {
            player.connection.sendChat("/" + this.command.get() + " " + this.target);
            this.lastSent = now;
        }
    }

    public void setTarget(String name) {
        this.target = name;
        this.lastSent = 0L;
        AetherClient.chat(this, "Sending /" + this.command.get() + " " + name + " every " + this.interval.get() + "s.");
    }

    public void clearTarget() {
        this.target = null;
    }
}
