package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;

public class SlowSwingModule extends Module {
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 0.25, 0.1, 2.0, 0.05F));

    public SlowSwingModule() {
        super("slow-swing", "Control hand swing animation speed.", Category.MISC);
    }

    public int getModifiedSwingDuration(int vanilla) {
        float s = this.speed.get().floatValue();
        if (s <= 0.0F) {
            s = 0.1F;
        }

        int modified = Math.round(vanilla / s);
        return Math.max(1, modified);
    }
}
