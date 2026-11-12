package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;

public final class PanoramaModule extends Module {
   private static final String PACK_FILE = "aether-panorama.zip";
   private static final String EMBEDDED_PACK = "/assets/aetherclient/embedded/aether-panorama.zip";

   public PanoramaModule() {
      super("Panorama", "Uses the bundled Aether menu panorama while enabled.", Category.RENDER);
   }

   @Override
   protected void onEnable() {
      this.apply(true);
   }

   @Override
   protected void onDisable() {
      this.apply(false);
   }

   private void apply(boolean bl) {
      try {
         Minecraft var2 = Minecraft.getInstance();
         PackRepository var3 = var2.getResourcePackRepository();
         if (bl) {
            Path var4 = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
            Files.createDirectories(var4);

            try (InputStream var5 = PanoramaModule.class.getResourceAsStream("/assets/aetherclient/embedded/aether-panorama.zip")) {
               if (var5 == null) {
                  return;
               }

               Files.copy(var5, var4.resolve("aether-panorama.zip"), StandardCopyOption.REPLACE_EXISTING);
            }

            var3.reload();
         }

         String var11 = null;

         for (Object var6 : var3.getAvailableIds()) {
            String var7 = String.valueOf(var6);
            if (var7.endsWith("aether-panorama.zip")) {
               var11 = var7;
               break;
            }
         }

         if (var11 != null) {
            boolean var13 = bl ? var3.addPack(var11) : var3.removePack(var11);
            if (var13) {
               var2.reloadResourcePacks();
            }
         }
      } catch (Exception var10) {
      }
   }
}
