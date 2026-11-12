package com.dg.client.hud;

import com.dg.client.DGClient;
import com.dg.client.module.modules.hud.ClickGuiModule;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Interactive drag-and-drop HUD Editor with center/edge snap guides and right-click visibility toggles.
 */
public final class HudEditorScreen extends Screen {
    private HudElement dragging = null;
    private float dragOffsetX;
    private float dragOffsetY;

    public HudEditorScreen() {
        super(Text.literal("DGClient HUD Editor"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Handled explicitly in render() for crisp post-process blur control
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        ClickGuiModule guiMod = DGClient.getInstance().getModuleManager().get(ClickGuiModule.class);
        if (guiMod != null && guiMod.blurBackground.getValue()) {
            this.applyBlur(delta);
        }
        MatrixStack ms = context.getMatrices();
        Render2DUtil.drawQuad(ms, 0, 0, this.width, this.height, 0x660A0F0C);

        // Center alignment guides
        Render2DUtil.drawLine2D(ms, this.width * 0.5f, 0, this.width * 0.5f, this.height, 1.0f, 0x3339FF14, 0x3339FF14);
        Render2DUtil.drawLine2D(ms, 0, this.height * 0.5f, this.width, this.height * 0.5f, 1.0f, 0x3339FF14, 0x3339FF14);

        // Header banner
        String banner = "HUD EDITOR  —  Left-Drag to Reposition  |  Right-Click to Toggle  |  ESC to Return to ClickGUI";
        float bw = DGFontRenderer.getWidth(banner, true, 0.9f) + 24.0f;
        float bx = (this.width - bw) * 0.5f;
        Render2DUtil.drawGaussianGlow(ms, bx, 10.0f, bw, 22.0f, 6.0f, 8.0f, ColorUtil.NEON_GREEN, 0.8f);
        Render2DUtil.drawRoundedRect(ms, bx, 10.0f, bw, 22.0f, 6.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ms, bx, 10.0f, bw, 22.0f, 6.0f, 1.0f, ColorUtil.NEON_GREEN);
        DGFontRenderer.drawCenteredText(context, banner, this.width * 0.5f, 17.0f, ColorUtil.PURE_WHITE, true, 0.9f);

        for (HudElement elem : DGClient.getInstance().getHudManager().getElements()) {
            elem.render(context, this.width, this.height, delta, true);
            float ex = elem.getX(this.width);
            float ey = elem.getY(this.height);
            float ew = elem.getWidth();
            float eh = elem.getHeight();
            boolean hovered = mouseX >= ex && mouseX <= ex + ew && mouseY >= ey && mouseY <= ey + eh;
            int outline = !elem.isEnabled() ? 0x888FA69A : (hovered || elem == dragging ? ColorUtil.NEON_GREEN : ColorUtil.MINT_GREEN);
            Render2DUtil.drawRoundedOutline(ms, ex - 2.0f, ey - 2.0f, ew + 4.0f, eh + 4.0f, 5.0f, hovered ? 1.6f : 1.0f, outline);
            String cap = elem.getDisplayName() + (elem.isEnabled() ? "" : " [OFF]");
            DGFontRenderer.drawScaledText(context, cap, ex, ey - 10.0f, outline, false, 0.78f, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var list = DGClient.getInstance().getHudManager().getElements();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudElement elem = list.get(i);
            float ex = elem.getX(this.width);
            float ey = elem.getY(this.height);
            if (mouseX >= ex && mouseX <= ex + elem.getWidth() && mouseY >= ey && mouseY <= ey + elem.getHeight()) {
                if (button == 0) {
                    dragging = elem;
                    dragOffsetX = (float) mouseX - ex;
                    dragOffsetY = (float) mouseY - ey;
                    return true;
                } else if (button == 1) {
                    elem.setEnabled(!elem.isEnabled());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null && button == 0) {
            float nx = (float) mouseX - dragOffsetX;
            float ny = (float) mouseY - dragOffsetY;
            // Snap to screen center within 6px
            float centerX = (this.width - dragging.getWidth()) * 0.5f;
            float centerY = (this.height - dragging.getHeight()) * 0.5f;
            if (Math.abs(nx - centerX) < 6.0f) nx = centerX;
            if (Math.abs(ny - centerY) < 6.0f) ny = centerY;
            dragging.setPosition(nx, ny, this.width, this.height);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            DGClient.getInstance().getConfigManager().saveAll();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        DGClient.getInstance().getConfigManager().saveAll();
        if (this.client != null) {
            this.client.setScreen(DGClient.getInstance().getClickGuiScreen());
        }
    }
}
