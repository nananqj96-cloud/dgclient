package com.dg.client.module.modules.fun;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.sound.SoundEvents;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Anarchy Queue Notifier: parses queue position updates ("Position in queue: 12")
 * from server game/actionbar messages and fires toast + chime alerts at threshold.
 */
public final class QueueNotifierModule extends Module {
    private static final Pattern QUEUE_PATTERN = Pattern.compile(
            "(?i)(?:position\\s+in\\s+queue|queue\\s+position)\\s*[:=-]?\\s*(\\d+)");

    public final IntSetting threshold = addSetting(new IntSetting(
            "Alert Position", "Alerts when queue position drops at or below this number", 15, 1, 100, 1));
    public final BooleanSetting playSound = addSetting(new BooleanSetting(
            "Sound Chime", "Plays an audible chime when threshold is reached", true));

    private int lastPosition = -1;
    private long lastAlertMs = 0L;
    private static boolean listenerRegistered = false;

    public QueueNotifierModule() {
        super("QueueNotifier", "Sound & toast alerts when anarchy server queue nears front", ModuleCategory.FUN);
        ensureHook();
    }

    @Override
    public String getDisplayInfo() {
        return lastPosition > 0 ? "#" + lastPosition : null;
    }

    private void ensureHook() {
        if (listenerRegistered) {
            return;
        }
        listenerRegistered = true;
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            QueueNotifierModule mod = DGClient.getInstance().getModuleManager().get(QueueNotifierModule.class);
            if (mod != null && mod.isEnabled() && message != null) {
                mod.inspectText(message.getString());
            }
        });
    }

    public void inspectText(String raw) {
        Matcher m = QUEUE_PATTERN.matcher(raw);
        if (!m.find()) {
            return;
        }
        try {
            int pos = Integer.parseInt(m.group(1));
            lastPosition = pos;
            long now = System.currentTimeMillis();
            if (pos <= threshold.getValue() && now - lastAlertMs > 12000L) {
                lastAlertMs = now;
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        "Queue Position #" + pos,
                        "Almost in! Get ready."
                );
                if (playSound.getValue() && mc.player != null) {
                    mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
