package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.staff.StaffEntry;
import dev.aether.staff.StaffTracker;
import java.util.List;

public class StaffListModule extends Module {
   public final BooleanSetting showRank = new BooleanSetting("Show Rank", "", false);
   public final BooleanSetting showPing = new BooleanSetting("Show Ping", "", false);
   public final SliderSetting maxRows = new SliderSetting("Max Rows", "", 6.0, 6.0, 6.0, 1.0);
   public final StaffTracker tracker = new StaffTracker(this);

   public StaffListModule() {
      super("StaffList", "Automatically lists online players with the confirmed green star badge.", Category.MISC);
   }

   @Override
   protected void onEnable() {
      this.tracker.reset();
   }

   @Override
   protected void onDisable() {
      this.tracker.clear();
   }

   @Override
   public void onTick() {
      this.tracker.tick();
   }

   public List<StaffEntry> staff() {
      return this.tracker.current();
   }
}
