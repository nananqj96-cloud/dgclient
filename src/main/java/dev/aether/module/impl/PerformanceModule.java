package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

public class PerformanceModule extends Module {
    private final BooleanSetting optimize = this.addSetting(new BooleanSetting("Optimize Client", "Optimize Client", true));
    private final BooleanSetting showStats = this.addSetting(new BooleanSetting("Show Stats", "Show Stats", true));
    private final BooleanSetting frustumCulling = this.addSetting(new BooleanSetting("Frustum Culling", "Frustum Culling", true));
    private final SliderSetting scanBudget = this.addSetting(new SliderSetting("Scan Budget ms", "Scan Budget ms", 2.0, 0.5, 8.0, 0.5F));
    private long smoothedNanos;
    private long nextStatsUpdate;
    private String statsText = "";

    public PerformanceModule() {
        super("performance", "Reduces background work and image memory while keeping effects and features. Disable to compare FPS.", Category.CLIENT);
    }

    @Override
    public void onTick() {
        long now = System.nanoTime();
        if (this.showStats.get() && now >= this.nextStatsUpdate) {
            this.smoothedNanos = now - this.lastUpdate;
            this.nextStatsUpdate = now + 250000000L;
            this.statsText = "Perf: " + (this.optimize.get() ? "Optimized" : "Standard");
        }
        this.lastUpdate = now;
    }

    private long lastUpdate = System.nanoTime();

    public String getStatsText() {
        return this.statsText;
    }

    public boolean isOptimizing() {
        return this.optimize.get();
    }
}
