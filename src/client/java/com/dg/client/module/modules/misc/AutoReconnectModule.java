package com.dg.client.module.modules.misc;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.DoubleSetting;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Auto-Reconnect: caches last connected ServerInfo and injects an interactive
 * countdown toggle button onto Minecraft's DisconnectedScreen.
 */
public final class AutoReconnectModule extends Module {
    public final DoubleSetting delaySeconds = addSetting(new DoubleSetting(
            "Delay (sec)", "Seconds before reconnecting automatically", 5.0, 1.0, 60.0, 0.5));

    private static boolean screenHookRegistered = false;
    private ServerInfo lastServerInfo = null;
    private long reconnectDeadlineMs = 0L;
    private ButtonWidget toggleButton = null;

    public AutoReconnectModule() {
        super("AutoReconnect", "Automatic server reconnect with countdown button on Disconnect screen", ModuleCategory.MISC);
        registerDisconnectScreenHook();
    }

    @Override
    public String getDisplayInfo() {
        return String.format(Locale.ROOT, "%.1fs", delaySeconds.getValue());
    }

    private void registerDisconnectScreenHook() {
        if (screenHookRegistered) {
            return;
        }
        screenHookRegistered = true;
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof DisconnectedScreen)) {
                return;
            }
            AutoReconnectModule mod = DGClient.getInstance().getModuleManager().get(AutoReconnectModule.class);
            if (mod == null || mod.lastServerInfo == null) {
                return;
            }
            mod.reconnectDeadlineMs = System.currentTimeMillis() + Math.round(mod.delaySeconds.getValue() * 1000.0);
            ButtonWidget btn = ButtonWidget.builder(mod.buildButtonLabel(), b -> {
                        mod.toggle();
                        mod.reconnectDeadlineMs = System.currentTimeMillis() + Math.round(mod.delaySeconds.getValue() * 1000.0);
                        b.setMessage(mod.buildButtonLabel());
                    })
                    .dimensions(scaledWidth / 2 - 100, Math.min(scaledHeight - 28, scaledHeight / 2 + 72), 200, 20)
                    .build();
            mod.toggleButton = btn;
            Screens.getButtons(screen).add(btn);

            ScreenEvents.afterTick(screen).register(scr -> {
                if (mod.toggleButton != null) {
                    mod.toggleButton.setMessage(mod.buildButtonLabel());
                }
                if (mod.isEnabled() && mod.lastServerInfo != null && System.currentTimeMillis() >= mod.reconnectDeadlineMs) {
                    mod.connectNow();
                }
            });
        });
    }

    private Text buildButtonLabel() {
        if (!isEnabled()) {
            return Text.literal("DG Auto-Reconnect: OFF");
        }
        double rem = Math.max(0.0, (reconnectDeadlineMs - System.currentTimeMillis()) / 1000.0);
        return Text.literal(String.format(Locale.ROOT, "DG Auto-Reconnect: ON (%.1fs)", rem));
    }

    public void connectNow() {
        if (lastServerInfo == null) {
            return;
        }
        ServerInfo target = lastServerInfo;
        ConnectScreen.connect(
                new MultiplayerScreen(new TitleScreen()),
                mc,
                ServerAddress.parse(target.address),
                target,
                false,
                null
        );
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        ServerInfo cur = mc.getCurrentServerEntry();
        if (cur != null) {
            lastServerInfo = cur;
        }
    }
}
