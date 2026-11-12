package com.dg.client.module.modules.world;

import com.dg.client.DGClient;
import com.dg.client.core.WaypointManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ChatUtil;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Locale;

/**
 * In-world glowing neon green/white 3D waypoint beacons, Overworld <-> Nether 8:1
 * coordinate projection, floating HUD/3D markers, and automatic Death Waypoint capture.
 */
public final class WaypointsModule extends Module {
    public final BooleanSetting renderBeacons = addSetting(new BooleanSetting(
            "Beacon Pillars", "Draws vertical glowing green/white pillars at waypoint locations", true));
    public final BooleanSetting crossDimension = addSetting(new BooleanSetting(
            "Nether 8:1 Projection", "Projects Overworld <-> Nether waypoints automatically", true));
    public final BooleanSetting autoDeathWaypoint = addSetting(new BooleanSetting(
            "Auto Death Waypoint", "Automatically creates a LastDeath waypoint when you die", true));
    public final DoubleSetting labelScale = addSetting(new DoubleSetting(
            "Label Scale", "Floating waypoint label scale", 1.0, 0.5, 2.0, 0.05));

    private boolean recordedCurrentDeath = false;

    public WaypointsModule() {
        super("Waypoints", "Glowing green/white 3D beacons, Nether 8:1 sync & auto death waypoints", ModuleCategory.WORLD);
        setEnabled(true, true);
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(DGClient.getInstance().getWaypointManager().getWaypointsForCurrentServer().size());
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null || !autoDeathWaypoint.getValue()) {
            return;
        }
        if (mc.currentScreen instanceof DeathScreen || mc.player.isDead()) {
            if (!recordedCurrentDeath) {
                recordedCurrentDeath = true;
                double x = mc.player.getX();
                double y = mc.player.getY();
                double z = mc.player.getZ();
                DGClient.getInstance().getWaypointManager().addWaypoint("LastDeath", x, y, z, true);
                DGClient.getInstance().getConfigManager().saveAll();
                ChatUtil.accent("Death Waypoint Saved:", String.format(Locale.ROOT, "X:%.0f Y:%.0f Z:%.0f", x, y, z));
            }
        } else {
            recordedCurrentDeath = false;
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        String activeDim = WaypointManager.getCurrentDimensionId();
        List<WaypointManager.Waypoint> list = DGClient.getInstance().getWaypointManager().getWaypointsForCurrentServer();

        for (WaypointManager.Waypoint wp : list) {
            if (!wp.isEnabled()) {
                continue;
            }
            if (!crossDimension.getValue() && !wp.getDimension().equals(activeDim)) {
                continue;
            }
            Vec3d pos = wp.getPosInDimension(activeDim);
            if (pos == null) {
                continue;
            }

            double dist = mc.player.getEntityPos().distanceTo(pos);
            // Clamp far-away render position to 220m horizon so distant waypoints remain visible!
            Vec3d renderPos = pos;
            if (dist > 220.0) {
                Vec3d dir = pos.subtract(mc.player.getEyePos()).normalize();
                renderPos = mc.player.getEyePos().add(dir.multiply(220.0));
            }

            int primary = wp.isDeathPoint() ? ColorUtil.PURE_WHITE : ColorUtil.NEON_GREEN;
            int accent = wp.isDeathPoint() ? ColorUtil.NEON_GREEN : ColorUtil.PURE_WHITE;

            if (renderBeacons.getValue() && dist <= 260.0) {
                Box pillar = new Box(renderPos.x - 0.25, mc.world.getBottomY(), renderPos.z - 0.25,
                        renderPos.x + 0.25, mc.world.getTopYInclusive(), renderPos.z + 0.25);
                Render3DUtil.drawFilledBox(event.getMatrices(), event.getCamera(), pillar,
                        ColorUtil.withAlpha(primary, 36), true);
                Render3DUtil.drawBoxOutline(event.getMatrices(), event.getCamera(), pillar,
                        1.4f, ColorUtil.withAlpha(accent, 155), true);
            }

            String label = String.format(Locale.ROOT, "%s [%.0fm]", wp.getName(), dist);
            Render3DUtil.drawBillboardLabel(
                    event.getMatrices(),
                    event.getCamera(),
                    new Vec3d(renderPos.x, renderPos.y + 1.8, renderPos.z),
                    label,
                    ColorUtil.PURE_WHITE,
                    primary,
                    labelScale.getValue().floatValue(),
                    true
            );
        }
    }
}
