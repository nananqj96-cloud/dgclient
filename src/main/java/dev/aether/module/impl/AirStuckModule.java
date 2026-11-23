package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class AirStuckModule extends Module {
    public static volatile AirStuckModule active;

    public AirStuckModule() {
        super("air-stuck", "Freezes you in place, even mid-air.", Category.MOVEMENT);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        active = null;
    }

//     private void onPacketSend(PacketSendEvent event) {
        if (event.packet instanceof ServerboundClientTickEndPacket || event.packet instanceof ServerboundMovePlayerPacket) {
            event.setCancelled(true);
        }
    }
}
