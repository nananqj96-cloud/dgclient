package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.jetbrains.annotations.Nullable;

public class ArmorTrimHiderModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Hide wipes worn trims; Random gives every piece a random one",
         "Hide",
         "Hide",
         "Random"
      )
   );
   public final BooleanSetting ownArmor = this.addSetting(
      new BooleanSetting(
         "Own Armor", "Also affect your own worn armor (F5 / inventory)", true
      )
   );
   private RegistryAccess cachedAccess;
   private final List<Holder<TrimMaterial>> materials = new ArrayList<>();
   private final List<Holder<TrimPattern>> patterns = new ArrayList<>();

   public ArmorTrimHiderModule() {
      super("ArmorTrimHider", "Hides or randomizes worn armor trims", Category.MISC);
   }

   public boolean affectsOwn() {
      return this.ownArmor.get();
   }

   @Nullable
   public ArmorTrim mapTrim(ItemStack stack, @Nullable ArmorTrim original) {
      if (this.mode.is("Random")) {
         ArmorTrim random = this.randomTrim(stack);
         return random != null ? random : original;
      } else {
         return null;
      }
   }

   @Nullable
   private ArmorTrim randomTrim(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         return null;
      } else {
         RegistryAccess access = mc.level.registryAccess();
         if (access != this.cachedAccess) {
            this.rebuildCache(access);
         }

         if (!this.materials.isEmpty() && !this.patterns.isEmpty()) {
            Random rng = new Random((long)stack.getItem().getDescriptionId().hashCode());
            Holder<TrimMaterial> material = this.materials.get(rng.nextInt(this.materials.size()));
            Holder<TrimPattern> pattern = this.patterns.get(rng.nextInt(this.patterns.size()));
            return new ArmorTrim(material, pattern);
         } else {
            return null;
         }
      }
   }

   private void rebuildCache(RegistryAccess access) {
      this.cachedAccess = access;
      this.materials.clear();
      this.patterns.clear();

      try {
         access.lookupOrThrow(Registries.TRIM_MATERIAL).listElements().forEach(this.materials::add);
         access.lookupOrThrow(Registries.TRIM_PATTERN).listElements().forEach(this.patterns::add);
      } catch (Exception var3) {
         this.materials.clear();
         this.patterns.clear();
      }
   }

   @Override
   protected void onDisable() {
      this.cachedAccess = null;
      this.materials.clear();
      this.patterns.clear();
   }
}
