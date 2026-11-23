package dev.aether.module.impl;

import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

public class ItemEspModule extends Module {
    private static ItemEspModule instance;
    private final SliderSetting sRange = this.addSetting(new SliderSetting("Range", "Range", 128.0, 10.0, 512.0, 1.0F));
    private final BooleanSetting sOutline = this.addSetting(new BooleanSetting("Outline", "Outline", true));
    private final BooleanSetting sTracers = this.addSetting(new BooleanSetting("Tracers", "Tracers", false));
    public ItemEspModule() {
        super("item-esp", "Highlights selected dropped items with model outlines and tracers.", Category.RENDER);
        instance = this;
    }

    public static boolean shouldGlow(Entity entity) {
        ItemEspModule module = instance;
        return module != null && module.isEnabled() && module.sOutline.get() ? entity instanceof ItemEntity item && module.wants(item, 1.0F) : false;
    }

    public static int glowColor(Entity entity) {
        ItemEspModule module = instance;
        return module != null && entity instanceof ItemEntity item ? module.sItemColors.colorArgb(item.getItem().getItem()) & 16777215 : 16777215;
    }

    private boolean wants(ItemEntity item, float partialTick) {
        if (item.isRemoved()) {
            return false;
        }

        Item kind = item.getItem().getItem();
        if (!this.sItemColors.isEnabled(kind)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return true;
        }

        float range = Math.max(1.0F, this.sRange.get());
        return item.getPosition(partialTick).distanceToSqr(mc.player.getEyePosition(partialTick)) <= (double)range * range;
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        // if (this.sTracers.get() && this.sItemColors.hasAnyEnabled()) {
            // Minecraft mc = Minecraft.getInstance();
            // if (mc.level != null) {
                // for (Entity entity : mc.level.entitiesForRendering()) {
                    // if (entity instanceof ItemEntity item && this.wants(item, event.partialTick)) {
                        // Vec3 pos = item.getPosition(event.partialTick);
                        // TracerEngine.draw(pos, this.sItemColors.colorArgb(item.getItem().getItem()));
                    // }
                // }
            // }
        // }
    // }
}
