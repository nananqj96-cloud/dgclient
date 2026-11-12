package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.ModeSetting;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;

public class SkinProtectModule extends Module {
   public final ModeSetting applyTo = this.addSetting(
      new ModeSetting("Apply To", "Which players use the random skin locally.", "Self", "Everyone", "Others", "Self")
   );
   private volatile PlayerSkin replacement;

   public SkinProtectModule() {
      super("Skin Protect", "Uses a random built-in Minecraft skin locally while enabled.", Category.MISC);
   }

   @Override
   protected void onEnable() {
      this.replacement = DefaultPlayerSkin.get(UUID.randomUUID());
   }

   @Override
   protected void onDisable() {
      this.replacement = null;
   }

   public PlayerSkin replacementSkin() {
      return this.replacement;
   }

   public boolean shouldReplace(UUID var1) {
      if (var1 == null) {
         return false;
      } else {
         Minecraft var3 = Minecraft.getInstance();
         UUID var2 = var3.player == null ? null : var3.player.getUUID();
         if (this.applyTo.is("Everyone")) {
            return true;
         } else {
            return this.applyTo.is("Self") ? var2 != null && var2.equals(var1) : var2 == null || !var2.equals(var1);
         }
      }
   }
}
