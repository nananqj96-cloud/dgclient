package dev.aether.module.impl;

import java.util.Locale;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

public class AutoReconnectModule extends Module {
    private static final long HOLD_MS = 15000L;
    private static volatile long holdUntil;
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 5.0, 3.0, 60.0, 1.0F));
    private ServerData lastServer;
    private Screen activeScreen;
    private Button reconnectButton;
    private int ticksLeft;
    private boolean heldScreen;

    public static void holdOff() {
        holdUntil = Util.getMillis() + 15000L;
    }

    public AutoReconnectModule() {
        super("auto-reconnect", "Reconnects to the last server after an unexpected disconnect.", Category.MISC);
        TickEvent.TYPE.listen(this, this::trackServer);
        ScreenEvents.AFTER_INIT.register(this::onScreenInit);
    }

    private void trackServer(TickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            ServerData server = mc.isLocalServer() ? null : mc.getCurrentServer();
            this.lastServer = server != null && !server.isLan() && !server.isRealm() ? server : null;
        }
    }

    private void onScreenInit(Minecraft client, Screen screen, int scaledWidth, int scaledHeight) {
        if (screen instanceof TitleScreen || screen instanceof JoinMultiplayerScreen) {
            this.lastServer = null;
        } else if (screen instanceof DisconnectedScreen && this.lastServer != null) {
            ServerData server = this.lastServer;
            if (screen == this.activeScreen) {
                Screens.getButtons(screen).remove(this.reconnectButton);
            } else {
                this.activeScreen = screen;
                this.ticksLeft = Math.round(this.delay.get() * 20.0F);
                this.heldScreen = Util.getMillis() < holdUntil || isBan(screen);
                holdUntil = 0L;
            }

            boolean auto = this.isEnabled() && !this.heldScreen;
            Button button = Button.builder(reconnectLabel(auto, this.ticksLeft), btn -> {
                btn.active = false;
                this.reconnect(client, server);
            }).bounds(scaledWidth - 150 - 10, scaledHeight - 20 - 10, 150, 20).build();
            this.reconnectButton = button;
            Screens.getButtons(screen).add(button);
            if (auto) {
                ScreenEvents.afterTick(screen).register(s -> {
                    this.ticksLeft--;
                    if (this.ticksLeft <= 0) {
                        button.active = false;
                        this.reconnect(client, server);
                    } else {
                        button.setMessage(reconnectLabel(true, this.ticksLeft));
                    }
                });
            }
        }
    }

    private static boolean isBan(Screen screen) {
        String reason = screen.getNarrationMessage().getString().toLowerCase(Locale.ROOT);
        return reason.contains("banned") || reason.contains("blacklisted");
    }

    private void reconnect(Minecraft client, ServerData server) {
        ServerAddress address = ServerAddress.parseString(server.ip);
        ConnectScreen.startConnecting(new JoinMultiplayerScreen(new TitleScreen()), client, address, server, false, null);
    }

    private static Component reconnectLabel(boolean auto, int ticksLeft) {
        return auto ? Component.literal(String.format(Locale.ROOT, "Reconnect (%.1f)", ticksLeft / 20.0)) : Component.literal("Reconnect");
    }
}
