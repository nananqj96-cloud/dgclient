package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;

public class PlayerBypassModule extends Module {
    private final SliderSetting opacity = this.addSetting(new SliderSetting("Opacity", "Opacity", 140.0, 10.0, 255.0, 1.0F));
    private final BooleanSetting tracers = this.addSetting(new BooleanSetting("Tracers", "Tracers", false));
    private final SliderSetting sensitivity = this.addSetting(new SliderSetting("Sensitivity", "Sensitivity", 5.0, 1.0, 10.0, 1.0F));
    private final BooleanSetting sound = this.addSetting(new BooleanSetting("Sound", "Sound", true));
    private final BooleanSetting showCoordinates = this.addSetting(new BooleanSetting("Show Coordinates", "Show Coordinates", true));
    private final BooleanSetting disconnect = this.addSetting(new BooleanSetting("Auto Disconnect", "Auto Disconnect", false));
    private final BooleanSetting autoRtp = this.addSetting(new BooleanSetting("Auto RTP", "Auto RTP", false));
    private final SliderSetting rtpScanTime = this.addSetting(new SliderSetting("Scan Time (s)", "Scan Time (s)", 5.0, 2.0, 30.0, 0.5F));
    private final SliderSetting rtpDelay = this.addSetting(new SliderSetting("RTP Delay (s)", "RTP Delay (s)", 6.0, 3.0, 120.0, 0.5F));
    private final SliderSetting rtpTimeout = this.addSetting(new SliderSetting("RTP Timeout (s)", "RTP Timeout (s)", 10.0, 3.0, 60.0, 1.0F));

    public PlayerBypassModule() {
        super("player-bypass", "Finds hidden players from light data and from chunks that keep ticking outside your simulation distance.", Category.EXPLOIT);
    }

    @Override
    public void onTick() {
    }
}
