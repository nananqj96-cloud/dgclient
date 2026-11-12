package com.dg.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

/**
 * Formats client-side messages in DGClient's green-and-white theme.
 */
public final class ChatUtil {
    private static final Style BRACKET_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x8FA69A));
    private static final Style BRAND_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x39FF14)).withBold(true);
    private static final Style BODY_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style ACCENT_STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x00FF88));

    private ChatUtil() {
    }

    public static void info(String text) {
        sendStyled(text, BODY_STYLE);
    }

    public static void accent(String label, String detail) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) {
            return;
        }
        MutableText prefix = buildPrefix();
        prefix.append(Text.literal(label + " ").setStyle(ACCENT_STYLE));
        prefix.append(Text.literal(detail).setStyle(BODY_STYLE));
        mc.inGameHud.getChatHud().addMessage(prefix);
    }

    public static void warn(String text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) {
            return;
        }
        MutableText prefix = buildPrefix();
        prefix.append(Text.literal("[!] ").setStyle(BRAND_STYLE));
        prefix.append(Text.literal(text).setStyle(BODY_STYLE));
        mc.inGameHud.getChatHud().addMessage(prefix);
    }

    private static void sendStyled(String body, Style style) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.inGameHud == null) {
            return;
        }
        MutableText msg = buildPrefix().append(Text.literal(body).setStyle(style));
        mc.inGameHud.getChatHud().addMessage(msg);
    }

    private static MutableText buildPrefix() {
        return Text.literal("[").setStyle(BRACKET_STYLE)
                .append(Text.literal("DGClient").setStyle(BRAND_STYLE))
                .append(Text.literal("] ").setStyle(BRACKET_STYLE));
    }
}
