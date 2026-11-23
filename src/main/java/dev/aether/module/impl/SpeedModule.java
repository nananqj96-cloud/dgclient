package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.phys.Vec3;

public class SpeedModule extends Module {
    public static volatile SpeedModule active;
    public static volatile float timerMultiplier = 1.0F;
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Vanilla", "Strafe", "Vanilla"));
    private final SliderSetting vanillaSpeed = this.addSetting(new SliderSetting("Vanilla Speed", 5.6, 0.0, 20.0, 0.1F));
    private final SliderSetting strafeSpeed = this.addSetting(new SliderSetting("Strafe Speed", 1.6, 0.0, 3.0, 0.1F));
    private final BooleanSetting speedLimit = this.addSetting(new BooleanSetting("Speed Limit", false));
    private final SliderSetting timer = this.addSetting(new SliderSetting("Timer", 1.0, 0.01, 10.0, 0.01F));
    private final BooleanSetting inLiquids = this.addSetting(new BooleanSetting("In Liquids", false));
    private final BooleanSetting whenSneaking = this.addSetting(new BooleanSetting("When Sneaking", false));
    private final BooleanSetting onlyOnGround = this.addSetting(new BooleanSetting("Only On Ground", false));
    private SpeedMode currentMode;
    private volatile boolean rubberband;

    public SpeedModule() {
        super("speed", "Modifies your movement speed when moving on the ground.", Category.MOVEMENT);
        this.vanillaSpeed.visibleWhen(() -> this.mode.is("Vanilla"));
        this.strafeSpeed.visibleWhen(() -> this.mode.is("Strafe"));
        this.speedLimit.visibleWhen(() -> this.mode.is("Strafe"));
        this.onlyOnGround.visibleWhen(() -> this.mode.is("Vanilla"));
        this.currentMode = this.createMode();
    }

    @Override
    protected void onEnable() {
        this.currentMode = this.createMode();
        this.rubberband = false;
        active = this;
        this.currentMode.onActivate();
    }

    @Override
    protected void onDisable() {
        active = null;
        timerMultiplier = 1.0F;
        this.currentMode.onDeactivate();
    }

    private SpeedMode createMode() {
        return this.mode.is("Strafe") ? new StrafeSpeedMode(this) : new VanillaSpeedMode(this);
    }

    public void onTick() {
        if (this.rubberband) {
            this.rubberband = false;
            this.currentMode.onRubberband();
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            timerMultiplier = 1.0F;
        } else if (!this.stopSpeed(player)) {
            this.currentMode.onTick(player);
        }
    }

//     private void onPacketReceive(PacketReceiveNettyEvent event) {
        if (event.packet instanceof ClientboundPlayerPositionPacket) {
            this.rubberband = true;
        } else if (event.packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> packet : bundle.subPackets()) {
                if (packet instanceof ClientboundPlayerPositionPacket) {
                    this.rubberband = true;
                    break;
                }
            }
        }
    }

    public Vec3 onSelfMove(LocalPlayer player, Vec3 movement) {
        if (player == Minecraft.getInstance().player && !this.stopSpeed(player)) {
            float timerValue = this.timer.get();
            timerMultiplier = timerValue != 1.0F && isMoving(player) ? timerValue : 1.0F;
            Vec3 result = this.currentMode.onMove(player, movement);
            if (result != movement && movement == player.getDeltaMovement()) {
                player.setDeltaMovement(result);
            }

            return result;
        } else {
            return movement;
        }
    }

    private boolean stopSpeed(LocalPlayer player) {
        if (player.isFallFlying() || player.onClimbable() || player.getVehicle() != null) {
            return true;
        } else if (!this.whenSneaking.get() && player.isShiftKeyDown()) {
            return true;
        } else {
            return this.onlyOnGround.get() && !player.onGround() && this.mode.is("Vanilla")
                ? true
                : !this.inLiquids.get() && (player.isInWater() || player.isInLava());
        }
    }

    public static boolean isMoving(LocalPlayer player) {
        return player.zza != 0.0F || player.xxa != 0.0F;
    }

    public double vanillaSpeed() {
        return this.vanillaSpeed.get();
    }

    public double strafeSpeed() {
        return this.strafeSpeed.get();
    }

    public boolean speedLimit() {
        return this.speedLimit.get();
    }
}
