package dev.aether.module.impl;

import dev.aether.mixin.MinecraftAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class FastUseModule extends Module {
   public final ModeSetting items = this.addSetting(
      new ModeSetting(
         "Items",
         "What to speed up",
         "All",
         "All",
         "Pearls",
         "XP Bottles"
      )
   );

   public FastUseModule() {
      super("FastUse", "Removes item use cooldowns", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && this.appliesTo(mc)) {
         ((MinecraftAccessor)mc).aetherclient$setRightClickDelay(0);
      }
   }

   private boolean appliesTo(Minecraft mc) {
      if (this.items.is("All")) {
         return true;
      } else {
         Item target = this.items.is("Pearls") ? Items.ENDER_PEARL : Items.EXPERIENCE_BOTTLE;
         return mc.player.getMainHandItem().is(target) || mc.player.getOffhandItem().is(target);
      }
   }
}
