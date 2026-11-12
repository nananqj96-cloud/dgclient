package com.dg.client.module.modules.movement;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.IntSetting;

/**
 * AutoWalk, AutoSprint, and configurable Anti-AFK micro-actions.
 */
public final class AutoWalkModule extends Module {
    public final BooleanSetting autoWalk = addSetting(new BooleanSetting(
            "Auto Walk", "Holds forward movement key automatically", true));
    public final BooleanSetting autoSprint = addSetting(new BooleanSetting(
            "Auto Sprint", "Automatically sprints whenever moving forward without hunger/blindness", true));
    public final BooleanSetting stopOnScreen = addSetting(new BooleanSetting(
            "Pause in Containers", "Pauses forward walk while an inventory screen is open", false));
    public final BooleanSetting antiAfk = addSetting(new BooleanSetting(
            "Anti-AFK", "Performs periodic subtle yaw/jump/swing actions to prevent idle kicks", false));
    public final IntSetting antiAfkIntervalSec = addSetting(new IntSetting(
            "Anti-AFK Interval (s)", "Seconds between Anti-AFK actions", 25, 5, 120, 5)
            .visibleWhen(antiAfk::getValue));

    private int afkTicks = 0;
    private boolean toggledForward = false;

    public AutoWalkModule() {
        super("AutoWalk", "Hands-free walking, auto-sprint, and Anti-AFK protection", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getDisplayInfo() {
        if (autoWalk.getValue() && antiAfk.getValue()) return "Walk+AFK";
        if (autoWalk.getValue()) return "Walk";
        if (autoSprint.getValue()) return "Sprint";
        return "AFK";
    }

    @Override
    protected void onDisable() {
        if (toggledForward && mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            toggledForward = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (autoWalk.getValue()) {
            boolean allow = !stopOnScreen.getValue() || mc.currentScreen == null;
            mc.options.forwardKey.setPressed(allow);
            toggledForward = allow;
        } else if (toggledForward) {
            mc.options.forwardKey.setPressed(false);
            toggledForward = false;
        }

        if (autoSprint.getValue()
                && mc.player.input != null
                && mc.player.input.movementForward > 0.05f
                && !mc.player.isSneaking()
                && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6) {
            mc.player.setSprinting(true);
        }

        if (antiAfk.getValue()) {
            if (++afkTicks >= antiAfkIntervalSec.getValue() * 20) {
                afkTicks = 0;
                float deltaYaw = ((System.currentTimeMillis() & 1L) == 0L) ? 4.5f : -4.5f;
                mc.player.setYaw(mc.player.getYaw() + deltaYaw);
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
            }
        }
    }
}
