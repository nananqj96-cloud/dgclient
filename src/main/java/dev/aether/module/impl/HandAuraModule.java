package dev.aether.module.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;

public class HandAuraModule extends Module {
    public static volatile HandAuraModule active;
    private static final String COLOR_THEME = "Theme";
    private static final String COLOR_MANUAL = "Manual";
    private static final int WHITE = -1;
    private static final float SHEEN_LIFT = 0.55F;
    private static final long BREATH_NANOS = 3200000000L;
    private static final long SHEEN_NANOS = 4400000000L;
    private final ModeSetting colorMode = this.addSetting(new ModeSetting("Color", "Color", "Theme", "Theme", "Manual"));
    private final ColorSetting glowColor = this.addSetting(new ColorSetting("Glow Color", "Glow Color", -13338378));
    private final SliderSetting aura = this.addSetting(new SliderSetting("Aura", "Aura", 65.0, 0.0, 100.0, 5.0F));
    private final SliderSetting reach = this.addSetting(new SliderSetting("Reach", "Reach", 8.0, 2.0, 20.0, 1.0F));
    private final SliderSetting rim = this.addSetting(new SliderSetting("Rim", "Rim", 40.0, 0.0, 100.0, 5.0F));
    private final SliderSetting sheen = this.addSetting(new SliderSetting("Sheen", "Sheen", 25.0, 0.0, 100.0, 5.0F));
    private final ColorSetting sheenColor = this.addSetting(new ColorSetting("Sheen Color", "Sheen Color", -3153665));
    private final BooleanSetting breathe = this.addSetting(new BooleanSetting("Breathe", "Breathe", true));

    public HandAuraModule() {
        super("hand-aura", "Adds a glowing outline to your hand and held item.", Category.VISUALS);
        this.glowColor.visibleWhen(this::manual);
        this.reach.visibleWhen(() -> this.aura.get() > 0.0F);
        this.sheenColor.visibleWhen(() -> this.sheen.get() > 0.0F && this.manual());
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
        if (RenderSystem.isOnRenderThread()) {
            HandAuraPass.release();
        } else {
            Minecraft.getInstance().execute(HandAuraModule::releaseIfOff);
        }
    }

    private static void releaseIfOff() {
        if (active == null) {
            HandAuraPass.release();
        }
    }

    public static boolean beginCapture() {
        HandAuraModule module = active;
        return module != null && module.shows() && HandAuraPass.begin();
    }

    public static void endCapture(boolean drawn) {
        if (HandAuraPass.end() && drawn) {
            HandAuraModule module = active;
            float scale = Math.max(1.0F, Minecraft.getInstance().getMainRenderTarget().height / 1080.0F);
            if (module == null) {
                HandAuraPass.compose(-1, 0.0F, 2.0F, 0.0F, 2.0F * scale, -1, 0.0F, 0.0F);
            } else {
                long now = System.nanoTime();
                float phase = (float)(now % 4400000000L / 4.4E9);
                int glow = module.glowArgb();
                HandAuraPass.compose(
                    glow,
                    module.auraStrength(now),
                    module.reach.get() * scale,
                    module.rim.get() / 100.0F,
                    2.0F * scale,
                    module.sheenArgb(glow),
                    module.sheen.get() / 100.0F,
                    phase
                );
            }
        }
    }

    private boolean manual() {
        return this.colorMode.is("Manual");
    }

    private int glowArgb() {
        return this.manual() ? this.glowColor.argb() : CosmeticAccent.argb();
    }

    private int sheenArgb(int glow) {
        return this.manual() ? this.sheenColor.argb() : ARGB.srgbLerp(0.55F, glow, -1);
    }

    private boolean shows() {
        return this.aura.get() > 0.0F || this.rim.get() > 0.0F;
    }

    private float auraStrength(long now) {
        float strength = (float) this.aura.get() / 100.0F;
        if (!this.breathe.get()) {
            return strength;
        }

        double cycle = now % 3200000000L / 3.2E9;
        float swell = (float)Math.sin(cycle * Math.PI);
        return strength * (0.6F + 0.4F * swell * swell);
    }
}
