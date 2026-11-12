package com.dg.client.module.modules.render;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Renders an expanding neon green/white shockwave burst and floating pop counter
 * whenever a player pops a Totem of Undying.
 */
public final class TotemPopEffectsModule extends Module {
    private record PopBurst(Vec3d origin, String label, long startTimeMs) {
    }

    public final ColorSetting burstColor = addSetting(new ColorSetting(
            "Burst Color", "Shockwave ring accent color", ColorUtil.NEON_GREEN));
    public final BooleanSetting floatingCounter = addSetting(new BooleanSetting(
            "Floating Counter", "Shows rising '-1 TOTEM (xN)' label at pop location", true));
    public final DoubleSetting expandRadius = addSetting(new DoubleSetting(
            "Burst Radius", "Maximum shockwave ring expansion in blocks", 2.4, 0.8, 5.0, 0.2));

    private final Map<UUID, Integer> popCounts = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<PopBurst> bursts = new CopyOnWriteArrayList<>();

    public TotemPopEffectsModule() {
        super("TotemPopEffects", "Glowing neon burst and floating pop counter on Totem pops", ModuleCategory.RENDER);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!(event.getPacket() instanceof EntityStatusS2CPacket pkt) || mc.world == null) {
            return;
        }
        if (pkt.getStatus() == 35) { // Totem of Undying activation
            Entity entity = pkt.getEntity(mc.world);
            if (entity != null) {
                int count = 1;
                if (entity instanceof PlayerEntity pe) {
                    count = popCounts.merge(pe.getUuid(), 1, Integer::sum);
                }
                String text = entity.getName().getString() + " -" + count + " TOTEM";
                bursts.add(new PopBurst(entity.getEntityPos().add(0.0, 1.0, 0.0), text, System.currentTimeMillis()));
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (bursts.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<PopBurst> it = bursts.iterator();
        while (it.hasNext()) {
            PopBurst b = it.next();
            long age = now - b.startTimeMs();
            if (age > 1400L) {
                it.remove();
                continue;
            }
            float t = age / 1400.0f;
            float fade = 1.0f - t;
            double r = 0.35 + t * expandRadius.getValue();
            double y = b.origin().y + t * 0.8;
            Box ring = new Box(b.origin().x - r, y - 0.06, b.origin().z - r, b.origin().x + r, y + 0.06, b.origin().z + r);

            int col = ColorUtil.withAlpha(burstColor.getColor(), Math.round(220 * fade));
            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), ring,
                    col, ColorUtil.withAlpha(ColorUtil.PURE_WHITE, Math.round(240 * fade)), fade, true);

            if (floatingCounter.getValue()) {
                Vec3d labelPos = b.origin().add(0.0, 0.9 + t * 1.15, 0.0);
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(), labelPos,
                        b.label(), ColorUtil.PURE_WHITE, ColorUtil.NEON_GREEN, 1.05f, true);
            }
        }
    }
}
