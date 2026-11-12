package dev.aether.module.impl;

import dev.aether.mixin.MinecraftAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class AutoClickerModule extends Module {
   public final BooleanSetting inScreens = this.addSetting(
      new BooleanSetting("While In Screens", "Whether to click while a screen is open.", true)
   );
   public final ModeSetting leftMode = this.addSetting(
      new ModeSetting(
         "Left Click Mode",
         "The method of clicking for left clicks.",
         "Press",
         "Disabled",
         "Hold",
         "Press"
      )
   );
   public final SliderSetting leftDelay = this.addSetting(
      new SliderSetting(
         "Left Click Delay",
         "Delay between left clicks in ticks.",
         2.0,
         0.0,
         60.0,
         1.0,
         " ticks"
      )
   );
   public final ModeSetting rightMode = this.addSetting(
      new ModeSetting(
         "Right Click Mode",
         "The method of clicking for right clicks.",
         "Press",
         "Disabled",
         "Hold",
         "Press"
      )
   );
   public final SliderSetting rightDelay = this.addSetting(
      new SliderSetting(
         "Right Click Delay",
         "Delay between right clicks in ticks.",
         2.0,
         0.0,
         60.0,
         1.0,
         " ticks"
      )
   );
   private int leftTimer;
   private int rightTimer;

   public AutoClickerModule() {
      super("AutoClicker", "Automatically clicks.", Category.MISC);
      this.leftDelay.visibleWhen(() -> this.leftMode.is("Press"));
      this.rightDelay.visibleWhen(() -> this.rightMode.is("Press"));
   }

   @Override
   protected void onEnable() {
      this.leftTimer = 0;
      this.rightTimer = 0;
      this.release();
   }

   @Override
   protected void onDisable() {
      this.release();
   }

   private void release() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.options != null) {
         mc.options.keyAttack.setDown(false);
         mc.options.keyUse.setDown(false);
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && (this.inScreens.get() || mc.screen == null)) {
         String var2 = this.leftMode.get();
         switch (var2) {
            case "Hold":
               mc.options.keyAttack.setDown(true);
               break;
            case "Press":
               this.leftTimer++;
               if (this.leftTimer > this.leftDelay.getInt()) {
                  this.leftClick(mc);
                  this.leftTimer = 0;
               }
         }

         var2 = this.rightMode.get();
         switch (var2) {
            case "Hold":
               mc.options.keyUse.setDown(true);
               break;
            case "Press":
               this.rightTimer++;
               if (this.rightTimer > this.rightDelay.getInt()) {
                  this.rightClick(mc);
                  this.rightTimer = 0;
               }
         }
      }
   }

   private void leftClick(Minecraft mc) {
      if (mc.missTime == 10000) {
         mc.missTime = 0;
      }

      mc.options.keyAttack.setDown(true);
      ((MinecraftAccessor)mc).aetherclient$startAttack();
      mc.options.keyAttack.setDown(false);
   }

   private void rightClick(Minecraft mc) {
      ((MinecraftAccessor)mc).aetherclient$startUseItem();
   }
}
