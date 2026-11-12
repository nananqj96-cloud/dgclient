package com.dg.client.module.modules.misc;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.EntityEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Auto-Log & Logout Spots:
 * - Safely disconnects on Low Health, Low Totems, or Unknown Player in range (disables AutoReconnect too!)
 * - Renders glowing green/white Logout Spot boxes where players disconnected!
 */
public final class AutoLogModule extends Module {
    public record LogoutSpot(String name, Box box, Vec3d labelPos, long timeMs) {
    }

    public final BooleanSetting logOnHealth = addSetting(new BooleanSetting(
            "Disconnect Low HP", "Disconnects when health falls at or below Health Threshold", true));
    public final DoubleSetting minHealth = addSetting(new DoubleSetting(
            "Health Threshold", "HP trigger for emergency disconnect", 6.0, 2.0, 18.0, 0.5)
            .visibleWhen(logOnHealth::getValue));
    public final BooleanSetting logOnTotems = addSetting(new BooleanSetting(
            "Disconnect Low Totems", "Disconnects when remaining Totems <= Totem Threshold", false));
    public final IntSetting minTotems = addSetting(new IntSetting(
            "Min Totems", "Totem count trigger", 1, 0, 5, 1)
            .visibleWhen(logOnTotems::getValue));
    public final BooleanSetting logOnStranger = addSetting(new BooleanSetting(
            "Disconnect on Stranger", "Disconnects immediately if a non-friend player enters range", false));
    public final BooleanSetting renderLogoutSpots = addSetting(new BooleanSetting(
            "Render Logout Spots", "Highlights where nearby players logged out in glowing green/white", true));

    private final List<LogoutSpot> logoutSpots = new CopyOnWriteArrayList<>();

    public AutoLogModule() {
        super("AutoLog", "Emergency low-HP/totem/stranger disconnect & glowing Logout Spots", ModuleCategory.MISC);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        // Grace period right after joining so player isn't trapped in disconnect loop
        if (mc.player.age < 40) {
            return;
        }

        if (logOnHealth.getValue() && mc.player.getHealth() <= minHealth.getValue().floatValue()) {
            triggerDisconnect("AutoLog: Low Health (" + String.format(Locale.ROOT, "%.1f HP", mc.player.getHealth()) + ")");
            return;
        }

        if (logOnTotems.getValue()) {
            int totems = countTotems();
            if (totems <= minTotems.getValue()) {
                triggerDisconnect("AutoLog: Remaining Totems = " + totems);
                return;
            }
        }

        if (logOnStranger.getValue()) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                String name = p.getName().getString();
                if (!DGClient.getInstance().getFriendManager().isFriend(name)) {
                    triggerDisconnect("AutoLog: Unknown player '" + name + "' entered range!");
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEntityRemove(EntityEvent.Remove event) {
        if (!renderLogoutSpots.getValue() || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (event.getEntity() instanceof PlayerEntity p && p != mc.player) {
            // If removed from world AND no longer in tab list (or >0 health), record logout spot
            if (p.getHealth() > 0.0f && mc.player.squaredDistanceTo(p) < 200.0 * 200.0) {
                String name = p.getName().getString();
                logoutSpots.removeIf(s -> s.name().equalsIgnoreCase(name));
                logoutSpots.add(new LogoutSpot(
                        name,
                        p.getBoundingBox(),
                        p.getEntityPos().add(0, p.getHeight() + 0.45, 0),
                        System.currentTimeMillis()
                ));
            }
        }
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (!renderLogoutSpots.getValue() || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        logoutSpots.removeIf(s -> now - s.timeMs() > 600_000L);
        for (LogoutSpot spot : logoutSpots) {
            Render3DUtil.drawGlowingBox(event.getMatrices(), event.getCamera(), spot.box(),
                    ColorUtil.NEON_GREEN, ColorUtil.PURE_WHITE, 0.85f, true);
            long agoSec = (now - spot.timeMs()) / 1000L;
            String label = String.format(Locale.ROOT, "%s [Logout %ds]", spot.name(), agoSec);
            Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(), spot.labelPos(),
                    label, ColorUtil.PURE_WHITE, ColorUtil.NEON_GREEN, 0.9f, true);
        }
    }

    private void triggerDisconnect(String reason) {
        // Disable AutoReconnect & AutoLog so player isn't reconnected into danger
        AutoReconnectModule ar = DGClient.getInstance().getModuleManager().get(AutoReconnectModule.class);
        if (ar != null && ar.isEnabled()) {
            ar.setEnabled(false, true);
        }
        setEnabled(false, true);
        if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection() != null) {
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("[DGClient] " + reason));
        }
    }

    private int countTotems() {
        int sum = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                sum += mc.player.getInventory().getStack(i).getCount();
            }
        }
        return sum;
    }
}
