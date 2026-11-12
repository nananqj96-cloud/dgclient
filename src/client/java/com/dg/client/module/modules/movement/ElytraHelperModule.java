package com.dg.client.module.modules.movement;

import com.dg.client.DGClient;
import com.dg.client.core.NotificationManager;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.IntSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;

/**
 * Elytra Helper: auto-deploy on fall, cruise pitch lock, auto firework rocket boost,
 * and low-durability / steep-descent landing warnings.
 */
public final class ElytraHelperModule extends Module {
    public final BooleanSetting autoDeploy = addSetting(new BooleanSetting(
            "Auto Deploy", "Automatically opens Elytra when falling with chestplate Elytra equipped", true));
    public final BooleanSetting pitchLock = addSetting(new BooleanSetting(
            "Cruise Pitch Lock", "Smoothly stabilizes pitch for long-distance highway cruising", false));
    public final DoubleSetting targetPitch = addSetting(new DoubleSetting(
            "Target Pitch", "Cruise pitch angle in degrees (-2.0 = level glide)", -1.8, -45.0, 45.0, 0.2)
            .visibleWhen(pitchLock::getValue));
    public final BooleanSetting fireworkAssist = addSetting(new BooleanSetting(
            "Auto Firework", "Uses a hotbar firework rocket when flight speed drops below threshold", false));
    public final DoubleSetting minSpeedBps = addSetting(new DoubleSetting(
            "Min Speed (m/s)", "Minimum horizontal speed before firing a boost rocket", 17.5, 8.0, 32.0, 0.5)
            .visibleWhen(fireworkAssist::getValue));
    public final IntSetting fireworkCooldownTicks = addSetting(new IntSetting(
            "Rocket Cooldown (t)", "Minimum ticks between automatic rockets", 45, 15, 160, 5)
            .visibleWhen(fireworkAssist::getValue));
    public final BooleanSetting landingWarning = addSetting(new BooleanSetting(
            "Safety Warnings", "Warns on low Elytra durability or dangerous downward impact speed", true));

    private int rocketCooldown = 0;
    private int warningCooldown = 0;

    public ElytraHelperModule() {
        super("ElytraHelper", "Auto-deploy, cruise pitch assist, rocket boost & durability alerts", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getDisplayInfo() {
        if (mc.player != null && mc.player.isGliding()) {
            double bps = mc.player.getVelocity().horizontalLength() * 20.0;
            return String.format(java.util.Locale.ROOT, "%.1fm/s", bps);
        }
        return pitchLock.getValue() ? "Cruise" : null;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            return;
        }
        if (rocketCooldown > 0) rocketCooldown--;
        if (warningCooldown > 0) warningCooldown--;

        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean wearingElytra = chest.isOf(Items.ELYTRA) && chest.getMaxDamage() - chest.getDamage() > 1;
        if (!wearingElytra) {
            return;
        }

        // Auto-deploy when airborne & falling
        if (autoDeploy.getValue()
                && !mc.player.isGliding()
                && !mc.player.isOnGround()
                && !mc.player.isTouchingWater()
                && mc.player.fallDistance > 1.35f
                && mc.getNetworkHandler() != null) {
            mc.player.startFallFlying();
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }

        if (!mc.player.isGliding()) {
            return;
        }

        if (pitchLock.getValue()) {
            float desired = targetPitch.getValue().floatValue();
            float cur = mc.player.getPitch();
            mc.player.setPitch(cur + (desired - cur) * 0.35f);
        }

        if (fireworkAssist.getValue() && rocketCooldown == 0) {
            double speedBps = mc.player.getVelocity().horizontalLength() * 20.0;
            if (speedBps < minSpeedBps.getValue()) {
                useFireworkFromHotbar();
                rocketCooldown = fireworkCooldownTicks.getValue();
            }
        }

        if (landingWarning.getValue() && warningCooldown == 0) {
            int remainingDur = chest.getMaxDamage() - chest.getDamage();
            if (remainingDur <= 18) {
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        "Elytra Durability Critical!",
                        remainingDur + " durability remaining!"
                );
                warningCooldown = 100;
            } else if (mc.player.getVelocity().y < -0.95 && mc.player.getY() < mc.world.getBottomY() + 18) {
                DGClient.getInstance().getNotificationManager().push(
                        NotificationManager.Type.WARNING,
                        "Steep Descent!",
                        "Pull up — high vertical descent velocity!"
                );
                warningCooldown = 60;
            }
        }
    }

    private void useFireworkFromHotbar() {
        if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            return;
        }
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                mc.player.getInventory().setSelectedSlot(i);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(prevSlot);
                return;
            }
        }
    }
}
