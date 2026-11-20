package dev.aether.corz;

import java.util.List;

public interface CorzPortProvider {
   List<String> categories();

   List<CorzModuleHandle> allModules();

   default List<CorzModuleHandle> modules() {
      return this.allModules();
   }

   List<CorzModuleHandle> modulesInCategory(String category);

   void syncTheme(int accentArgb, int accentBrightArgb, int backgroundArgb);

   void openNativeStudio(String moduleId);

   void saveConfig();
}
