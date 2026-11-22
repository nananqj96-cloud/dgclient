package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.SliderSetting;
import dev.aether.settings.BooleanSetting;
import net.minecraft.client.Minecraft;

public class TenacityKnockbackDisplacementModule extends Module {
   public final SliderSetting displacement = this.addSetting(new SliderSetting("Displacement", "Knockback displacement", 0.5, 0.1, 5.0, 0.1));
   public final BooleanSetting cancel = this.addSetting(new BooleanSetting("Cancel", "Cancel knockback packets", true));

   public TenacityKnockbackDisplacementModule() {
      super("KnockbackDisplacement", "Controls knockback displacement on hit", Category.EXPLOIT);
   }

   @Override
   public void onTick() {
      // Knockback displacement handled via mixin
   }
}
