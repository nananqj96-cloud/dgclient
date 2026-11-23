package dev.aether.module.impl;

import dev.aether.AetherClient;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

public class TpMacroModule extends Module {
    private static final Pattern RATE_LIMIT = Pattern.compile("^you need to wait another ([\\d.]+) seconds? to execute a command");
    private static final Pattern VALID_NAME = Pattern.compile("\\.?[A-Za-z0-9_]{1,16}");
    private final ModeSetting command = this.addSetting(new ModeSetting("Command", "tpa", "tpa", "tpahere"));
    private final SliderSetting interval = this.addSetting(new SliderSetting("Interval (s)", 1.0, 0.25, 60.0, 0.25F));
    private final TeleportConfirm confirm = new TeleportConfirm(this);
    private volatile long holdUntil;
    private long nextAt;
    private int sent;

    public TpMacroModule() {
        super("tp-macro", "Sends /tpa or /tpahere to one player on a timer.", Category.DONUT);
        this.usesNotifications();
        this.target.privateValue();
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        String name = this.target.get().trim();
        if (!VALID_NAME.matcher(name).matches()) {
//             AetherClient.chat(this, ModuleChat.body().warn("Set a valid Player name first."));
            this.requestDisable();
        } else {
            this.confirm.reset();
            this.holdUntil = 0L;
            this.nextAt = 0L;
            this.sent = 0;
            AetherClient.chat(
                this,
//                 ModuleChat.body()
                    .text("Sending ")
                    .value("/" + this.command.get() + " " + name)
                    .text(" every ")
                    .value(this.interval.formatted() + "s")
                    .text(".")
            );
        }
    }

    @Override
    protected void onDisable() {
        if (this.sent > 0) {
//             AetherClient.chat(this, ModuleChat.body().text("Sent ").value(this.sent).text(this.sent == 1 ? " request." : " requests."));
        }
    }

//     @EventHandler
//     public void onPacket(PacketReceiveNettyEvent event) {
        // if (event.packet instanceof ClientboundSystemChatPacket chat && chat.content() != null) {
            // Matcher wait = RATE_LIMIT.matcher(ServerText.normalize(chat.content().getString()));
            // if (wait.find()) {
                // try {
                    // this.holdUntil = System.currentTimeMillis() + (long)Math.ceil(Math.min(30.0, Double.parseDouble(wait.group(1))) * 1000.0) + 80L;
                // } catch (NumberFormatException ignored) {
                // }
            // }
        // }
    // }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player != null && mc.getConnection() != null && !player.isDeadOrDying()) {
            this.confirm.tick(mc);
            long now = System.currentTimeMillis();
            if (now >= this.nextAt && now >= this.holdUntil) {
                player.connection.sendCommand(this.command.get() + " " + this.target.get().trim());
                this.confirm.sent();
                this.sent++;
                this.nextAt = now + (long)(this.interval.get() * 1000.0F);
            }
        }
    }
}
