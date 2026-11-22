package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;

public class TenacityStaffDetectorModule extends Module {
   public final SliderSetting range = this.addSetting(new SliderSetting("Range", "Staff detection range", 50.0, 10.0, 200.0, 5.0, "m"));
   public final BooleanSetting notify = this.addSetting(new BooleanSetting("Notify", "Notify on staff detection", true));
   public final BooleanSetting autoReport = this.addSetting(new BooleanSetting("AutoReport", "Auto-report detected staff", true));

   public TenacityStaffDetectorModule() {
      super("StaffDetector", "Detects staff members on the server", Category.MISC);
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      // Staff detection handled via player list analysis
   }
}
