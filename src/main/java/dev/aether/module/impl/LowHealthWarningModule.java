package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class LowHealthWarningModule extends Module {
    private final SliderSetting warnBelow = this.addSetting(new SliderSetting("Warn Below", "Warn Below", 7.0, 2.0, 20.0, 1.0F));
    private final BooleanSetting countAbsorption = this.addSetting(new BooleanSetting("Count Absorption", "Count Absorption", true));
    private final ModeSetting pulse = this.addSetting(new ModeSetting("Pulse", "Pulse", "Heartbeat", "Heartbeat", "Breath", "Steady"));
    private final SliderSetting intensity = this.addSetting(new SliderSetting("Intensity", "Intensity", 0.7, 0.2, 1.0, 0.05F));
    private final SliderSetting reach = this.addSetting(new SliderSetting("Reach", "Reach", 0.16, 0.06, 0.35, 0.01F));
    private final BooleanSetting edgeLine = this.addSetting(new BooleanSetting("Edge Line", "Edge Line", true));
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Color", "Theme", "Theme", "Manual"));
    private final ColorSetting glowColor = this.addSetting(new ColorSetting("Glow Color", "Glow Color", -13338378));
    private float level;
    private float phase;
    private float danger;
    private long lastFrame;

    public LowHealthWarningModule() {
        super("low-health-warning", "Flashes the screen edges when your health is low.", Category.RENDER);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        long now = System.nanoTime();
        if (now - this.lastFrame > 250000000L) {
            this.level = 0.0F;
            this.phase = 0.0F;
        }
        this.lastFrame = now;

        boolean dead = player.isDeadOrDying();
        float want = 0.0F;
        if (!dead && !player.isSpectator() && !player.getAbilities().invulnerable) {
            float health = player.getHealth() + (this.countAbsorption.get() ? player.getAbsorptionAmount() : 0.0F);
            float limit = this.warnBelow.getFloat();
            if (health < limit) {
                want = 1.0F;
                this.danger = Math.max(0.0F, Math.min(1.0F, (limit - health) / limit));
            }
        }
        this.level += (want - this.level) * 0.15F;
    }

    public float getLevel() {
        return this.level;
    }

    public float getDanger() {
        return this.danger;
    }

    public int getGlowColor() {
        return this.glowColor.argb();
    }
}
