package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.MouseScrollEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.MathUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Smooth optical FOV zoom on key hold with mouse-wheel magnification control
 * and optional cinematic smooth camera damping.
 */
public final class ZoomModule extends Module {
    public final DoubleSetting targetMagnification = addSetting(new DoubleSetting(
            "Magnification", "Default zoom multiplier", 4.0, 1.5, 25.0, 0.5));
    public final BooleanSetting scrollAdjust = addSetting(new BooleanSetting(
            "Scroll Adjust", "Allows mouse wheel scrolling to adjust zoom level", true));
    public final BooleanSetting smoothTransition = addSetting(new BooleanSetting(
            "Smooth Zoom", "Interpolates FOV smoothly instead of snapping", true));
    public final BooleanSetting cinematicCamera = addSetting(new BooleanSetting(
            "Cinematic Camera", "Enables smooth camera look damping while zoomed", false));

    private double dynamicMagnification = 4.0;
    private double currentFactor = 1.0;
    private boolean prevSmoothCam = false;

    public ZoomModule() {
        super("Zoom", "Smooth FOV zoom on key hold with scroll magnification & cinematic camera", ModuleCategory.RENDER, GLFW.GLFW_KEY_C);
        getKeybind().setHoldMode(true);
    }

    @Override
    protected boolean shouldToastToggle() {
        return false;
    }

    @Override
    protected void onEnable() {
        dynamicMagnification = targetMagnification.getValue();
        if (mc.options != null) {
            prevSmoothCam = mc.options.smoothCameraEnabled;
            if (cinematicCamera.getValue()) {
                mc.options.smoothCameraEnabled = true;
            }
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null && cinematicCamera.getValue()) {
            mc.options.smoothCameraEnabled = prevSmoothCam;
        }
    }

    @EventHandler
    public void onScroll(MouseScrollEvent event) {
        if (!isEnabled() || !scrollAdjust.getValue() || mc.currentScreen != null) {
            return;
        }
        if (event.getVertical() != 0.0) {
            dynamicMagnification = MathUtil.clamp(dynamicMagnification + event.getVertical() * 0.75, 1.25, 30.0);
            event.cancel();
        }
    }

    public double modifyFov(double baseFov) {
        double desired = isEnabled() ? (1.0 / Math.max(1.0, dynamicMagnification)) : 1.0;
        if (smoothTransition.getValue()) {
            currentFactor = currentFactor + (desired - currentFactor) * 0.22;
            if (Math.abs(desired - currentFactor) < 0.002) {
                currentFactor = desired;
            }
        } else {
            currentFactor = desired;
        }
        return baseFov * currentFactor;
    }
}
