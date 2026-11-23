package dev.aether.module.impl;

import java.util.concurrent.ThreadLocalRandom;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;

public class NoJumpDelayModule extends Module {
    public static volatile NoJumpDelayModule active;
    private final SliderSetting cooldown = this.addSetting(new SliderSetting("Cooldown", 0.0, 0.0, 9.0, 1.0F));
    private final SliderSetting variance = this.addSetting(new SliderSetting("Variance", 0.0, 0.0, 4.0, 1.0F));

    public NoJumpDelayModule() {
        super("no-jump-delay", "Removes the delay between jumps.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public int rearm(int vanillaTicks) {
        int ticks = this.cooldown.getInt();
        int spread = this.variance.getInt();
        if (spread > 0) {
            ticks += ThreadLocalRandom.current().nextInt(spread + 1);
        }

        return Math.min(vanillaTicks, ticks);
    }
}
