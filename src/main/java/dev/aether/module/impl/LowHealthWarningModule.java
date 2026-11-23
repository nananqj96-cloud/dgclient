package dev.aether.module.impl;

import imgui.ImDrawList;
import imgui.ImGui;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;

public class LowHealthWarningModule extends Module {
    private static final String PULSE_HEARTBEAT = "Heartbeat";
    private static final String PULSE_BREATH = "Breath";
    private static final String PULSE_STEADY = "Steady";
    private static final String COLOR_THEME = "Theme";
    private static final String COLOR_MANUAL = "Manual";
    private static final int OPAQUE = -16777216;
    private static final int WHITE = -1;
    private static final float EDGE_LIFT = 0.5F;
    private static final float RISE_RATE = 5.0F;
    private static final float FALL_RATE = 2.6F;
    private static final float DEATH_RATE = 12.0F;
    private static final float CALM_BPM = 64.0F;
    private static final float PANIC_BPM = 150.0F;
    private static final float SECOND_BEAT = 0.2F;
    private static final float BEAT_WIDTH = 0.065F;
    private static final long GAP_NANOS = 250000000L;
    private static final float TAU = (float) (Math.PI * 2);
    private static final float[] BAND_REACH = new float[]{1.0F, 0.55F, 0.22F};
    private static final float[] BAND_WEIGHT = new float[]{0.5F, 0.34F, 0.3F};
    private final SliderSetting warnBelow = this.addSetting(new SliderSetting("Warn Below", 7.0, 2.0, 20.0, 1.0F));
    private final BooleanSetting countAbsorption = this.addSetting(new BooleanSetting("Count Absorption", true));
    private final ModeSetting pulse = this.addSetting(new ModeSetting("Pulse", "Heartbeat", "Heartbeat", "Breath", "Steady"));
    private final SliderSetting intensity = this.addSetting(new SliderSetting("Intensity", 0.7, 0.2, 1.0, 0.05F));
    private final SliderSetting reach = this.addSetting(new SliderSetting("Reach", 0.16, 0.06, 0.35, 0.01F));
    private final BooleanSetting edgeLine = this.addSetting(new BooleanSetting("Edge Line", true));
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Theme", "Theme", "Manual"));
    private final ColorSetting glowColor = this.addSetting(new ColorSetting("Glow Color", -13338378));
    private final ColorSetting edgeColor = this.addSetting(new ColorSetting("Edge Color", -5650177));
    private float level;
    private float danger;
    private float phase;
    private long lastFrame;

    public LowHealthWarningModule() {
        super("low-health-warning", "Flashes the screen edges when your health is low.", Category.RENDER);
        this.glowColor.visibleWhen(this::manual);
        this.edgeColor.visibleWhen(() -> this.edgeLine.get() && this.manual());
    }

    @Override
    protected void onEnable() {
        this.level = 0.0F;
        this.danger = 0.0F;
        this.phase = 0.0F;
        this.lastFrame = 0L;
    }

    @Override
    protected void onDisable() {
        this.level = 0.0F;
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled();
    }

    @Override
    public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
        Minecraft mc = Minecraft.getInstance();
        float dt = Math.min(Math.max(ImGui.getIO().getDeltaTime(), 0.0F), 0.1F);
        long now = System.nanoTime();
        if (now - this.lastFrame > 250000000L) {
            this.level = 0.0F;
            this.phase = 0.0F;
        }

        this.lastFrame = now;
        LocalPlayer player = mc.player;
        boolean dead = player == null || player.isDeadOrDying();
        float want = 0.0F;
        if (!dead && !mc.options.hideGui && !player.isSpectator() && !player.getAbilities().invulnerable) {
            float health = player.getHealth() + (this.countAbsorption.get() ? player.getAbsorptionAmount() : 0.0F);
            float limit = this.warnBelow.get();
            if (health < limit) {
                want = 1.0F;
                this.danger = Math.max(0.0F, Math.min(1.0F, (limit - health) / limit));
            }
        }

        float rate = dead ? 12.0F : (want > this.level ? 5.0F : 2.6F);
        this.level = this.level + (want - this.level) * (1.0F - (float)Math.exp(-rate * dt));
        if (this.level < 0.003F && want == 0.0F) {
            this.level = 0.0F;
            this.phase = 0.0F;
        } else {
            float bpm = 64.0F + 86.0F * this.danger;
            if (this.pulse.is("Breath")) {
                bpm *= 0.5F;
            }

            this.phase = (this.phase + dt * bpm / 60.0F) % 1.0F;
            float beat = this.beat();
            float shown = this.level * this.level * (3.0F - 2.0F * this.level);
            float strength = this.intensity.get() * shown * (0.45F + 0.55F * this.danger);
            float depth = this.reach.get() * Math.min(screenW, screenH) * (1.0F + 0.12F * beat);
            boolean manual = this.manual();
            int accent = manual ? 0 : CosmeticAccent.argb();
//             int base = GuiPalette.abgr(manual ? this.glowColor.argb() | 0xFF000000 : accent);

            for (int i = 0; i < BAND_REACH.length; i++) {
                float alpha = strength * BAND_WEIGHT[i] * (0.6F + 0.4F * beat);
                frame(dl, screenW, screenH, depth * BAND_REACH[i], withAlpha(base, alpha));
            }

            if (this.edgeLine.get()) {
                float line = Math.max(2.0F, depth * 0.07F);
                float alpha = this.intensity.get() * shown * (0.2F + 0.55F * beat);
                int edge = manual ? this.edgeColor.argb() | 0xFF000000 : ARGB.srgbLerp(0.5F, accent, -1);
//                 frame(dl, screenW, screenH, line, EgyptDraw.col(edge, alpha));
            }
        }
    }

    private float beat() {
        if (this.pulse.is("Steady")) {
            return 0.35F;
        } else {
            return this.pulse.is("Breath")
                ? 0.5F - 0.5F * (float)Math.cos((float) (Math.PI * 2) * this.phase)
                : Math.min(1.0F, bump(this.phase, 0.0F) + 0.62F * bump(this.phase, 0.2F));
        }
    }

    private static float bump(float at, float center) {
        float d = Math.abs(at - center);
        d = Math.min(d, 1.0F - d) / 0.065F;
        return (float)Math.exp(-d * d);
    }

    private boolean manual() {
        return this.colorMode.is("Manual");
    }

    private static int withAlpha(int abgr, float alpha) {
        int a = Math.round(255.0F * Math.max(0.0F, Math.min(1.0F, alpha)));
        return abgr & 16777215 | a << 24;
    }

    private static void frame(ImDrawList dl, float w, float h, float depth, int edge) {
        if (!(depth < 1.0F) && edge >>> 24 != 0) {
            int clear = edge & 16777215;
            dl.addRectFilledMultiColor(0.0F, 0.0F, w, depth, edge, edge, clear, clear);
            dl.addRectFilledMultiColor(0.0F, h - depth, w, h, clear, clear, edge, edge);
            dl.addRectFilledMultiColor(0.0F, 0.0F, depth, h, edge, clear, clear, edge);
            dl.addRectFilledMultiColor(w - depth, 0.0F, w, h, clear, edge, edge, clear);
        }
    }
}
