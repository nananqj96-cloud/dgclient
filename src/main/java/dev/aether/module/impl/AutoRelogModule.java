package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRelogModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
    private static final int REARM_HEIGHT = 4;
    private static final int REARM_TICKS = 40;
    private final SliderSetting yLevel = this.addSetting(new SliderSetting("Y Level", "Y Level", 0.0, -60.0, 16.0, 1.0F));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Delay", 3.0, 1.0, 30.0, 0.5F));
    private ClientLevel seenLevel;
    private LocalPlayer seenPlayer;
    private int aboveTicks;
    private ServerData rejoin;
    private boolean disconnected;
    private int rejoinTicks;

    public AutoRelogModule() {
        super("auto-relog", "Relogs once when you go down to deepslate level.", Category.DONUT);
    }

    @Override
    protected void onEnable() {
        this.forget();
    }

    @Override
    protected void onDisable() {
        this.forget();
    }

    private void forget() {
        this.seenLevel = null;
        this.seenPlayer = null;
        this.aboveTicks = 0;
        this.rejoin = null;
        this.disconnected = false;
    }

    private void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (this.rejoin != null) {
            this.waitToRejoin(mc);
        } else {
            LocalPlayer player = mc.player;
            ClientLevel level = mc.level;
            if (player != null && level != null && !(mc.screen instanceof LevelLoadingScreen)) {
                if (level != this.seenLevel || player != this.seenPlayer) {
                    this.seenLevel = level;
                    this.seenPlayer = player;
                    this.aboveTicks = 0;
                }

                if (!LevelUtil.isOverworld(level)) {
                    this.aboveTicks = 0;
                } else {
                    double y = player.getY();
                    float line = this.yLevel.get().floatValue();
                    if (y >= line + 4.0F) {
                        if (this.aboveTicks < 40) {
                            this.aboveTicks++;
                        }
                    } else if (y < line) {
                        boolean armed = this.aboveTicks >= 40;
                        this.aboveTicks = 0;
                        if (armed) {
                            this.relog(mc, y);
                        }
                    }
                }
            } else {
                this.aboveTicks = 0;
            }
        }
    }

    private void relog(Minecraft mc, double y) {
        ServerData server = mc.isLocalServer() ? null : mc.getCurrentServer();
        if (server != null && !server.isRealm()) {
            LOGGER.info("[Anubis] Auto Relog: reached Y {}, relogging", (int)Math.floor(y));
            this.rejoin = server;
            this.disconnected = false;
            this.rejoinTicks = Math.round(this.delay.get() * 20.0F);
            mc.schedule(() -> {
                if (this.rejoin == server && mc.level != null) {
                    mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE);
                    this.disconnected = true;
                }
            });
        }
    }

    private void waitToRejoin(Minecraft mc) {
        if (this.disconnected) {
            if (mc.level == null && mc.screen instanceof JoinMultiplayerScreen) {
                if (--this.rejoinTicks <= 0) {
                    ServerData server = this.rejoin;
                    Screen serverList = mc.screen;
                    this.rejoin = null;
                    mc.schedule(() -> {
                        if (mc.screen == serverList && mc.level == null) {
                            ConnectScreen.startConnecting(serverList, mc, ServerAddress.parseString(server.ip), server, false, null);
                        }
                    });
                }
            } else {
                this.rejoin = null;
            }
        }
    }
}
