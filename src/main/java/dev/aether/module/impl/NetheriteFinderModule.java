package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class NetheriteFinderModule extends Module {
    private final ColorSetting color = this.addSetting(new ColorSetting("Near Color", "Near Color", -34304));
    private final ColorSetting farColor = this.addSetting(new ColorSetting("Far Color", "Far Color", -11688961));
    private final SliderSetting fade = this.addSetting(new SliderSetting("Fade Distance", "Fade Distance", 192.0, 32.0, 512.0, 8.0F));
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", "Range", 512.0, 64.0, 1024.0, 1.0F));
    private final BooleanSetting tracers = this.addSetting(new BooleanSetting("Chunk Tracers", "Chunk Tracers", true));
    private final SliderSetting tracerCount = this.addSetting(new SliderSetting("Tracer Count", "Tracer Count", 3.0, 1.0, 8.0, 1.0F));
    private final BooleanSetting advanced = this.addSetting(new BooleanSetting("Advanced Finder", "Advanced Finder", false));
    private final BooleanSetting showProbes = this.addSetting(new BooleanSetting("Show Probes", "Show Probes", true));
    private final ModeSetting probeStyle = this.addSetting(new ModeSetting("Probe Style", "Probe Style", "Dots", "Dots", "Cubes", "Pulses"));
    private final SliderSetting missingFrom = this.addSetting(new SliderSetting("Mark Missing From %", "Mark Missing From %", 60.0, 30.0, 100.0, 5.0F));
    private int pendingCount;
    private long lastNotifyAt;

    public NetheriteFinderModule() {
        super("netherite-finder", "Boxes Nether chunk sections hiding ancient debris.", Category.DONUT);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (this.pendingCount > 0) {
            long now = System.currentTimeMillis();
            if (now - this.lastNotifyAt >= 2000L) {
                AetherClient.notifications().pushInfo(this.pendingCount + " new hidden-debris chunk" + (this.pendingCount == 1 ? " " : "s ") + "found");
                this.pendingCount = 0;
                this.lastNotifyAt = now;
            }
        }
    }
}
