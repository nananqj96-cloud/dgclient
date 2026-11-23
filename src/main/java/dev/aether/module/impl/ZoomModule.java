package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class ZoomModule extends Module {
    private static final float MIN_ZOOM = 1.1F;
    private static final float MAX_ZOOM = 50.0F;
    private static final float SCROLL_STEP = 1.2F;
    private static final float FAST_RATE = 30.0F;
    private static final float SLOW_RATE = 5.0F;
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static volatile ZoomModule instance;
    private static float target = 1.0F;
    private static float current = 1.0F;
    private static long lastNanos;
    private final SliderSetting zoom = this.addSetting(new SliderSetting("Zoom", "Zoom", 4.0, 1.5, 20.0, 0.5F));
    private final SliderSetting smoothness = this.addSetting(new SliderSetting("Smoothness", "Smoothness", 60.0, 0.0, 100.0, 5.0F));
    private final BooleanSetting scroll = this.addSetting(new BooleanSetting("Scroll", "Scroll", true));
    private final BooleanSetting lowerSensitivity = this.addSetting(new BooleanSetting("Lower Sensitivity", "Lower Sensitivity", true));
    private final BooleanSetting cinematic = this.addSetting(new BooleanSetting("Cinematic Camera", "Cinematic Camera", false));
    private boolean smoothBefore;
    private boolean smoothPushed;

    public ZoomModule() {
        super("zoom", "Hold the key to zoom in.", Category.VISUALS);
        instance = this;
    }

    @Override
    protected void onEnable() {
        target = this.zoom.getFloat();
        if (this.cinematic.get()) {
            Minecraft mc = Minecraft.getInstance();
            this.smoothBefore = mc.options.smoothCamera;
            mc.options.smoothCamera = true;
            this.smoothPushed = true;
        }
    }

    @Override
    protected void onDisable() {
        target = 1.0F;
        if (this.smoothPushed) {
            Minecraft.getInstance().options.smoothCamera = this.smoothBefore;
            this.smoothPushed = false;
        }
    }

    public static float fov(float fov) {
        float factor = advance();
        return factor > 1.0001F ? fov / factor : fov;
    }

    public static double turnScale() {
        ZoomModule module = instance;
        return module != null && module.lowerSensitivity.get() && !(current <= 1.0001F) ? 1.0 / current : 1.0;
    }

    public static boolean scroll(double amount) {
        ZoomModule module = instance;
        if (module != null && module.isEnabled() && module.scroll.get() && amount != 0.0) {
            float next = target * (amount > 0.0 ? 1.2F : 0.8333333F);
            target = Math.max(1.1F, Math.min(50.0F, next));
            return true;
        } else {
            return false;
        }
    }

    private static float advance() {
        long now = System.nanoTime();
        float seconds = lastNanos == 0L ? 0.0F : Math.min(0.1F, (float)(now - lastNanos) / 1.0E9F);
        lastNanos = now;
        ZoomModule module = instance;
        float smooth = module == null ? 0.0F : module.smoothness.getFloat() / 100.0F;
        if (!(smooth <= 0.0F) && current != target) {
            float rate = 30.0F + -25.0F * smooth;
            float blend = 1.0F - (float)Math.exp(-seconds * rate);
            double from = Math.log(current);
            double to = Math.log(target);
            current = (float)Math.exp(from + (to - from) * blend);
            if (Math.abs(current - target) < 0.002F) {
                current = target;
            }

            return current;
        } else {
            current = target;
            return current;
        }
    }
}
