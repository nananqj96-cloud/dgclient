package dev.aether.gui.picker;

import dev.aether.settings.ColorSetting;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface PickerGrid {
   String title();

   long activeCount();

   List<PickerGrid.Cell> cells();

   public interface Cell {
      ItemStack icon();

      String label();

      boolean matches(String string);

      boolean tracked();

      boolean enabled();

      boolean selected();

      int color();

      void toggle();

      ColorSetting colorTarget();
   }
}
