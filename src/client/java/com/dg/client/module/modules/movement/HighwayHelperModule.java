package com.dg.client.module.modules.movement;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Highway / Tunnel Helper: 8-way cardinal & diagonal yaw snap, optional mining pitch lock,
 * lateral center alignment assist, and safe-walk edge protection over Nether lava drops.
 */
public final class HighwayHelperModule extends Module {
    public final BooleanSetting snapYaw8Way = addSetting(new BooleanSetting(
            "8-Way Yaw Snap", "Snaps look yaw to nearest 45-degree cardinal/diagonal highway axis", true));
    public final BooleanSetting lockPitch = addSetting(new BooleanSetting(
            "Lock Pitch", "Locks vertical pitch while digging tunnels", false));
    public final DoubleSetting targetPitch = addSetting(new DoubleSetting(
            "Mining Pitch", "Pitch angle when Lock Pitch is active", 12.0, -45.0, 60.0, 1.0)
            .visibleWhen(lockPitch::getValue));
    public final BooleanSetting safeEdgeSneak = addSetting(new BooleanSetting(
            "Safe Edge Guard", "Automatically sneaks at exposed block edges to prevent highway falls", true));

    private boolean forcedSneak = false;

    public HighwayHelperModule() {
        super("HighwayHelper", "8-way highway axis snap, mining pitch lock, and ledge guard", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getDisplayInfo() {
        if (mc.player == null) return "8-Way";
        float snapped = Math.round(MathHelper.wrapDegrees(mc.player.getYaw()) / 45.0f) * 45.0f;
        return String.format(java.util.Locale.ROOT, "%.0f°", snapped);
    }

    @Override
    protected void onDisable() {
        if (forcedSneak && mc.options != null) {
            mc.options.sneakKey.setPressed(false);
            forcedSneak = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (snapYaw8Way.getValue()) {
            float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
            float snapped = Math.round(yaw / 45.0f) * 45.0f;
            mc.player.setYaw(snapped);
        }
        if (lockPitch.getValue()) {
            mc.player.setPitch(targetPitch.getValue().floatValue());
        }

        if (safeEdgeSneak.getValue() && mc.player.isOnGround() && !mc.player.getAbilities().flying) {
            BlockPos belowNext = BlockPos.ofFloored(
                    mc.player.getX() + mc.player.getVelocity().x * 2.2,
                    mc.player.getY() - 0.6,
                    mc.player.getZ() + mc.player.getVelocity().z * 2.2
            );
            boolean airDrop = mc.world.getBlockState(belowNext).isAir()
                    && mc.world.getBlockState(belowNext.down()).isAir();
            if (airDrop) {
                mc.options.sneakKey.setPressed(true);
                forcedSneak = true;
            } else if (forcedSneak) {
                mc.options.sneakKey.setPressed(false);
                forcedSneak = false;
            }
        } else if (forcedSneak) {
            mc.options.sneakKey.setPressed(false);
            forcedSneak = false;
        }
    }
}
