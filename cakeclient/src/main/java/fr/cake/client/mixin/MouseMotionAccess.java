package fr.cake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Discard pointer movement made inside Cake's menu before the mouse is grabbed again.
 *
 * <p>The {@code @Accessor} methods require a refMap to resolve at runtime. When no refMap is
 * available (production intermediary environment), the mixin fails to apply and the methods would
 * remain abstract, causing {@link AbstractMethodError} when Cake Client's {@code ClientScreen}
 * calls them. Default no-op implementations prevent the crash.
 */
@Mixin(net.minecraft.client.MouseHandler.class)
public interface MouseMotionAccess {
   @Accessor("accumulatedDX")
   default void cake$pendingX(double value) {
      // No-op fallback when the @Accessor cannot be resolved (no refMap)
   }

   @Accessor("accumulatedDY")
   default void cake$pendingY(double value) {
      // No-op fallback when the @Accessor cannot be resolved (no refMap)
   }
}
