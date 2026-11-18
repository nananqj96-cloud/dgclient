package dev.aether.module.impl;

import dev.aether.mixin.MinecraftAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/**
 * Reduces the block-placement cooldown so you can place blocks faster than vanilla. The
 * anticheat-safe mode keeps the cooldown above the suspicious threshold.
 */
public class FastPlaceModule extends Module {
   public final SliderSetting cooldown = this.addSetting(
      new SliderSetting("Cooldown", "Place cooldown in ticks (lower = faster)", 4.0, 1.0, 8.0, 1.0, "t")
   );
   public final BooleanSetting anticheatSafe = this.addSetting(
      new BooleanSetting("Anticheat Safe", "Keep cooldown above the suspicious threshold", true)
   );
   public final BooleanSetting blocksOnly = this.addSetting(
      new BooleanSetting("Blocks Only", "Only speed up placing block items", true)
   );

   private int placeCooldown;

   public FastPlaceModule() {
      super("Fast Place", "Place blocks faster than vanilla", Category.MISC);
      this.placeCooldown = 4;
   }

   public int cooldownValue() {
      int c = (int) this.cooldown.get();
      if (this.anticheatSafe.get() && c < 2) {
         // Randomize between 2-4 to look less suspicious.
         c = java.util.concurrent.ThreadLocalRandom.current().nextInt(2, 5);
      }
      return c;
   }

   public boolean blocksOnly() {
      return this.blocksOnly.get();
   }

   public void setCooldown(int c) {
      this.placeCooldown = c;
   }

   public int getCooldown() {
      return this.placeCooldown;
   }

   @Override
   public void onTick() {
      if (!this.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return;
      }
      // Only speed up block placement (unless Blocks Only is off).
      if (this.blocksOnly.get()) {
         ItemStack main = mc.player.getMainHandItem();
         ItemStack off = mc.player.getOffhandItem();
         if (!(main.getItem() instanceof BlockItem) && !(off.getItem() instanceof BlockItem)) {
            return;
         }
      }
      ((MinecraftAccessor) mc).aetherclient$setRightClickDelay(this.cooldownValue());
   }

   @Override
   protected void onDisable() {
      Minecraft mc = Minecraft.getInstance();
      if (mc != null) {
         ((MinecraftAccessor) mc).aetherclient$setRightClickDelay(4);
      }
   }
}
