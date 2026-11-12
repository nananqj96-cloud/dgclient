package com.dg.client.module.modules.movement;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.setting.EnumSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;

/**
 * Clearly labeled NoFall modes:
 * - PACKET_GROUND: sends onGround=true packet when fallDistance > 2.8 (classic anarchy servers)
 * - ELYTRA_GLIDE: briefly deploys equipped Elytra before ground impact
 * - MLG_BUCKET: switches to hotbar Water Bucket / Powder Snow when ground raycast is imminent
 */
public final class NoFallModule extends Module {
    public enum Mode {
        PACKET_GROUND,
        ELYTRA_GLIDE,
        MLG_BUCKET
    }

    public final EnumSetting<Mode> mode = addSetting(new EnumSetting<>(
            "Mode", "PACKET_GROUND (vanilla/anarchy), ELYTRA_GLIDE, or MLG_BUCKET assist", Mode.PACKET_GROUND));

    public NoFallModule() {
        super("NoFall", "Mitigates fall damage via packet ground, elytra flare, or MLG water assist", ModuleCategory.MOVEMENT);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (mc.player.isGliding() || mc.player.getAbilities().flying || mc.player.fallDistance <= 2.85f) {
            return;
        }

        switch (mode.getValue()) {
            case PACKET_GROUND -> {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
                mc.player.fallDistance = 0.0f;
            }
            case ELYTRA_GLIDE -> {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(
                            mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                }
            }
            case MLG_BUCKET -> {
                if (mc.world.getDimension().ultrawarm() || mc.interactionManager == null) {
                    return;
                }
                HitResult hit = mc.player.raycast(4.2, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) {
                            int prev = mc.player.getInventory().getSelectedSlot();
                            float prevPitch = mc.player.getPitch();
                            mc.player.getInventory().setSelectedSlot(i);
                            mc.player.setPitch(90.0f);
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            mc.player.setPitch(prevPitch);
                            mc.player.getInventory().setSelectedSlot(prev);
                            break;
                        }
                    }
                }
            }
        }
    }
}
