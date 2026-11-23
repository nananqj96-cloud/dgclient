package dev.aether.module.impl;

import dev.aether.AetherClient;

import imgui.ImDrawList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NetheriteFinderModule extends Module {
    private static final int MAX_RENDERED_SECTIONS = 128;
    private static final long NOTIFY_COOLDOWN_MS = 2000L;
    private final ColorSetting color = this.addSetting(new ColorSetting("Near Color", -34304));
    private final ColorSetting farColor = this.addSetting(new ColorSetting("Far Color", -11688961));
    private final SliderSetting fade = this.addSetting(new SliderSetting("Fade Distance", 192.0, 32.0, 512.0, 8.0F));
    private final SliderSetting range = this.addSetting(new SliderSetting("Range", 512.0, 64.0, 1024.0, 1.0F));
    private final BooleanSetting tracers = this.addSetting(new BooleanSetting("Chunk Tracers", true));
    private final SliderSetting tracerCount = this.addSetting(new SliderSetting("Tracer Count", 3.0, 1.0, 8.0, 1.0F));
    private final BooleanSetting advanced = this.addSetting(new BooleanSetting("Advanced Finder", false));
    private final BooleanSetting showProbes = this.addSetting(new BooleanSetting("Show Probes", true));
    private final ModeSetting probeStyle = this.addSetting(new ModeSetting("Probe Style", "Dots", "Dots", "Cubes", "Pulses"));
    private final SliderSetting missingFrom = this.addSetting(new SliderSetting("Mark Missing From %", 60.0, 30.0, 100.0, 5.0F));
    private final AdvancedFinder advancedFinder = new AdvancedFinder(this);
    private final NetheriteVisuals visuals = new NetheriteVisuals();
    private int pendingCount;
    private BlockPos nearestPending;
    private double nearestPendingDistanceSq;
    private long lastNotifyAt;

    public NetheriteFinderModule() {
        super("netherite-finder", "Boxes Nether chunk sections hiding ancient debris.", Category.DONUT);
        this.usesNotifications().set(false);
        this.tracerCount.visibleWhen(this.tracers::get);
        this.showProbes.visibleWhen(this.advanced::get);
        this.probeStyle.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
        this.missingFrom.visibleWhen(() -> this.advanced.get() && this.showProbes.get());
    }

    @Override
    protected void onEnable() {
        this.clearPending();
        this.lastNotifyAt = 0L;
        NetheriteChunkTracker.setListener(this::onCandidate);
        NetheriteChunkTracker.setRevealListener(this::onRevealed);
    }

    @Override
    protected void onDisable() {
        NetheriteChunkTracker.setListener(null);
        NetheriteChunkTracker.setRevealListener(null);
        this.advancedFinder.reset(Minecraft.getInstance());
        this.visuals.clear();
    }

    private void onRevealed(BlockPos pos) {
        if (this.advancedFinder.onRevealed(pos)) {
            this.visuals.burst(pos, this.color.argb());
        }
    }

    private void onCandidate(NetheriteChunkTracker.Candidate candidate) {
        if (this.notificationsOn()) {
            BlockPos pos = new BlockPos((candidate.chunkX() << 4) + 8, (candidate.sectionY() << 4) + 8, (candidate.chunkZ() << 4) + 8);
            this.pendingCount++;
            Minecraft mc = Minecraft.getInstance();
            double distanceSq = mc.player != null ? mc.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) : 0.0;
            if (this.nearestPending == null || distanceSq < this.nearestPendingDistanceSq) {
                this.nearestPending = pos;
                this.nearestPendingDistanceSq = distanceSq;
            }
        }
    }

//     @EventHandler
    public void onTick() {
        this.flushNotifications();
        Minecraft mc = Minecraft.getInstance();
        this.advancedFinder.maintain(mc);
        if (this.advanced.get()) {
            if (!this.advancedFinder.armed()) {
                this.advancedFinder.arm();
            }

            this.advancedFinder.tick(mc);
        } else if (this.advancedFinder.armed()) {
            this.advancedFinder.disarm(mc);
        }
    }

    private void flushNotifications() {
        if (this.pendingCount != 0) {
            if (!this.notificationsOn()) {
                this.clearPending();
            } else {
                long now = System.currentTimeMillis();
                if (now - this.lastNotifyAt >= 2000L) {
                    AetherClient.chat(
                        this,
//                         ModuleChat.body()
                            .value(this.pendingCount)
                            .text(" new hidden-debris chunk" + (this.pendingCount == 1 ? " " : "s "))
                            .good("found")
                            .text(", nearest at ")
                            .coords(this.nearestPending.getX(), this.nearestPending.getY(), this.nearestPending.getZ())
                    );
                    this.clearPending();
                    this.lastNotifyAt = now;
                }
            }
        }
    }

    private void clearPending() {
        this.pendingCount = 0;
        this.nearestPending = null;
        this.nearestPendingDistanceSq = Double.MAX_VALUE;
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        // Minecraft mc = Minecraft.getInstance();
        // if (mc.level != null && mc.player != null && NetheriteChunkTracker.isNether(mc.level)) {
            // Vec3 eye = mc.player.getEyePosition(event.partialTick);
            // double maxDistance = this.range.get();
            // List<NetheriteVisuals.Section> sections = new ArrayList<>();

            // for (NetheriteChunkTracker.Candidate candidate : NetheriteChunkTracker.snapshot()) {
                // if (!this.advancedFinder.isDone(candidate)) {
                    // double x = candidate.chunkX() * 16.0;
                    // double y = candidate.sectionY() * 16.0;
                    // double z = candidate.chunkZ() * 16.0;
                    // AABB box = new AABB(x, y, z, x + 16.0, y + 16.0, z + 16.0);
                    // double distance = Math.sqrt(distanceSq(box, eye));
                    // if (distance <= maxDistance) {
                        // sections.add(new NetheriteVisuals.Section(box, distance, this.advancedFinder.progress(candidate)));
                    // }
                // }
            // }

            // sections.sort(Comparator.comparingDouble(NetheriteVisuals.Section::distance));
            // if (sections.size() > 128) {
                // sections = sections.subList(0, 128);
            // }

            // NetheriteVisuals.Options options = new NetheriteVisuals.Options(
                // this.color.argb(),
                // this.farColor.argb(),
                // this.fade.get(),
                // maxDistance,
                // this.probeStyle.get(),
                // this.missingFrom.get() / 100.0F,
                // this.tracers.get() ? this.tracerCount.getInt() : 0,
                // this.showProbes.get()
            // );
            // this.visuals.render(mc, event.partialTick, eye, sections, List.copyOf(this.advancedFinder.revealed()), this.advancedFinder, options);
        // } else {
            // this.visuals.clear();
        // }
    // }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.visuals.hasLabels();
    }

    @Override
    public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
        this.visuals.overlay(dl, screenW, screenH);
    }

    private static double distanceSq(AABB box, Vec3 point) {
        double dx = Math.max(0.0, Math.max(box.minX - point.x, point.x - box.maxX));
        double dy = Math.max(0.0, Math.max(box.minY - point.y, point.y - box.maxY));
        double dz = Math.max(0.0, Math.max(box.minZ - point.z, point.z - box.maxZ));
        return dx * dx + dy * dy + dz * dz;
    }
}
