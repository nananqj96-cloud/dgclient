package com.dg.client.module.modules.fun;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.EntityEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.module.modules.misc.SessionStatsModule;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.util.ChatUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.sound.SoundEvents;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual Range, Server Join/Leave, and Totem-Pop Notifier with per-player pop counter.
 */
public final class VisualRangeNotifierModule extends Module {
    public final BooleanSetting visualRange = addSetting(new BooleanSetting(
            "Visual Range", "Alerts when another player enters or leaves your render distance", true));
    public final BooleanSetting joinLeave = addSetting(new BooleanSetting(
            "Join / Leave", "Alerts when players join or leave the server", false));
    public final BooleanSetting totemPops = addSetting(new BooleanSetting(
            "Totem Pops", "Counts and announces player totem pops in range", true));
    public final BooleanSetting playPingSound = addSetting(new BooleanSetting(
            "Sound Ping", "Plays a subtle note chime when an unknown player enters visual range", true));

    private final Map<UUID, Integer> popCounts = new ConcurrentHashMap<>();

    public VisualRangeNotifierModule() {
        super("Notifier", "Visual range, player join/leave, and totem-pop counter alerts", ModuleCategory.FUN);
    }

    public int getPopCount(UUID uuid) {
        return popCounts.getOrDefault(uuid, 0);
    }

    @EventHandler
    public void onEntityAdd(EntityEvent.Add event) {
        if (!visualRange.getValue() || mc.player == null) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity p && p != mc.player) {
            String name = p.getName().getString();
            boolean friend = DGClient.getInstance().getFriendManager().isFriend(name);
            String tag = friend ? "[Friend] " + name : name;
            String coords = String.format(Locale.ROOT, "X:%d Z:%d", p.getBlockX(), p.getBlockZ());
            DGClient.getInstance().getNotificationManager().push(
                    friend ? NotificationManager.Type.INFO : NotificationManager.Type.WARNING,
                    "Visual Range (+)",
                    tag + " @ " + coords
            );
            ChatUtil.accent("Visual Range (+):", tag + " entered at " + coords);
            if (playPingSound.getValue() && !friend) {
                mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 0.7f, 1.6f);
            }
        }
    }

    @EventHandler
    public void onEntityRemove(EntityEvent.Remove event) {
        if (!visualRange.getValue() || mc.player == null) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity p && p != mc.player) {
            String name = p.getName().getString();
            ChatUtil.info("Visual Range (-): " + name + " left render distance.");
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.world == null) {
            return;
        }

        if (totemPops.getValue() && event.getPacket() instanceof EntityStatusS2CPacket st
                && st.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            Entity ent = st.getEntity(mc.world);
            if (ent instanceof PlayerEntity p) {
                int count = popCounts.merge(p.getUuid(), 1, Integer::sum);
                if (p == mc.player) {
                    SessionStatsModule stats = DGClient.getInstance().getModuleManager().get(SessionStatsModule.class);
                    if (stats != null) {
                        stats.incrementSelfTotemPop();
                    }
                }
                ChatUtil.accent("Totem Pop:", p.getName().getString() + " popped " + count + (count == 1 ? " totem!" : " totems!"));
            }
        }

        if (joinLeave.getValue() && mc.getNetworkHandler() != null && mc.player != null && mc.player.age > 40) {
            if (event.getPacket() instanceof PlayerListS2CPacket pl) {
                for (PlayerListS2CPacket.Entry entry : pl.getPlayerAdditionEntries()) {
                    if (entry.profile() != null && entry.profile().name() != null) {
                        ChatUtil.info("[+] " + entry.profile().name() + " joined.");
                    }
                }
            } else if (event.getPacket() instanceof PlayerRemoveS2CPacket rem) {
                for (UUID id : rem.profileIds()) {
                    PlayerListEntry ple = mc.getNetworkHandler().getPlayerListEntry(id);
                    if (ple != null && ple.getProfile() != null) {
                        ChatUtil.info("[-] " + ple.getProfile().name() + " left.");
                    }
                    popCounts.remove(id);
                }
            }
        }
    }
}
