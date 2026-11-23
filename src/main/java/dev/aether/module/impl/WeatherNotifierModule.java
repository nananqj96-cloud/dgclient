package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class WeatherNotifierModule extends Module {
    private final BooleanSetting sound = this.addSetting(new BooleanSetting("Sound", "Sound", true));
    private boolean wasRaining;
    private boolean notified;

    public WeatherNotifierModule() {
        super("weather-notifier", "Shows an advancement-style toast when it starts raining.", Category.MISC);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        boolean raining = mc.level.isRaining();
        if (raining && !this.wasRaining && !this.notified) {
            this.notified = true;
        }
        if (!raining) {
            this.notified = false;
        }
        this.wasRaining = raining;
    }
}
