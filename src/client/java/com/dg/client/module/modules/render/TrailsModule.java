package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Draws a smooth fading neon green-to-white 3D line trail behind the local player
 * and optionally other nearby players.
 */
public final class TrailsModule extends Module {
    private record TrailPoint(Vec3d pos, long timestampMs) {
    }

    public final BooleanSetting selfTrail = addSetting(new BooleanSetting("Self", "Draws trail behind your player", true));
    public final BooleanSetting otherPlayers = addSetting(new BooleanSetting("Other Players", "Draws trails behind nearby players", true));
    public final IntSetting lifetimeMs = addSetting(new IntSetting("Lifetime (ms)", "How long trail segments persist", 1600, 300, 6000, 100));
    public final DoubleSetting lineWidth = addSetting(new DoubleSetting("Width", "Trail line thickness", 2.2, 1.0, 5.0, 0.2));
    public final ColorSetting startColor = addSetting(new ColorSetting("Head Color", "Newest segment color", ColorUtil.NEON_GREEN));
    public final ColorSetting tailColor = addSetting(new ColorSetting("Tail Color", "Oldest fading segment color", ColorUtil.PURE_WHITE));

    private final Map<UUID, Deque<TrailPoint>> playerTrails = new HashMap<>();

    public TrailsModule() {
        super("Trails", "Smooth fading neon green/white 3D motion trail behind you or other players", ModuleCategory.RENDER);
    }

    @Override
    protected void onDisable() {
        playerTrails.clear();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.world == null || mc.player == null) {
            playerTrails.clear();
            return;
        }
        long now = System.currentTimeMillis();
        long maxAge = lifetimeMs.getValue();

        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player && !selfTrail.getValue()) continue;
            if (p != mc.player && !otherPlayers.getValue()) continue;

            Deque<TrailPoint> pts = playerTrails.computeIfAbsent(p.getUuid(), k -> new ArrayDeque<>());
            Vec3d foot = p.getEntityPos().add(0.0, 0.12, 0.0);
            if (pts.isEmpty() || pts.peekLast().pos().squaredDistanceTo(foot) > 0.015) {
                pts.addLast(new TrailPoint(foot, now));
            }
            while (!pts.isEmpty() && now - pts.peekFirst().timestampMs() > maxAge) {
                pts.pollFirst();
            }
        }
        playerTrails.values().removeIf(Deque::isEmpty);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (playerTrails.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long maxAge = lifetimeMs.getValue();
        float width = lineWidth.getValue().floatValue();

        for (Deque<TrailPoint> pts : playerTrails.values()) {
            if (pts.size() < 2) {
                continue;
            }
            Iterator<TrailPoint> it = pts.iterator();
            TrailPoint prev = it.next();
            while (it.hasNext()) {
                TrailPoint cur = it.next();
                float life1 = Math.max(0.0f, 1.0f - (now - prev.timestampMs()) / (float) maxAge);
                float life2 = Math.max(0.0f, 1.0f - (now - cur.timestampMs()) / (float) maxAge);
                int c1 = ColorUtil.withAlpha(ColorUtil.lerp(tailColor.getColor(), startColor.getColor(), life1), Math.round(215 * life1));
                int c2 = ColorUtil.withAlpha(ColorUtil.lerp(tailColor.getColor(), startColor.getColor(), life2), Math.round(215 * life2));
                Render3DUtil.drawLine3D(event.getMatrices(), event.getCamera(), prev.pos(), cur.pos(), c1, c2, width, false);
                prev = cur;
            }
        }
    }
}
