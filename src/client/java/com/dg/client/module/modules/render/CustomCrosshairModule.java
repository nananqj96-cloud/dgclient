package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;
import net.minecraft.util.hit.HitResult;

/**
 * Replaces the vanilla crosshair with a crisp neon green/white vector reticle.
 */
public final class CustomCrosshairModule extends Module {
    public enum StyleMode {
        CROSS,
        DOT,
        CIRCLE,
        GLOW_RING,
        ANIMATED
    }

    public final EnumSetting<StyleMode> style = addSetting(new EnumSetting<>(
            "Style", "Crosshair geometry preset", StyleMode.CROSS));
    public final DoubleSetting size = addSetting(new DoubleSetting(
            "Size", "Arm length or ring radius in pixels", 5.0, 1.5, 16.0, 0.5));
    public final DoubleSetting gap = addSetting(new DoubleSetting(
            "Gap", "Center gap distance in pixels", 2.5, 0.0, 10.0, 0.5));
    public final DoubleSetting thickness = addSetting(new DoubleSetting(
            "Thickness", "Stroke width in pixels", 1.4, 0.8, 4.0, 0.2));
    public final BooleanSetting centerDot = addSetting(new BooleanSetting(
            "Center Dot", "Draws a 1px crisp center dot inside cross/ring modes", true));
    public final BooleanSetting targetShift = addSetting(new BooleanSetting(
            "Entity Target Highlight", "Pulses pure white + glow when aiming at an entity", true));
    public final ColorSetting color = addSetting(new ColorSetting(
            "Color", "Default reticle color", ColorUtil.NEON_GREEN));
    public final ColorSetting targetColor = addSetting(new ColorSetting(
            "Target Color", "Reticle color when aiming at an entity", ColorUtil.PURE_WHITE)
            .visibleWhen(targetShift::getValue));

    public CustomCrosshairModule() {
        super("CustomCrosshair", "Neon green/white customizable crosshair with entity target feedback", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return style.getValue().name();
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.options.hudHidden || !mc.options.getPerspective().isFirstPerson()) {
            return;
        }
        DrawContext ctx = event.getContext();
        Matrix3x2fStack ms = ctx.getMatrices();
        float cx = event.getScreenWidth() * 0.5f;
        float cy = event.getScreenHeight() * 0.5f;

        boolean onEntity = targetShift.getValue() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.ENTITY;
        int col = onEntity ? targetColor.getColor() : color.getColor();
        float sz = size.getValue().floatValue();
        float gp = gap.getValue().floatValue();
        float th = thickness.getValue().floatValue();

        if (mc.player != null && style.getValue() == StyleMode.ANIMATED) {
            float cool = mc.player.getAttackCooldownProgress(event.getTickDelta());
            gp += (1.0f - cool) * 4.5f;
        }

        switch (style.getValue()) {
            case DOT -> {
                Render2DUtil.drawGaussianGlow(ms, cx - sz * 0.5f, cy - sz * 0.5f, sz, sz, sz * 0.5f, 4.5f, col, 0.8f);
                Render2DUtil.drawCircle(ms, cx, cy, sz * 0.5f, col);
            }
            case CIRCLE -> {
                Render2DUtil.drawCircleOutline(ms, cx, cy, sz, th, col);
                if (centerDot.getValue()) {
                    Render2DUtil.drawCircle(ms, cx, cy, 1.1f, ColorUtil.PURE_WHITE);
                }
            }
            case GLOW_RING -> {
                Render2DUtil.drawGaussianGlow(ms, cx - sz, cy - sz, sz * 2.0f, sz * 2.0f, sz, 6.0f, ColorUtil.NEON_GREEN, 1.0f);
                Render2DUtil.drawCircleOutline(ms, cx, cy, sz, th, col);
                if (centerDot.getValue()) {
                    Render2DUtil.drawCircle(ms, cx, cy, 1.3f, ColorUtil.PURE_WHITE);
                }
            }
            case CROSS, ANIMATED -> {
                if (onEntity) {
                    Render2DUtil.drawGaussianGlow(ms, cx - gp - sz, cy - gp - sz, (gp + sz) * 2.0f, (gp + sz) * 2.0f, 4.0f, 5.0f, ColorUtil.NEON_GREEN, 0.7f);
                }
                // Left, Right, Top, Bottom arms
                Render2DUtil.drawQuad(ms, cx - gp - sz, cy - th * 0.5f, sz, th, col);
                Render2DUtil.drawQuad(ms, cx + gp, cy - th * 0.5f, sz, th, col);
                Render2DUtil.drawQuad(ms, cx - th * 0.5f, cy - gp - sz, th, sz, col);
                Render2DUtil.drawQuad(ms, cx - th * 0.5f, cy + gp, th, sz, col);
                if (centerDot.getValue()) {
                    Render2DUtil.drawCircle(ms, cx, cy, Math.max(1.0f, th * 0.55f), ColorUtil.PURE_WHITE);
                }
            }
        }
    }
}
