package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

public class TridentUtilModule extends Module {
    private final SliderSetting chargeScale = this.addSetting(new SliderSetting("Charge Scale", "Charge Scale", 0.0, 0.0, 1.0, 0.1F));
    private final BooleanSetting noWater = this.addSetting(new BooleanSetting("No Water", "No Water", true));

    public TridentUtilModule() {
        super("trident-util", "Use tridents out of water and charge them faster.", Category.MOVEMENT);
    }

    private static TridentUtilModule active() {
//         TridentUtilModule module = ModuleRegistry.get(TridentUtilModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    public static boolean allowOutOfWater() {
        TridentUtilModule module = active();
        return module != null && module.noWater.get();
    }

    public static int minChargeTicks(int vanilla) {
        TridentUtilModule module = active();
        return module == null ? vanilla : Math.max(1, Math.round(vanilla * module.chargeScale.get()));
    }
}
