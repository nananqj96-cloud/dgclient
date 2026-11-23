package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.world.entity.Entity;

public class EntitySpeedModule extends Module {
    public static volatile EntitySpeedModule active;
    private final SliderSetting horizontalSpeed = this.addSetting(new SliderSetting("Horizontal Speed", 10.0, 0.0, 50.0, 0.5F));
    private final BooleanSetting onlyOnGround = this.addSetting(new BooleanSetting("Only On Ground", false));
    private final BooleanSetting inWater = this.addSetting(new BooleanSetting("In Water", true));

    public EntitySpeedModule() {
        super("entity-speed", "Makes you go faster horizontally when riding entities.", Category.MOVEMENT);
    }

    @Override
    protected void onEnable() {
        active = this;
        EntityControlVehicleHook.refresh();
    }

    @Override
    protected void onDisable() {
        active = null;
        EntityControlVehicleHook.refresh();
    }

    public boolean appliesTo(Entity vehicle) {
        return (!this.onlyOnGround.get() || vehicle.onGround() || vehicle.isFlyingVehicle()) && (this.inWater.get() || !vehicle.isInWater());
    }

    public double horizontalSpeed() {
        return this.horizontalSpeed.get();
    }
}
