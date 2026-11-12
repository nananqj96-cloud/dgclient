package com.dg.client.module.modules.movement;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Smooth detached 3D camera that passes cleanly through blocks without sending
 * any illegal movement packets to the server. Draws a glowing green/white box
 * around the real player body while detached.
 */
public final class FreecamModule extends Module {
    public final DoubleSetting horizontalSpeed = addSetting(new DoubleSetting(
            "Horizontal Speed", "Freecam horizontal fly speed", 1.15, 0.15, 5.0, 0.05));
    public final DoubleSetting verticalSpeed = addSetting(new DoubleSetting(
            "Vertical Speed", "Freecam ascend/descend speed", 0.85, 0.15, 4.0, 0.05));
    public final BooleanSetting showPlayerBodyBox = addSetting(new BooleanSetting(
            "Highlight Body", "Draws a glowing green/white box at your real server position", true));

    private Vec3d prevCamPos = Vec3d.ZERO;
    private Vec3d camPos = Vec3d.ZERO;
    private float camYaw = 0.0f;
    private float camPitch = 0.0f;

    public FreecamModule() {
        super("Freecam", "Smooth detached spectator camera with zero illegal movement packets", ModuleCategory.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            camPos = mc.player.getEyePos();
            prevCamPos = camPos;
            camYaw = mc.player.getYaw();
            camPitch = mc.player.getPitch();
        }
    }

    public Vec3d getInterpolatedPos(float tickDelta) {
        return new Vec3d(
                MathHelper.lerp(tickDelta, prevCamPos.x, camPos.x),
                MathHelper.lerp(tickDelta, prevCamPos.y, camPos.y),
                MathHelper.lerp(tickDelta, prevCamPos.z, camPos.z)
        );
    }

    public float getCamYaw() {
        return camYaw;
    }

    public float getCamPitch() {
        return camPitch;
    }

    public void addLookDelta(double cursorDeltaX, double cursorDeltaY) {
        camYaw += (float) (cursorDeltaX * 0.15);
        camPitch = MathHelper.clamp(camPitch + (float) (cursorDeltaY * 0.15), -90.0f, 90.0f);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.options == null) {
            return;
        }
        prevCamPos = camPos;

        // Keep real player stationary client-side while freecam is active
        mc.player.setVelocity(Vec3d.ZERO);

        if (mc.currentScreen != null) {
            return;
        }

        double forward = 0.0;
        double strafe = 0.0;
        double vertical = 0.0;
        if (mc.options.forwardKey.isPressed()) forward += 1.0;
        if (mc.options.backKey.isPressed()) forward -= 1.0;
        if (mc.options.leftKey.isPressed()) strafe += 1.0;
        if (mc.options.rightKey.isPressed()) strafe -= 1.0;
        if (mc.options.jumpKey.isPressed()) vertical += 1.0;
        if (mc.options.sneakKey.isPressed()) vertical -= 1.0;

        double hSpeed = horizontalSpeed.getValue() * (mc.options.sprintKey.isPressed() ? 1.85 : 1.0);
        double rad = Math.toRadians(camYaw);
        double sin = -Math.sin(rad);
        double cos = Math.cos(rad);

        double dx = (forward * sin + strafe * cos) * hSpeed;
        double dz = (forward * cos - strafe * sin) * hSpeed;
        double dy = vertical * verticalSpeed.getValue();

        camPos = camPos.add(dx, dy, dz);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        // Suppress movement packets that would move the real body while user steers freecam
        if (event.getPacket() instanceof PlayerMoveC2SPacket move && move.changesPosition()) {
            event.cancel();
        } else if (event.getPacket() instanceof PlayerInputC2SPacket) {
            event.cancel();
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!showPlayerBodyBox.getValue() || mc.player == null) {
            return;
        }
        Render3DUtil.drawGlowingBox(
                event.getMatrices(),
                event.getCamera(),
                mc.player.getBoundingBox(),
                ColorUtil.NEON_GREEN,
                ColorUtil.PURE_WHITE,
                0.85f,
                true
        );
    }
}
