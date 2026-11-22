package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityBlockInSelfDamageModule extends Module {
   public final BooleanSetting block = this.addSetting(new BooleanSetting("Block", "Block self-damage", true));

   public TenacityBlockInSelfDamageModule() {
      super("BlockInSelfDamage", "Prevents taking damage from your own actions", Category.PLAYER);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      // Self-damage blocking handled via mixin
   }
}
