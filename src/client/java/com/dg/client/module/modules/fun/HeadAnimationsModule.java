package com.dg.client.module.modules.fun;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fun Head Animations, Spin, Dinnerbone Flip & Client-Side Fake-Lag Ghost Box:
 * Defaults to 100% client-side visual mode (SAFE — zero packets sent), with a clearly
 * marked optional Server Look-Only Spin toggle.
 */
public final class HeadAnimationsModule extends Module {
    public enum AnimMode {
        CLIENT_SPIN,
        HEAD_NOD,
        DINNERBONE_FLIP,
        FAKE_LAG_GHOST
    }

    public final EnumSetting<AnimMode> mode = addSetting(new EnumSetting<>(
            "Animation Mode", "Visual animation style (strictly client-side by default)", AnimMode.CLIENT_SPIN));
    public final DoubleSetting spinSpeed = addSetting(new DoubleSetting(
            "Spin Speed", "Degrees per tick for spin/nod modes", 18.0, 2.0, 60.0, 1.0));
    public final BooleanSetting serverLookPacket = addSetting(new BooleanSetting(
            "Send Look Packet (Server)", "WARNING: Sends C2S LookAndOnGround rotation (keep OFF for strict servers)", false)
            .visibleWhen(() -> mode.getValue() == AnimMode.CLIENT_SPIN));

    private float spinYaw = 0.0f;
    private final Deque<Vec3d> lagPositions = new ArrayDeque<>();
    private Vec3d displayedGhostPos = null;
    private int ghostUpdateTimer = 0;

    public HeadAnimationsModule() {
        super("HeadAnimations", "Safe client-side spin, nod, Dinnerbone flip & fake-lag ghost visual", ModuleCategory.FUN);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    public boolean shouldFlipSelfUpsideDown() {
        return isEnabled() && mode.getValue() == AnimMode.DINNERBONE_FLIP;
    }

    public float getClientSpinYaw(float tickDelta) {
        return (spinYaw + spinSpeed.getValue().floatValue() * tickDelta) % 360.0f;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) {
            return;
        }
        spinYaw = (spinYaw + spinSpeed.getValue().floatValue()) % 360.0f;

        if (mode.getValue() == AnimMode.HEAD_NOD) {
            float nod = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 55.0);
            mc.player.headYaw = mc.player.getYaw() + nod * 0.35f;
        } else if (mode.getValue() == AnimMode.CLIENT_SPIN) {
            mc.player.bodyYaw = spinYaw;
            mc.player.headYaw = spinYaw;
            if (serverLookPacket.getValue() && mc.getNetworkHandler() != null && !mc.player.isUsingItem()) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(
                        spinYaw, mc.player.getPitch(), mc.player.isOnGround()));
            }
        } else if (mode.getValue() == AnimMode.FAKE_LAG_GHOST) {
            lagPositions.addLast(mc.player.getPos());
            while (lagPositions.size() > 16) {
                lagPositions.removeFirst();
            }
            if (++ghostUpdateTimer >= 12) {
                ghostUpdateTimer = 0;
                displayedGhostPos = lagPositions.peekFirst();
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mode.getValue() != AnimMode.FAKE_LAG_GHOST || displayedGhostPos == null) {
            return;
        }
        double hw = mc.player.getWidth() * 0.5;
        Box ghost = new Box(
                displayedGhostPos.x - hw, displayedGhostPos.y, displayedGhostPos.z - hw,
                displayedGhostPos.x + hw, displayedGhostPos.y + mc.player.getHeight(), displayedGhostPos.z + hw
        );
        Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), ghost,
                ColorUtil.MINT_GREEN, ColorUtil.PURE_WHITE, 0.75f, true);
    }
}
