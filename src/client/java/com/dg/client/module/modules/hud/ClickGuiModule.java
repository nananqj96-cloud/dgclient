package com.dg.client.module.modules.hud;

import com.dg.client.DGClient;
import com.dg.client.gui.ClickGuiScreen;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import org.lwjgl.glfw.GLFW;

/**
 * Controls the Right-Shift ClickGUI screen and global theme parameters
 * (GUI scale, gaussian glow strength, animation speed, background blur, custom TTF font).
 */
public final class ClickGuiModule extends Module {
    public final DoubleSetting guiScale = addSetting(new DoubleSetting(
            "GUI Scale", "Global scale factor for ClickGUI panels", 1.0, 0.75, 1.35, 0.05));
    public final DoubleSetting glowStrength = addSetting(new DoubleSetting(
            "Glow Strength", "Intensity of neon gaussian outer bloom", 1.0, 0.0, 2.0, 0.05));
    public final DoubleSetting animSpeed = addSetting(new DoubleSetting(
            "Animation Speed", "Transition speed for panels, switches, and drawers", 1.0, 0.25, 2.5, 0.05));
    public final BooleanSetting blurBackground = addSetting(new BooleanSetting(
            "Background Blur", "Applies real post-process shader blur behind ClickGUI", true));
    public final DoubleSetting dimOpacity = addSetting(new DoubleSetting(
            "Dim Strength", "Dark emerald background dim alpha", 0.55, 0.0, 0.9, 0.05));
    public final BooleanSetting customFont = addSetting(new BooleanSetting(
            "Custom TTF Font", "Uses bundled 4x-oversampled FreeType sans font", true)
            .onChange(DGFontRenderer::setUseCustomTtf));
    public final BooleanSetting tooltips = addSetting(new BooleanSetting(
            "Tooltips", "Displays module & setting descriptions on hover", true));

    public ClickGuiModule() {
        super("ClickGUI", "Opens DGClient's neon green & white configuration interface", ModuleCategory.HUD, GLFW.GLFW_KEY_RIGHT_SHIFT);
        setVisibleInArrayList(false);
    }

    @Override
    protected boolean shouldToastToggle() {
        return false;
    }

    @Override
    protected void onEnable() {
        DGFontRenderer.setUseCustomTtf(customFont.getValue());
        if (mc != null) {
            mc.setScreen(DGClient.getInstance().getClickGuiScreen());
        }
        // ClickGuiModule acts as an instant screen opener; screen close persists config.
        setEnabled(false, false);
    }
}
