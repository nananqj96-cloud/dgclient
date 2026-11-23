package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;

public class FullbrightModule extends Module {
    private final SliderSetting gamma = this.addSetting(new SliderSetting("Gamma", 8.0, 1.0, 16.0, 0.5F));

    public FullbrightModule() {
        super("fullbright", "Brighten dark areas by overriding the client lightmap gamma.", Category.RENDER);
    }

    public float gamma() {
        return Math.max(1.0F, this.gamma.get());
    }
}
