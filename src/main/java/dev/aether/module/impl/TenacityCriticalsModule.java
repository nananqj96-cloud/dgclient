package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TenacityCriticalsModule extends Module {
   public final SliderSetting packets = this.addSetting(new SliderSetting("Packets", "Fall damage packets", 2.0, 1.0, 10.0, 1.0));
   public final BooleanSetting strict = this.addSetting(new BooleanSetting("Strict", "Require falling state", true));
   public final BooleanSetting onGround = this.addSetting(new BooleanSetting("On Ground", "Trigger when landing", false));

   private int counter;

   public TenacityCriticalsModule() {
      super("Criticals", "Inflicts extra fall damage on targets", Category.COMBAT);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      Player p = mc.player;
      if (p.isOnGround()) {
         if (onGround.get()) {
            counter = packets.getInt();
         } else {
            counter = 0;
         }
      } else if (!p.isOnGround() && !strict.get()) {
         counter = packets.getInt();
      }
      if (counter > 0) {
         counter--;
         p.fallDistance += 0.1F;
      }
   }
}
