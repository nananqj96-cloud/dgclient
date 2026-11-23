package dev.aether.module.impl;

import dev.aether.AetherClient;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaBaseFinderModule extends Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("Anubis");
    private static final String MEDIA_PREFIX = "\ud83d\udcf9";
    private static final String MEDIA_TEAM = "09-";
    private static final int SETTLE_TICKS = 60;
    private static final int SCAN_TICKS = 10;
    private static final int MAX_LISTED = 8;
    private static final long RTP_WINDOW_NANOS = 60000000000L;
    private final BooleanSetting reportEmpty = this.addSetting(new BooleanSetting("Report Empty Goliaths", true));
    private final BooleanSetting statusBox = this.addSetting(new BooleanSetting("Status Box", true));
    private final SliderSetting scale = this.addSetting(new SliderSetting("Scale", 1.0, 0.5, 3.0, 0.05F));
//     private final SliderSetting opacity = this.addSetting(HudPanel.opacitySetting());
//     private final HudPanel panel = new HudPanel(
//         this.addSetting("pos-x", HudPanel.coordinate("X")), this.addSetting("pos-y", HudPanel.coordinate("Y")), this.scale, this.opacity
    );
    private final ShardTracker shard = new ShardTracker();
//     private final ScreenText screenText = new ScreenText(16733695, 3.0F);
    private final Queue<UUID> modeUpdates = new ConcurrentLinkedQueue<>();
    private final Set<String> proven = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private Set<String> here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private String goliath = "—";
    private int settle;
    private int ticks;
    private boolean reported;
    private boolean afterRtp;
    private volatile long rtpAt;

    public MediaBaseFinderModule() {
        super("media-base-finder", "Tells you which DonutSMP media are on the goliath you land on (after /rtp), where their base most likely is.", Category.EXPLOIT);
        this.usesNotifications();
        this.scale.visibleWhen(this.statusBox::get);
        this.opacity.visibleWhen(this.statusBox::get);
    }

    @Override
    protected void onEnable() {
        this.arrive(Minecraft.getInstance());
    }

    @Override
    protected void onDisable() {
        this.shard.clear();
        this.modeUpdates.clear();
        this.proven.clear();
        this.here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    }

    private void arrive(Minecraft mc) {
        this.modeUpdates.clear();
        this.proven.clear();
        this.here = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        this.settle = 60;
        this.reported = false;
        this.goliath = label(mc);
        long sent = this.rtpAt;
        this.afterRtp = sent != 0L && System.nanoTime() - sent < 60000000000L;
    }

//     @EventHandler
//     public void onPacketSend(PacketSendEvent event) {
        String command = event.packet instanceof ServerboundChatCommandPacket plain
            ? plain.command()
            : (event.packet instanceof ServerboundChatCommandSignedPacket signed ? signed.command() : null);
        if (command != null) {
            String text = command.trim().toLowerCase(Locale.ROOT);
            if (text.equals("rtp") || text.startsWith("rtp ")) {
                this.rtpAt = System.nanoTime();
            }
        }
    }

//     @EventHandler(stage = Stage.POST)
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        LocalPlayer player = mc.player;
        if (connection != null && player != null && mc.level != null) {
            if (this.shard.moved(mc)) {
                this.arrive(mc);
            }

            this.drainModeUpdates(connection);
            if (this.settle > 0) {
                this.settle--;
            } else if (!this.reported || ++this.ticks % 10 == 0) {
                Set<String> found = this.scan(mc, connection, player);
                this.goliath = label(mc);
                if (!this.reported) {
                    this.reported = true;
                    this.here = found;
                    this.report(found);
                } else {
                    List<String> arrived = new ArrayList<>();

                    for (String name : found) {
                        if (!this.here.contains(name)) {
                            arrived.add(name);
                        }
                    }

                    this.here = found;
                    if (!arrived.isEmpty()) {
                        this.announce(arrived, found.size(), false);
                    }
                }
            }
        } else {
            this.shard.clear();
        }
    }

    private Set<String> scan(Minecraft mc, ClientPacketListener connection, LocalPlayer player) {
        Set<String> found = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String self = player.getGameProfile().name();

        for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams()) {
            if (isMedia(team)) {
                for (String member : team.getPlayers()) {
                    if (!member.equalsIgnoreCase(self) && (connection.getPlayerInfo(member) == null || this.proven.contains(member))) {
                        found.add(member);
                    }
                }
            }
        }

        return found;
    }

    private static boolean isMedia(PlayerTeam team) {
        return team.getName().startsWith("09-") || team.getPlayerPrefix().getString().contains("\ud83d\udcf9");
    }

