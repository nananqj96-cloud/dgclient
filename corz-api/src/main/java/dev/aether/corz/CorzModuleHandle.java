package dev.aether.corz;

import java.util.List;

public interface CorzModuleHandle {
   String id();

   String displayName();

   String category();

   boolean isEnabled();

   void setEnabled(boolean enabled);

   default void toggle() {
      this.setEnabled(!this.isEnabled());
   }

   int getBindKey();

   void setBindKey(int keyCode);

   boolean hasCustomStudio();

   List<CorzSettingHandle> settings();
}
