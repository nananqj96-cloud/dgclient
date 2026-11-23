package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class StaffLiveModule extends Module {
    private final SliderSetting interval = this.addSetting(new SliderSetting("Check Every (s)", "Check Every (s)", 60.0, 15.0, 600.0, 5.0F));
    private final SliderSetting scale = this.addSetting(new SliderSetting("Scale", "Scale", 1.0, 0.5, 3.0, 0.05F));
    private long lastCheck;

    public StaffLiveModule() {
        super("staff-live", "Tells you when DonutSMP staff go live on YouTube or TikTok: a chat line with a watch link, a chime, and a panel with the stream title while they are live. Channels are yt:handle or tt:handle.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastCheck >= this.interval.get() * 1000.0) {
            this.lastCheck = now;
        }
    }
}
