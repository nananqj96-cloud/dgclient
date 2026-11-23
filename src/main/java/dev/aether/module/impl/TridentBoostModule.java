package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

public class TridentBoostModule extends Module {
    public static volatile TridentBoostModule active;
    private final SliderSetting boost = this.addSetting(new SliderSetting("Boost", 2.0, 0.1, 10.0, 0.1F));
    private final BooleanSetting outOfWater = this.addSetting(new BooleanSetting("Out Of Water", true));

    public TridentBoostModule() {
        super("trident-boost", "Boosts you when using riptide with a trident.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

    public static double riptideMultiplier() {
        TridentBoostModule module = active;
        return module != null ? module.boost.get() : 1.0;
    }

    public static boolean allowOutOfWater() {
        TridentBoostModule module = active;
        return module != null && module.outOfWater.get();
    }
}
