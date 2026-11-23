package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;

public class PlayerDetectionModule extends Module {
    private final BooleanSetting enableDisconnect = this.addSetting(new BooleanSetting("Disconnect", true));
    private final ModeSetting notificationMode = this.addSetting(new ModeSetting("Notification Mode", "Both", "Chat", "Toast", "Both"));
    private final BooleanSetting toggleOnPlayer = this.addSetting(new BooleanSetting("Toggle On Detect", true));
    private final BooleanSetting enableWebhook = this.addSetting(new BooleanSetting("Webhook", false));
    private final BooleanSetting selfPing = this.addSetting(new BooleanSetting("Self Ping", false));
    private final BooleanSetting enablePanicPay = this.addSetting(new BooleanSetting("Enable Panic Pay", false));

    public PlayerDetectionModule() {
        super("player-detection", "Detects when players are in the world.", Category.DONUT);
    }

    @Override
    public void onTick() {
    }
}
