package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.aether.AetherClient;
import dev.aether.module.impl.CustomGlintModule;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When the CustomGlint module's "Non-Enchanted" option is enabled, every item reports a foil so the
 * vanilla glint layer (tinted/restyled by {@link dev.aether.render.GlintTextureTinter}) is drawn
 * on non-enchanted items too — applying the configured glint (including the end-portal overlay) to
 * all items, not just enchanted ones.
 */
@Mixin({ItemStack.class})
public class ItemStackGlintMixin {
   @ModifyReturnValue(
      method = {"hasFoil"},
      at = {@At("RETURN")}
   )
   private boolean aetherclient$forceGlint(boolean original) {
      CustomGlintModule module = AetherClient.modules() == null ? null : AetherClient.modules().customGlint;
      if (module != null && module.isActive()) {
         return true;
      }
      return original;
   }
}
