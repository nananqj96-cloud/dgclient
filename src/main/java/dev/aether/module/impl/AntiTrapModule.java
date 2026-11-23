package dev.aether.module.impl;

import dev.aether.AetherClient;

import java.util.ArrayList;
import java.util.List;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class AntiTrapModule extends Module {
    private static final int SWEEP_INTERVAL_TICKS = 20;
    private final BooleanSetting removeExisting = this.addSetting(new BooleanSetting("Remove Existing", true));
    private final BooleanSetting preventSpawn = this.addSetting(new BooleanSetting("Prevent Spawn", true));
    private final BooleanSetting armorStands = this.addSetting(new BooleanSetting("Armor Stands", true));
    private final BooleanSetting chestMinecarts = this.addSetting(new BooleanSetting("Chest Minecarts", true));
    private final List<Entity> pendingSpawns = new ArrayList<>();

    public AntiTrapModule() {
        super("anti-trap", "Allows you to escape from armor stands and chest minecarts.", Category.DONUT);
        this.usesNotifications();
    }

    @Override
    protected void onEnable() {
        this.pendingSpawns.clear();
        if (this.removeExisting.get()) {
            this.removeTrapEntities();
        }
    }

    @Override
    protected void onDisable() {
        this.pendingSpawns.clear();
    }

//     private void onEntityAdded(WorldEntityAddEvent event) {
//         if (this.preventSpawn.get()) {
//             Entity entity = event.entity;
//             if (this.isTrapEntity(entity)) {
//                 this.pendingSpawns.add(entity);
//             }
//         }
//     }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level != null && player != null) {
            if (!this.pendingSpawns.isEmpty()) {
                for (Entity entity : this.pendingSpawns) {
                    if (!entity.isRemoved() && entity.level() == level) {
                        Entity idHolder = level.getEntity(entity.getId());
                        if ((idHolder == null || idHolder == entity) && this.isTrapEntity(entity) && canRemove(mc, player, entity)) {
                            entity.discard();
                        }
                    }
                }

                this.pendingSpawns.clear();
            }

            if (player.tickCount % 20 == 0) {
                for (Entity entity : this.collectTrapEntities(mc, level, player)) {
                    entity.discard();
                }
            }
        } else {
            this.pendingSpawns.clear();
        }
    }

    private void removeTrapEntities() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null) {
            List<Entity> traps = this.collectTrapEntities(mc, level, mc.player);

            for (Entity entity : traps) {
                entity.discard();
            }

            if (!traps.isEmpty()) {
                AetherClient.chat(
//                     this, ModuleChat.body().good("Removed").text(" ").value(traps.size()).text(traps.size() == 1 ? " trap entity" : " trap entities")
                );
            }
        }
    }

    private List<Entity> collectTrapEntities(Minecraft mc, ClientLevel level, LocalPlayer player) {
        List<Entity> traps = new ArrayList<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (this.isTrapEntity(entity) && canRemove(mc, player, entity)) {
                traps.add(entity);
            }
        }

        return traps;
    }

    private boolean isTrapEntity(Entity entity) {
        if (entity == null) {
            return false;
        }

        EntityType<?> type = entity.getType();
        return this.armorStands.get() && type == EntityType.ARMOR_STAND ? true : this.chestMinecarts.get() && type == EntityType.CHEST_MINECART;
    }

    private static boolean canRemove(Minecraft mc, LocalPlayer player, Entity entity) {
        if (entity == mc.getCameraEntity()) {
            return false;
        } else {
            return player == null ? true : entity != player && !entity.hasIndirectPassenger(player);
        }
    }
}
