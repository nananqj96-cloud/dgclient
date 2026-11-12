package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.StringSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;

public final class FakeMediaModule extends Module {
   public final BooleanSetting mediaBadge = this.addSetting(new BooleanSetting("Media Badge", "Show the media badge beside your own nametag.", true));
   public final BooleanSetting bluePlus = this.addSetting(new BooleanSetting("Blue Plus", "Show the blue plus beside your own nametag.", false));
   public final StringSetting displayName = this.addSetting(
      new StringSetting("Display Name", "Optional local display name. Leave blank to use your Minecraft name.", "", 32, "Minecraft name")
   );

   public FakeMediaModule() {
      super("Fake Media", "Adds local media styling to your own nametag.", Category.MISC);
   }

   public boolean hasVisibleBadge() {
      return this.isEnabled() && (this.mediaBadge.get() || this.bluePlus.get());
   }

   public Component decorate(Component var1) {
      if (this.hasVisibleBadge() && var1 != null) {
         String var2 = this.displayName.get().trim();
         Component var3 = (Component)(var2.isEmpty() ? var1 : Component.literal(var2));
         MutableComponent var4 = Component.empty();
         if (this.bluePlus.get()) {
            var4.append(icon("\ue002"));
         }

         if (this.mediaBadge.get()) {
            var4.append(icon("\ue001"));
         }

         return var4.append(var3.copy());
      } else {
         return var1;
      }
   }

   private static Component icon(String var0) {
      return Component.literal(var0).setStyle(Style.EMPTY.withFont(new Resource(Identifier.fromNamespaceAndPath("fake_media", "default"))));
   }
}
