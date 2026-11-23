package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

public class JumpResetModule extends Module {
    private static final double MIN_KNOCKBACK = 0.1;
    private boolean pending;

    public JumpResetModule() {
        super("jump-reset", "Jumps the tick knockback lands to take less of it.", Category.COMBAT);
    }

    @Override
    protected void onDisable() {
        this.pending = false;
    }

//     @EventHandler
//     public void onPacketApplied(PacketAppliedEvent event) {
        // if (event.packet instanceof ClientboundSetEntityMotionPacket motion) {
            // LocalPlayer player = Minecraft.getInstance().player;
            // if (player != null && motion.getId() == player.getId()) {
                // Vec3 knockback = motion.getMovement();
                // if (knockback.horizontalDistance() >= 0.1 && knockback.y > 0.0) {
                    // this.pending = true;
                // }
            // }
        // }
    // }

//     @EventHandler
    public void onTick() {
        if (this.pending) {
            this.pending = false;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player != null && mc.screen == null) {
                if (player.onGround() && player.isSprinting() && player.input.keyPresses.forward()) {
                    if (!player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isPassenger() && !player.isFallFlying()) {
                        InputControl.jump();
                    }
                }
            }
        }
    }
}
