package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;

public class DonutNoFallModule extends Module {
    private static final double TRIGGER_FALL_DISTANCE = 3.0;
    private static final int GROUND_PACKETS_TO_RESET = 4;
    private boolean noFall;
    private double lastFallDistance;
    private int groundTicks;
    private boolean sendingReplacement;

    public DonutNoFallModule() {
        super("donut-nofall", "Take no fall damage on DonutSMP.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        this.noFall = false;
        this.lastFallDistance = 0.0;
        this.groundTicks = 0;
    }

//     @EventHandler
    public void onTick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            if (this.noFall) {
                player.setOnGround(false);
            }

            if (this.lastFallDistance > 3.0) {
                this.noFall = true;
            }

            this.lastFallDistance = player.fallDistance;
        }
    }

//     @EventHandler
//     public void onPacketSend(PacketSendEvent event) {
        if (!this.sendingReplacement && event.packet instanceof ServerboundMovePlayerPacket move) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            ClientPacketListener connection = mc.getConnection();
            if (player != null && connection != null) {
                event.setCancelled(true);
                if (this.noFall && move.isOnGround()) {
                    this.groundTicks++;
                }

                if (this.groundTicks % 2 == 1) {
                    player.setPosRaw(player.getX(), player.getY() + 1.0E-8, player.getZ());
                }

                boolean onGround = !this.noFall && player.onGround();
                this.sendingReplacement = true;

                try {
                    connection.send(new PosRot(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(), onGround, false));
                } finally {
                    this.sendingReplacement = false;
                }

                if (this.groundTicks == 4) {
                    this.noFall = false;
                    this.groundTicks = 0;
                }
            }
        }
    }
}
