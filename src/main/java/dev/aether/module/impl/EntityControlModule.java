package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

public class EntityControlModule extends Module {
    public static volatile EntityControlModule active;
    private final BooleanSetting boats = this.addSetting(new BooleanSetting("Boats", true));
    private final BooleanSetting horses = this.addSetting(new BooleanSetting("Horses", true));
    private final BooleanSetting camels = this.addSetting(new BooleanSetting("Camels", true));
    private final BooleanSetting pigs = this.addSetting(new BooleanSetting("Pigs", true));
    private final BooleanSetting striders = this.addSetting(new BooleanSetting("Striders", true));
    private final BooleanSetting nautiluses = this.addSetting(new BooleanSetting("Nautiluses", true));
    private final BooleanSetting happyGhasts = this.addSetting(new BooleanSetting("Happy Ghasts", true));
    private final BooleanSetting spoofSaddle = this.addSetting(new BooleanSetting("Spoof Saddle", false));
    private final BooleanSetting maxJump = this.addSetting(new BooleanSetting("Max Jump", true));
    private final BooleanSetting lockYaw = this.addSetting(new BooleanSetting("Lock Yaw", true));
    private final BooleanSetting cancelServerPackets = this.addSetting(new BooleanSetting("Cancel Server Packets", false));

    public EntityControlModule() {
        super("entity-control", "Lets you control rideable entities without a saddle.", Category.MOVEMENT);
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

//     private void onPacketReceive(PacketReceiveNettyEvent event) {
        // if (this.cancelServerPackets.get() && event.packet instanceof ClientboundMoveVehiclePacket) {
            // event.setCancelled(true);
        // }
    // }

    public static boolean spoofSaddle() {
        EntityControlModule module = active;
        return module != null && module.spoofSaddle.get();
    }

    public static boolean maxJump() {
        EntityControlModule module = active;
        return module != null && module.maxJump.get();
    }

    public boolean locksYaw(Entity vehicle) {
        return this.lockYaw.get() && this.isTarget(vehicle);
    }

    private boolean isTarget(Entity entity) {
        if (entity instanceof AbstractBoat) {
            return this.boats.get();
        } else {
            EntityType<?> type = entity.getType();
            if (type == EntityType.HORSE
                || type == EntityType.DONKEY
                || type == EntityType.MULE
                || type == EntityType.SKELETON_HORSE
                || type == EntityType.ZOMBIE_HORSE) {
                return this.horses.get();
            } else if (type == EntityType.CAMEL || type == EntityType.CAMEL_HUSK) {
                return this.camels.get();
            } else if (type == EntityType.PIG) {
                return this.pigs.get();
            } else if (type == EntityType.STRIDER) {
                return this.striders.get();
            } else if (type == EntityType.NAUTILUS || type == EntityType.ZOMBIE_NAUTILUS) {
                return this.nautiluses.get();
            } else {
                return type == EntityType.HAPPY_GHAST ? this.happyGhasts.get() : false;
            }
        }
    }
}
