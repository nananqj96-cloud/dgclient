package dev.aether.module.impl;

import java.util.Locale;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.util.StringUtil;
import org.apache.commons.lang3.StringUtils;

public class ChatMacroModule extends Module {
    public static final int MAX_MACROS = 8;
    private static final String ACTION_SEND = "Send";
    private static final String ACTION_OPEN = "Open in Chat";
    private final ModeSetting slots = this.addSetting(new ModeSetting("Slots", "3", slotChoices()));
    private final ModeSetting pressAction = this.addSetting(new ModeSetting("On Press", "Send", "Send", "Open in Chat"));
    private final SliderSetting cooldown = this.addSetting(new SliderSetting("Cooldown (s)", 1.0, 1.0, 10.0, 0.5F));
    private final KeySetting[] keys = new KeySetting[8];
//     private final StringSetting[] texts = new StringSetting[8];
    private int pendingSlot = -1;
    private long lastSendNanos;
    private boolean sentAny;

    public ChatMacroModule() {
        super("chat-macro", "Sends chat messages or commands with a key press.", Category.MISC);
        this.usesNotifications();
        this.cooldown.visibleWhen(() -> this.pressAction.is("Send"));

        for (int i = 0; i < 8; i++) {
            int slot = i + 1;
            this.keys[i] = this.addSetting("macro-" + slot + "-key", new KeySetting("Macro " + slot + " Key", -1));
//             this.texts[i] = this.addSetting("macro-" + slot + "-text", new StringSetting("Macro " + slot + " Text", ""));
            this.texts[i].privateValue();
            this.keys[i].visibleWhen(() -> slot <= this.slotCount());
            this.texts[i].visibleWhen(() -> slot <= this.slotCount());
        }
    }

    @Override
    public boolean inConfigs() {
        return false;
    }

    @Override
    protected void onEnable() {
        this.pendingSlot = -1;
    }

    @Override
    protected void onDisable() {
        this.pendingSlot = -1;
    }

//     private void onKey(KeyInputEvent event) {
        // if (event.action == 1) {
            // this.onPress(event.key);
        // }
    // }

//     private void onMouse(MouseInputEvent event) {
        // if (event.action == 1) {
            // this.onPress(1000 + event.button);
        // }
    // }

    private void onPress(int pressed) {
//         if (ModuleKeybinds.isBindable(pressed) && this.pendingSlot == -1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null) {
                int count = this.slotCount();

                for (int i = 0; i < count; i++) {
                    if (this.keys[i].value() == pressed && !this.texts[i].get().isBlank()) {
                        this.pendingSlot = i;
                        return;
                    }
                }
            }
        }
    }

    public void onTick() {
        int slot = this.pendingSlot;
        if (slot >= 0) {
            this.pendingSlot = -1;
            Minecraft mc = Minecraft.getInstance();
            ClientPacketListener connection = mc.getConnection();
            if (mc.player != null && connection != null && mc.screen == null) {
                String text = normalize(this.texts[slot].get());
                if (!text.isEmpty() && !text.equals("/")) {
                    if (this.pressAction.is("Open in Chat")) {
                        mc.setScreen(new ChatScreen(text, false));
                    } else {
                        long now = System.nanoTime();
                        long left = (long)(this.cooldown.get() * 1.0E9) - (now - this.lastSendNanos);
                        if (this.sentAny && left > 0L) {
                            double seconds = Math.ceil(left / 1.0E8) / 10.0;
//                             ModuleChat.actionBar(
//                                 this, ModuleChat.body().warn("On cooldown").text(", ").value(String.format(Locale.ROOT, "%.1fs", seconds)).text(" left")
                            );
                        } else {
                            this.lastSendNanos = now;
                            this.sentAny = true;
                            mc.gui.getChat().addRecentChat(text);
                            if (text.startsWith("/")) {
                                connection.sendCommand(text.substring(1));
                            } else {
                                connection.sendChat(text);
                            }
                        }
                    }
                }
            }
        }
    }

    static String normalize(String raw) {
        String allowed = StringUtil.filterText(raw);
        return StringUtil.trimChatMessage(StringUtils.normalizeSpace(allowed.trim()));
    }

    private int slotCount() {
        return Integer.parseInt(this.slots.get());
    }

    private static String[] slotChoices() {
        String[] choices = new String[8];

        for (int i = 0; i < 8; i++) {
            choices[i] = Integer.toString(i + 1);
        }

        return choices;
    }
}
