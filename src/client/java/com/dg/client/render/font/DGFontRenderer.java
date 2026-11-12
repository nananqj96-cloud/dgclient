package com.dg.client.render.font;

import com.dg.client.util.ColorUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Crisp anti-aliased TrueType font renderer backed by DGClient's bundled 4x-oversampled
 * FreeType TTF fonts (assets/dgclient/font/dgclient_sans.ttf & dgclient_bold.ttf),
 * with soft neon glow halo and animated green-white gradient support.
 */
public final class DGFontRenderer {
    public static final Identifier UI_FONT_ID = Identifier.of("dgclient", "ui");
    public static final Identifier UI_BOLD_FONT_ID = Identifier.of("dgclient", "ui_bold");

    private static final Style REGULAR_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(UI_FONT_ID));
    private static final Style BOLD_STYLE = Style.EMPTY.withFont(new StyleSpriteSource.Font(UI_BOLD_FONT_ID));

    private static boolean useCustomTtf = true;

    private DGFontRenderer() {
    }

    public static boolean isUseCustomTtf() {
        return useCustomTtf;
    }

    public static void setUseCustomTtf(boolean enabled) {
        useCustomTtf = enabled;
    }

    public static Text styled(String content, boolean bold) {
        if (content == null || content.isEmpty()) {
            return Text.empty();
        }
        if (!useCustomTtf) {
            return bold ? Text.literal(content).setStyle(Style.EMPTY.withBold(true)) : Text.literal(content);
        }
        return Text.literal(content).setStyle(bold ? BOLD_STYLE : REGULAR_STYLE);
    }

    public static float getWidth(String text, boolean bold, float scale) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null) {
            return text.length() * 6.0f * scale;
        }
        return tr.getWidth(styled(text, bold)) * scale;
    }

    public static float getWidth(String text) {
        return getWidth(text, false, 1.0f);
    }

    public static float getHeight(float scale) {
        return 9.0f * scale;
    }

    public static void drawText(DrawContext context, String text, float x, float y, int argb, boolean shadow) {
        drawScaledText(context, text, x, y, argb, false, 1.0f, shadow);
    }

    public static void drawBoldText(DrawContext context, String text, float x, float y, int argb, boolean shadow) {
        drawScaledText(context, text, x, y, argb, true, 1.0f, shadow);
    }

    public static void drawCenteredText(DrawContext context, String text, float centerX, float y, int argb, boolean bold, float scale) {
        float w = getWidth(text, bold, scale);
        drawScaledText(context, text, centerX - w * 0.5f, y, argb, bold, scale, false);
    }

    public static void drawScaledText(
            DrawContext context,
            String text,
            float x,
            float y,
            int argb,
            boolean bold,
            float scale,
            boolean shadow
    ) {
        if (text == null || text.isEmpty() || ColorUtil.getAlpha(argb) <= 2) {
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null) {
            return;
        }
        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        if (Math.abs(scale - 1.0f) > 0.001f) {
            matrices.scale(scale, scale);
        }
        context.drawText(tr, styled(text, bold), 0, 0, argb, shadow);
        matrices.popMatrix();
    }

    public static void drawGlowText(
            DrawContext context,
            String text,
            float x,
            float y,
            int coreColor,
            int glowColor,
            boolean bold,
            float scale,
            float glowStrength
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (glowStrength > 0.02f) {
            int haloAlpha = Math.round(42.0f * Math.min(1.5f, glowStrength));
            int halo = ColorUtil.withAlpha(glowColor, haloAlpha);
            float r = 0.85f * scale;
            drawScaledText(context, text, x - r, y, halo, bold, scale, false);
            drawScaledText(context, text, x + r, y, halo, bold, scale, false);
            drawScaledText(context, text, x, y - r, halo, bold, scale, false);
            drawScaledText(context, text, x, y + r, halo, bold, scale, false);
            int outerHalo = ColorUtil.withAlpha(glowColor, haloAlpha / 2);
            float r2 = 1.65f * scale;
            drawScaledText(context, text, x - r2, y - r2 * 0.5f, outerHalo, bold, scale, false);
            drawScaledText(context, text, x + r2, y + r2 * 0.5f, outerHalo, bold, scale, false);
        }
        drawScaledText(context, text, x, y, coreColor, bold, scale, false);
    }

    public static void drawAnimatedGradientText(
            DrawContext context,
            String text,
            float x,
            float y,
            boolean bold,
            float scale,
            float speed,
            float glowStrength
    ) {
        if (text == null || text.isEmpty()) {
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        if (tr == null) {
            return;
        }
        Style baseStyle = useCustomTtf ? (bold ? BOLD_STYLE : REGULAR_STYLE)
                : (bold ? Style.EMPTY.withBold(true) : Style.EMPTY);

        MutableText gradientText = Text.empty();
        for (int i = 0; i < text.length(); i++) {
            int color = ColorUtil.animatedAccent(-i * 0.16, speed);
            Style charStyle = baseStyle.withColor(TextColor.fromRgb(color & 0x00FFFFFF));
            gradientText.append(Text.literal(String.valueOf(text.charAt(i))).setStyle(charStyle));
        }

        Matrix3x2fStack matrices = context.getMatrices();
        if (glowStrength > 0.02f) {
            int halo = ColorUtil.withAlpha(ColorUtil.NEON_GREEN, Math.round(40.0f * Math.min(1.5f, glowStrength)));
            Text plain = styled(text, bold);
            float d = 0.9f * scale;
            float[][] offsets = {{-d, 0}, {d, 0}, {0, -d}, {0, d}};
            for (float[] off : offsets) {
                matrices.pushMatrix();
                matrices.translate(x + off[0], y + off[1]);
                matrices.scale(scale, scale);
                context.drawText(tr, plain, 0, 0, halo, false);
                matrices.popMatrix();
            }
        }

        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.drawText(tr, gradientText, 0, 0, 0xFFFFFFFF, false);
        matrices.popMatrix();
    }
}