//     @EventHandler
//     public void onPacketReceiveNetty(PacketReceiveNettyEvent event) {
        this.collect(event.packet);
    }

    private void collect(Packet<?> packet) {
        if (!(packet instanceof ClientboundBundlePacket bundle)) {
            if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
                if (info.actions().contains(Action.UPDATE_GAME_MODE) && !info.actions().contains(Action.ADD_PLAYER)) {
                    for (Entry entry : info.entries()) {
                        if (entry != null && entry.profileId() != null) {
                            this.modeUpdates.offer(entry.profileId());
                        }
                    }
                }
            }
        } else {
            for (Packet<?> inner : bundle.subPackets()) {
                this.collect(inner);
            }
        }
    }

    private void drainModeUpdates(ClientPacketListener connection) {
        UUID id;
        while ((id = this.modeUpdates.poll()) != null) {
            PlayerInfo info = connection.getPlayerInfo(id);
            if (info != null && info.getTeam() != null && isMedia(info.getTeam())) {
                String name = info.getProfile().name();
                if (this.proven.add(name)) {
                    LOGGER.info("[Media Base Finder] game mode update places {} on {}", name, this.goliath);
                }
            }
        }
    }

    private void report(Set<String> found) {
        this.rtpAt = 0L;
        LOGGER.info("[Media Base Finder] {}: {} media {}", this.goliath, found.size(), found);
        if (found.isEmpty()) {
            if (this.reportEmpty.get()) {
//                 AetherClient.chat(this, ModuleChat.body().text("No media on ").name(this.goliath).text("."));
            }
        } else {
            this.announce(new ArrayList<>(found), found.size(), true);
        }
    }

    private void announce(List<String> names, int total, boolean summary) {
        Minecraft mc = Minecraft.getInstance();
        String list = names(names);
        String area = area(mc);
        String bases = total + (total == 1 ? " media base" : " media bases") + (area == null ? "" : " within " + area + " blocks");
        if (summary) {
//             AetherClient.chat(this, ModuleChat.body().text(bases + " on ").name(this.goliath).text(": ").name(list));
        } else {
//             AetherClient.chat(this, ModuleChat.body().name(list).text(" joined ").name(this.goliath).text(", now " + bases + "."));
            LOGGER.info("[Media Base Finder] {} joined {}, now {}", list, this.goliath, total);
        }

        if (summary) {
            LOGGER.info("[Media Base Finder] screen text: afterRtp={} notifications={}", this.afterRtp, this.notificationsOn());
        }

        if (summary && this.afterRtp && this.notificationsOn()) {
            this.screenText.show(total + (total == 1 ? " Media Base" : " Media Bases"), area == null ? "on this goliath" : "within " + area + " blocks");
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.4F));
        }
    }

    private static String area(Minecraft mc) {
        if (mc.player != null && mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
            DonutGoliaths.Goliath at = DonutGoliaths.at(mc.player.getX(), mc.player.getZ());
            if (at == null) {
                return null;
            }

            int[] c = DonutGoliaths.CELLS[at.region()][at.index()];
            String w = blocks(c[2] - c[0]);
            String l = blocks(c[3] - c[1]);
            return w.equals(l) ? w : w + " × " + l;
        } else {
            return null;
        }
    }

    private static String blocks(int squares) {
        int value = squares * 12500;
        return value % 1000 == 0 ? value / 1000 + "k" : String.format(Locale.ROOT, "%.1fk", value / 1000.0);
    }

    private static String names(List<String> names) {
        StringBuilder out = new StringBuilder();
        int shown = Math.min(names.size(), 8);

        for (int i = 0; i < shown; i++) {
            out.append(i == 0 ? "" : ", ").append(NameFilter.apply(names.get(i)));
        }

        if (names.size() > shown) {
            out.append(" +").append(names.size() - shown);
        }

        return out.toString();
    }

    private static String label(Minecraft mc) {
        if (mc.player != null && mc.level != null && mc.level.dimension() == Level.OVERWORLD) {
            DonutGoliaths.Goliath at = DonutGoliaths.at(mc.player.getX(), mc.player.getZ());
            return at == null ? "Off the map" : at.label();
        } else {
            return "—";
        }
    }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.statusBox.get() && Minecraft.getInstance().player != null;
    }

    @Override
    public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
//         List<HudPanel.Line> lines = new ArrayList<>();
        if (this.settle > 0) {
//             lines.add(new HudPanel.Line("Checking…", -19648));
        } else if (this.here.isEmpty()) {
//             lines.add(new HudPanel.Line("None here", -6052949));
        } else {
            int shown = 0;

            for (String name : this.here) {
                if (shown++ == 8) {
//                     lines.add(new HudPanel.Line("+" + (this.here.size() - 8) + " more", -6052949));
                    break;
                }

//                 lines.add(new HudPanel.Line(HudPanel.face(name), NameFilter.apply(name), -1, "", -6052949));
            }
        }

        String title = this.here.size() > 1 ? "MEDIA BASES ON YOUR GOLIATH" : "MEDIA BASE ON YOUR GOLIATH";
        this.panel.card(dl, title, "", -1, lines, "staff_list", screenW, screenH, 1.0F, 220.0F);
    }

//     @EventHandler
//     public void onRender2D(Render2DEvent event) {
        this.screenText.render(event.graphics);
    }
}
