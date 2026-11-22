package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityQuickBuyModule extends Module {
   public final SliderSetting delay = this.addSetting(new SliderSetting("Delay", "Buy delay ms", 100.0, 50.0, 500.0, 50.0, "ms"));
   public final BooleanSetting autoBuy = this.addSetting(new BooleanSetting("AutoBuy", "Auto-buy items on Hypixel", true));

   private long lastBuy;

   public TenacityQuickBuyModule() {
      super("QuickBuy", "Quickly purchases items on Hypixel", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      if (autoBuy.get() && System.currentTimeMillis() - lastBuy > delay.getInt()) {
         lastBuy = System.currentTimeMillis();
         // Quick buy handled via command mixin
      }
   }
}
