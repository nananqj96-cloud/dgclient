package dev.aether.module.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

public class LightEspModule extends Module {
    private static final long SCAN_COOLDOWN_MS = 1000L;
    private static final int MAX_BOXES = 12000;
    private static final float LINE_WIDTH = 1.5F;
    private final SessionWorker<Map<Integer, List<AABB>>> worker = new SessionWorker<>("Anubis-Light");
    private ClientLevel sessionLevel;
    private LightSnapshot collecting;
    private long lastScanTime;
    private Map<Integer, List<AABB>> renderData = Map.of();
    private final SliderSetting sRadius = this.addSetting(new SliderSetting("Render Radius", 8.0, 1.0, 16.0, 1.0F));
    private final SliderSetting sMinLight = this.addSetting(new SliderSetting("Min Light", 5.0, 1.0, 15.0, 1.0F));
    private final BooleanSetting sDenoise = this.addSetting(new BooleanSetting("Denoise", true));
    private final ColorSetting sColor = this.addSetting(new ColorSetting("Color", -2130719744));

    public LightEspModule() {
        super("light-debug", "Highlights artificial light underground.", Category.EXPLOIT);
        List.of(this.sRadius, this.sMinLight, this.sDenoise).forEach(setting -> setting.onChange(this::resetScan));
    }

    @Override
    protected void onEnable() {
        this.resetScan();
    }

    @Override
    protected void onDisable() {
        this.resetScan();
    }

    private void resetScan() {
        this.worker.close();
        this.sessionLevel = null;
        this.collecting = null;
        this.renderData = Map.of();
        this.lastScanTime = 0L;
    }

//     @EventHandler
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (this.sessionLevel != mc.level) {
            this.resetScan();
            this.sessionLevel = mc.level;
        }

        if (mc.level != null && mc.player != null) {
            this.worker.poll(result -> this.renderData = result);
            if (!this.worker.busy()) {
                long now = System.currentTimeMillis();
                if (this.collecting == null) {
                    if (now - this.lastScanTime < 1000L) {
                        return;
                    }

                    this.collecting = new LightSnapshot(
                        mc.player.chunkPosition(), clampInt(num(this.sRadius, 8), 1, 16), clampInt(num(this.sMinLight, 5), 1, 15)
                    );
                }

                if (this.collecting.collect(mc.level)) {
                    Map<BlockPos, Integer> hits = this.collecting.hits();
                    boolean denoise = this.isDenoise();
                    int minLight = clampInt(num(this.sMinLight, 5), 1, 15);
                    this.collecting = null;
                    this.lastScanTime = now;
                    this.worker.submit(() -> buildBoxes(hits, denoise, minLight));
                }
            }
        }
    }

    static Map<Integer, List<AABB>> buildBoxes(Map<BlockPos, Integer> hits, boolean denoise, int minLight) {
        Set<BlockPos> skip = new HashSet<>();
        if (denoise) {
            for (Entry<BlockPos, Integer> entry : hits.entrySet()) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancellationException();
                }

                BlockPos source = entry.getKey();
                int light = entry.getValue();
                if (light >= minLight + 1 && light <= minLight + 2 && !skip.contains(source)) {
                    boolean stronger = false;

                    for (int x = -1; x <= 1 && !stronger; x++) {
                        for (int y = -1; y <= 1 && !stronger; y++) {
                            for (int z = -1; z <= 1; z++) {
                                if (hits.getOrDefault(source.offset(x, y, z), 0) > light) {
                                    stronger = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!stronger) {
                        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                        queue.add(source);
                        skip.add(source);

                        while (!queue.isEmpty()) {
                            if (Thread.currentThread().isInterrupted()) {
                                throw new CancellationException();
                            }

                            BlockPos pos = queue.removeFirst();

                            for (Direction dir : Direction.values()) {
                                BlockPos next = pos.relative(dir);
                                int value = hits.getOrDefault(next, 0);
                                if (value >= minLight && value <= light && skip.add(next)) {
                                    queue.addLast(next);
                                }
                            }
                        }
                    }
                }
            }
        }

        Map<Integer, List<AABB>> built = new HashMap<>();
        int count = 0;

        for (Entry<BlockPos, Integer> entry : hits.entrySet()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new CancellationException();
            }

            BlockPos pos = entry.getKey();
            if (!skip.contains(pos)) {
                boolean exposed = false;

                for (Direction dir : Direction.values()) {
                    BlockPos next = pos.relative(dir);
                    if (!hits.containsKey(next) || skip.contains(next)) {
                        exposed = true;
                        break;
                    }
                }

                if (exposed) {
                    built.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(new AABB(pos));
                    if (++count >= 12000) {
                        break;
                    }
                }
            }
        }

        built.replaceAll((light, boxes) -> List.copyOf((Collection<? extends AABB>)boxes));
        return Map.copyOf(built);
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        // Minecraft mc = Minecraft.getInstance();
        // if (mc.level != null && mc.player != null && mc.level == this.sessionLevel) {
            // Map<Integer, List<AABB>> data = this.renderData;
            // if (!data.isEmpty() && this.sColor != null) {
                // int base = this.sColor.argb();
                // int baseA = base >>> 24 & 0xFF;
                // int baseR = base >> 16 & 0xFF;
                // int baseG = base >> 8 & 0xFF;
                // int baseB = base & 0xFF;

                // for (Entry<Integer, List<AABB>> entry : data.entrySet()) {
                    // float t = clamp01(entry.getKey().intValue() / 15.0F);
                    // int r = lerp(baseR, 255, t);
                    // int g = lerp(baseG, 255, t);
                    // int b = lerp(baseB, 255, t);
                    // int fillA = clampByte(Math.round(baseA * (0.5F + 0.5F * t)));
                    // int fill = fillA << 24 | r << 16 | g << 8 | b;
                    // int stroke = 0xFF000000 | Math.round(r * 0.45F) << 16 | Math.round(g * 0.45F) << 8 | Math.round(b * 0.45F);
                    // GizmoStyle style = GizmoStyle.strokeAndFill(stroke, 1.5F, fill);

                    // for (AABB box : entry.getValue()) {
                        // int cx = (int)box.minX >> 4;
                        // int cz = (int)box.minZ >> 4;
                        // if (mc.level.getChunkSource().hasChunk(cx, cz)) {
                            // GizmoProperties props = Gizmos.cuboid(box, style);
                            // props.setAlwaysOnTop();
                        // }
                    // }
                // }
            // }
        // }
    // }

    private boolean isDenoise() {
        return this.sDenoise != null && this.sDenoise.value().get();
    }

    private static int num(NumberSetting s, int fallback) {
        return s == null ? fallback : Math.round(s.value()[0]);
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int clampByte(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }
}
