package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class JesusModule extends Module {
    public static volatile JesusModule active;
    private static final String SOLID = "Solid";
    private static final String IGNORE = "Ignore";
    private static final double ASCEND_SPEED = 0.11;
    private final ModeSetting mode = this.addSetting(new ModeSetting("Mode", "Mode", "Solid", "Solid", "Ignore", "Packet"));
    private final BooleanSetting ascend = this.addSetting(new BooleanSetting("Ascend", "Ascend", true));
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", "Speed", 0.11, 0.01, 0.5, 0.01F));
    private final BooleanSetting onlyInLiquid = this.addSetting(new BooleanSetting("Only In Liquid", "Only In Liquid", true));
    private final BooleanSetting stopOnLand = this.addSetting(new BooleanSetting("Stop On Land", "Stop On Land", true));

    public JesusModule() {
        super("jesus", "Walks on water and lava.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        Entity vehicle = player.getVehicle();
        boolean inBoat = vehicle instanceof AbstractBoat;
        FluidState fluid = player.level().getFluidState(player.blockPosition());

        boolean inLiquid = !fluid.isEmpty();
        if (!inLiquid && !inBoat) {
            return;
        }

        if (this.onlyInLiquid.get() && !inLiquid) {
            return;
        }

        if (this.stopOnLand.get() && player.onGround()) {
            return;
        }

        if (this.ascend.get() && player.zza != 0.0F) {
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, this.speed.get(), 0.0));
        }
    }
}
