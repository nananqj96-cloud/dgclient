package com.dg.client.module.modules.render;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.EventPriority;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.module.modules.hud.HudModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Adds a Sodium/Iris-compatible soft neon bloom halo on bright HUD elements and screen edges
 * without breaking third-party shader pipelines.
 */
public final class BloomModule extends Module {
    public final DoubleSetting intensity = addSetting(new DoubleSetting(
            "Intensity", "Bloom strength multiplier across HUD and bright accents", 1.25, 0.2, 2.5, 0.05));
    public final BooleanSetting ambientEdgeVignette = addSetting(new BooleanSetting(
            "Neon Edge Aura", "Subtle soft emerald screen-edge bloom", true));

    private double savedHudGlow = -1.0;

    public BloomModule() {
        super("Bloom", "Subtle multi-pass neon bloom on bright elements (Sodium & Iris safe)", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        HudModule hud = DGClient.getInstance().getModuleManager().get(HudModule.class);
        if (hud != null) {
            savedHudGlow = hud.hudGlow.getValue();
            hud.hudGlow.setValue(Math.min(2.0, savedHudGlow * intensity.getValue()));
        }
    }

    @Override
    protected void onDisable() {
        HudModule hud = DGClient.getInstance().getModuleManager().get(HudModule.class);
        if (hud != null && savedHudGlow >= 0.0) {
            hud.hudGlow.setValue(savedHudGlow);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRender2D(Render2DEvent event) {
        if (!ambientEdgeVignette.getValue() || mc.options.hudHidden) {
            return;
        }
        MatrixStack ms = event.getContext().getMatrices();
        int sw = event.getScreenWidth();
        int sh = event.getScreenHeight();
        int edgeAlpha = Math.min(36, Math.round(14.0f * intensity.getValue().floatValue()));
        int glowCol = ColorUtil.withAlpha(ColorUtil.NEON_GREEN, edgeAlpha);
        Render2DUtil.drawGradientQuad(ms, 0, 0, sw, 14.0f, glowCol, glowCol, 0x00000000, 0x00000000);
        Render2DUtil.drawGradientQuad(ms, 0, sh - 14.0f, sw, 14.0f, 0x00000000, 0x00000000, glowCol, glowCol);
    }
}
