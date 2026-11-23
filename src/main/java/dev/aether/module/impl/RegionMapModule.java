package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;

public class RegionMapModule extends Module {
    private final SliderSetting squareSize = this.addSetting(new SliderSetting("Cell Size", 6.0, 3.0, 16.0, 0.5F));
    private final SliderSetting playerScale = this.addSetting(new SliderSetting("Player Scale", 1.0, 0.5, 3.0, 0.1F));
    private final SliderSetting textScale = this.addSetting(new SliderSetting("Text Scale", 1.0, 0.1, 2.0, 0.1F));
    private final SliderSetting opacity = this.addSetting(new SliderSetting("Opacity", 1.0, 0.1, 1.0, 0.05F));
    private final BooleanSetting shadow = this.addSetting(new BooleanSetting("Shadow", true));
    private final BooleanSetting showRegionIds = this.addSetting(new BooleanSetting("Show Goliath Numbers", true));
    private final BooleanSetting showLegend = this.addSetting(new BooleanSetting("Show Legend", true));
    private final BooleanSetting showCoordinates = this.addSetting(new BooleanSetting("Show Coordinates", true));
    private final ColorSetting playerIndicatorColor = this.addSetting(new ColorSetting("Player Indicator", -52686));

    public RegionMapModule() {
        super("region-map", "DonutSMP goliath map with directional indicator.", Category.CLIENT);
    }

    public float squareSize() {
        return this.squareSize.get();
    }

    public float playerScale() {
        return this.playerScale.get();
    }

    public float textScale() {
        return this.textScale.get();
    }

    public float opacity() {
        return this.opacity.get();
    }

    public boolean shadow() {
        return this.shadow.get();
    }

    public boolean showRegionIds() {
        return this.showRegionIds.get();
    }

    public boolean showLegend() {
        return this.showLegend.get();
    }

    public boolean showCoordinates() {
        return this.showCoordinates.get();
    }

    public ColorSetting playerIndicatorColor() {
        return this.playerIndicatorColor;
    }
}
