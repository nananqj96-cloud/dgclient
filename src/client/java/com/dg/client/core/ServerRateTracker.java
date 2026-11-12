package com.dg.client.core;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.util.MathUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Estimates live server TPS from WorldTimeUpdateS2CPacket intervals, tracks packet-lag duration,
 * and measures real horizontal/3D player velocity in blocks per second.
 */
public final class ServerRateTracker {
    private final Deque<Float> tpsSamples = new ArrayDeque<>(20);
    private long lastTimePacketMs = -1L;
    private long lastAnyPacketMs = System.currentTimeMillis();
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean hasLastPos = false;
    private double blocksPerSecond = 0.0;

    public void init() {
        DGClient.getInstance().getEventBus().subscribe(this);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        long now = System.currentTimeMillis();
        lastAnyPacketMs = now;
        if (event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            if (lastTimePacketMs > 0L) {
                long elapsed = now - lastTimePacketMs;
                if (elapsed > 50L && elapsed < 30000L) {
                    float sampleTps = MathUtil.clamp(20000.0f / (float) elapsed, 0.0f, 20.0f);
                    synchronized (tpsSamples) {
                        if (tpsSamples.size() >= 20) {
                            tpsSamples.pollFirst();
                        }
                        tpsSamples.addLast(sampleTps);
                    }
                }
            }
            lastTimePacketMs = now;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            hasLastPos = false;
            blocksPerSecond = 0.0;
            return;
        }
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (hasLastPos) {
            double dx = x - lastX;
            double dz = z - lastZ;
            double dy = mc.player.isGliding() ? (y - lastY) : 0.0;
            double instantBps = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
            blocksPerSecond = blocksPerSecond * 0.65 + instantBps * 0.35;
        } else {
            hasLastPos = true;
        }
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    public float getTps() {
        synchronized (tpsSamples) {
            if (tpsSamples.isEmpty()) {
                return 20.0f;
            }
            float sum = 0.0f;
            for (float s : tpsSamples) {
                sum += s;
            }
            return sum / tpsSamples.size();
        }
    }

    public long getMillisSinceLastPacket() {
        return Math.max(0L, System.currentTimeMillis() - lastAnyPacketMs);
    }

    public double getBlocksPerSecond() {
        return blocksPerSecond;
    }

    public int getPingMs() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
}
