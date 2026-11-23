package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class CustomFovModule extends Module {
    public static volatile CustomFovModule active;
    private static float screenEffectScale = 1.0F;
    private final SliderSetting fov = this.addSetting(new SliderSetting("FOV", "FOV", 110.0, 30.0, 160.0, 1.0F));
    private int culledFov = Integer.MIN_VALUE;

    public CustomFovModule() {
        super("custom-fov", "Changes your field of view (FOV).", Category.VISUALS);
    }

    @Override
    protected void onEnable() {
        active = this;
        this.culledFov = this.fov.getInt();
        scheduleTerrainUpdate();
    }

    @Override
    protected void onDisable() {
        active = null;
        this.culledFov = Integer.MIN_VALUE;
        scheduleTerrainUpdate();
    }

    public float fov() {
        return this.fov.getInt();
    }

    public static void hudFovApplied(float vanilla, float applied) {
        screenEffectScale = vanilla == applied ? 1.0F : (float)(Math.tan(Math.toRadians(applied) * 0.5) / Math.tan(Math.toRadians(vanilla) * 0.5));
    }

    public static float screenEffectScale() {
        return screenEffectScale;
    }

    private void onFovChanged() {
        if (active == this) {
            int value = this.fov.getInt();
            if (value != this.culledFov) {
                this.culledFov = value;
                scheduleTerrainUpdate();
            }
        }
    }

    private static void scheduleTerrainUpdate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (!mc.isSameThread()) {
                mc.execute(CustomFovModule::scheduleTerrainUpdate);
            } else {
                if (mc.level != null && mc.levelRenderer != null) {
                    mc.levelRenderer.needsUpdate();
                }
            }
        }
    }
}
