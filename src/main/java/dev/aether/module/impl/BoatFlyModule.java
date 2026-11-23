package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.PlayerRideableJumping;

public class BoatFlyModule extends Module {
    public static volatile BoatFlyModule active;
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", 10.0, 0.0, 50.0, 0.5F));
    private final SliderSetting verticalSpeed = this.addSetting(new SliderSetting("Vertical Speed", 6.0, 0.0, 20.0, 0.5F));
    private final SliderSetting fallSpeed = this.addSetting(new SliderSetting("Fall Speed", 0.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting antiKick = this.addSetting(new BooleanSetting("Anti Fly Kick", true));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 40.0, 1.0, 80.0, 1.0F));
    private final BooleanSetting cancelServerPackets = this.addSetting(new BooleanSetting("Cancel Server Packets", false));
    private final VehicleAntiKick antiKicker = new VehicleAntiKick();

    public BoatFlyModule() {
        super("boat-fly", "Transforms your boat (or any ridden vehicle) into a plane.", Category.MOVEMENT);
        this.delay.visibleWhen(this.antiKick::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
//         BoatNoclipModule noclip = ModuleRegistry.get(BoatNoclipModule.class);
        if (noclip != null) {
            noclip.setEnabled(false);
        }

        this.antiKicker.start(this.delay.getInt());
        active = this;
        EntityControlVehicleHook.refresh();
    }

    @Override
    protected void onDisable() {
        active = null;
        EntityControlVehicleHook.refresh();
        this.antiKicker.stop();
    }

//     private void onPacketSend(PacketSendEvent event) {
        if (this.antiKick.get() && event.packet instanceof ServerboundMoveVehiclePacket packet && this.antiKicker.onPacketSend(packet, this.delay.getInt())) {
            event.setCancelled(true);
        }
    }

//     private void onPacketReceive(PacketReceiveNettyEvent event) {
        if (this.cancelServerPackets.get() && event.packet instanceof ClientboundMoveVehiclePacket) {
            event.setCancelled(true);
        }
    }

    public void onVehicleSetback() {
        this.antiKicker.forget();
    }

    public double horizontalSpeed() {
        return this.speed.get();
    }

    public double verticalVelocity() {
        Minecraft mc = Minecraft.getInstance();
        double velY = 0.0;
        if (EntityControlVehicleHook.isPhysicallyDown(mc.options.keyJump)) {
            velY += this.verticalSpeed.get() / 20.0;
        }

        if (EntityControlVehicleHook.isPhysicallyDown(mc.options.keySprint)) {
            velY -= this.verticalSpeed.get() / 20.0;
        } else {
            velY -= this.fallSpeed.get() / 20.0;
        }

        return velY;
    }

    public static boolean cancelsMountJump(LocalPlayer player) {
        return active != null && player.getVehicle() instanceof PlayerRideableJumping;
    }
}
