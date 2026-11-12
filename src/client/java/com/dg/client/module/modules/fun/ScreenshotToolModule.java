package com.dg.client.module.modules.fun;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.core.WaypointManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.ScreenshotRecorder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Screenshot & Stash Card Tool:
 * Captures either a clean HUD-free screenshot or a styled neon-green/white DGClient
 * Stash Card banner with optional coordinate redaction so players never leak coords.
 */
public final class ScreenshotToolModule extends Module {
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public final BooleanSetting stashCardOverlay = addSetting(new BooleanSetting(
            "Stash Card Banner", "Renders a sleek green/white DGClient banner on capture", true));
    public final BooleanSetting redactCoordinates = addSetting(new BooleanSetting(
            "Redact Coordinates", "Hides exact X/Z coordinates on Stash Card so screenshots stay opsec-safe", true));
    public final BooleanSetting captureNow = addSetting(new BooleanSetting(
            "Capture Screenshot Now", "Toggle ON (or bind this module) to capture a Stash Card screenshot", false));

    private int captureStage = 0;

    public ScreenshotToolModule() {
        super("StashCard", "Styled green/white Stash Card screenshot tool with opsec coord redaction", ModuleCategory.FUN);
        captureNow.onChange(val -> {
            if (Boolean.TRUE.equals(val)) {
                triggerCapture();
                captureNow.setValue(false);
            }
        });
    }

    @Override
    protected void onEnable() {
        triggerCapture();
        // Auto-return to standby after scheduling capture so keybind acts as one-press trigger
        captureStage = 2;
    }

    public void triggerCapture() {
        if (mc.getFramebuffer() == null) {
            return;
        }
        captureStage = 2;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (captureStage <= 0) {
            return;
        }
        if (stashCardOverlay.getValue()) {
            drawStashBanner(event.getContext(), event.getScreenWidth(), event.getScreenHeight());
        }

        captureStage--;
        if (captureStage == 0) {
            ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), text -> {
                ChatUtil.accent("Stash Card Saved:", text.getString());
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.INFO,
                        "Stash Card Captured",
                        redactCoordinates.getValue() ? "Saved (Coords Redacted)" : "Saved with Coords"
                );
            });
            setEnabled(false, true);
        }
    }

    private void drawStashBanner(DrawContext ctx, int sw, int sh) {
        String server = WaypointManager.getCurrentServerId();
        String coords;
        if (redactCoordinates.getValue() || mc.player == null) {
            coords = "Coords: [REDACTED]";
        } else {
            coords = String.format(Locale.ROOT, "X:%d Y:%d Z:%d",
                    mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
        }
        String line1 = "DGClient v" + DGClient.MOD_VERSION + "  |  " + server;
        String line2 = LocalDateTime.now().format(STAMP_FMT) + "  |  " + coords;

        float w = Math.max(DGFontRenderer.getWidth(line1, true, 0.92f), DGFontRenderer.getWidth(line2, false, 0.82f)) + 22.0f;
        float h = 30.0f;
        float x = sw - w - 12.0f;
        float y = sh - h - 12.0f;

        Render2DUtil.drawGaussianGlow(ctx.getMatrices(), x, y, w, h, 6.0f, 8.0f, ColorUtil.NEON_GREEN, 0.85f);
        Render2DUtil.drawRoundedRect(ctx.getMatrices(), x, y, w, h, 6.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(ctx.getMatrices(), x, y, w, h, 6.0f, 1.2f, ColorUtil.NEON_GREEN);
        DGFontRenderer.drawScaledText(ctx, line1, x + 10.0f, y + 6.0f, ColorUtil.NEON_GREEN, true, 0.92f, false);
        DGFontRenderer.drawScaledText(ctx, line2, x + 10.0f, y + 17.5f, ColorUtil.PURE_WHITE, false, 0.82f, false);
    }
}
