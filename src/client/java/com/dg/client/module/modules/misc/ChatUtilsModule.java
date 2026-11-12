package com.dg.client.module.modules.misc;

import com.dg.client.DGClient;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.ColorUtil;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Chat Utilities:
 * - Clean green/white [HH:mm:ss] timestamps
 * - Anti-spam duplicate message counter
 * - Highlights own username & friends in neon green
 * - Outgoing Coordinate-Leak Opsec Guard (blocks accidental X/Z coordinate leaks in public chat!)
 */
public final class ChatUtilsModule extends Module {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern COORD_LEAK_PATTERN = Pattern.compile("(?<!\\d)(-?\\d{3,7})[,\\s]+(?:-?\\d{1,3}[,\\s]+)?(-?\\d{3,7})(?!\\d)");

    public final BooleanSetting timestamps = addSetting(new BooleanSetting(
            "Timestamps", "Prepends a green/white [HH:mm:ss] timestamp to incoming messages", true));
    public final BooleanSetting highlightFriendsAndSelf = addSetting(new BooleanSetting(
            "Highlight Mentions", "Accents messages mentioning your username or friends", true));
    public final BooleanSetting antiSpamCounter = addSetting(new BooleanSetting(
            "Anti-SpamTag", "Tags consecutive identical messages with [x2], [x3], etc.", true));
    public final BooleanSetting coordLeakGuard = addSetting(new BooleanSetting(
            "Coord-Leak Guard", "Intercepts outgoing public chat containing 3+ digit coordinates (use .say to confirm)", true));

    private static boolean hooksInstalled = false;
    private String lastRawMessage = "";
    private int repeatCount = 1;

    public ChatUtilsModule() {
        super("ChatUtils", "Timestamps, friend mention highlight, anti-spam & coord-leak guard", ModuleCategory.MISC);
        setEnabled(true, true);
        installHooks();
    }

    private void installHooks() {
        if (hooksInstalled) {
            return;
        }
        hooksInstalled = true;

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            ChatUtilsModule mod = DGClient.getInstance().getModuleManager().get(ChatUtilsModule.class);
            if (mod == null || !mod.isEnabled() || !mod.coordLeakGuard.getValue()) {
                return true;
            }
            if (message.startsWith(".")) {
                return true;
            }
            if (COORD_LEAK_PATTERN.matcher(message).find()) {
                ChatUtil.warn("Coord-Leak Guard blocked outgoing message containing coordinates!");
                ChatUtil.info("Use '.say " + message + "' if you intentionally want to send it.");
                return false;
            }
            return true;
        });

        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> {
            if (overlay || message == null) {
                return message;
            }
            ChatUtilsModule mod = DGClient.getInstance().getModuleManager().get(ChatUtilsModule.class);
            if (mod == null || !mod.isEnabled()) {
                return message;
            }
            return mod.decorateIncoming(message);
        });
    }

    public Text decorateIncoming(Text original) {
        String raw = original.getString();
        if (raw.startsWith("[DGClient]")) {
            return original;
        }

        MutableText result = Text.empty();
        if (timestamps.getValue()) {
            String ts = "[" + LocalTime.now().format(TIME_FMT) + "] ";
            result.append(Text.literal(ts).setStyle(Style.EMPTY.withColor(ColorUtil.NEON_GREEN & 0x00FFFFFF)));
        }

        boolean mentioned = false;
        if (highlightFriendsAndSelf.getValue() && mc.player != null) {
            String selfName = mc.player.getName().getString();
            if (!selfName.isEmpty() && raw.contains(selfName)) {
                mentioned = true;
            } else {
                for (String friend : DGClient.getInstance().getFriendManager().getFriends()) {
                    if (raw.toLowerCase().contains(friend.toLowerCase())) {
                        mentioned = true;
                        break;
                    }
                }
            }
        }

        if (mentioned) {
            result.append(Text.literal("» ").setStyle(Style.EMPTY.withColor(ColorUtil.NEON_GREEN & 0x00FFFFFF).withBold(true)));
        }
        result.append(original);

        if (antiSpamCounter.getValue()) {
            if (raw.equals(lastRawMessage)) {
                repeatCount++;
                result.append(Text.literal(" [x" + repeatCount + "]")
                        .setStyle(Style.EMPTY.withColor(ColorUtil.MINT_GREEN & 0x00FFFFFF)));
            } else {
                lastRawMessage = raw;
                repeatCount = 1;
            }
        }
        return result;
    }
}
