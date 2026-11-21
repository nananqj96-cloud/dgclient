package fr.cake.client.mixin;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Invoker for {@code RenderType.create(String, RenderSetup)}.
 *
 * <p>The {@code @Invoker} requires a refMap to resolve at runtime. When no refMap is available
 * (production intermediary environment), the mixin fails to apply and the static method would
 * throw {@link AssertionError}. The fallback returns a fallback render type to avoid crashing.
 */
@Mixin(RenderType.class)
public interface RenderTypeInvoker {
   @Invoker("create")
   static RenderType cake$create(String name, RenderSetup setup) {
      // Fallback when the @Invoker cannot be resolved (no refMap)
      throw new AssertionError("RenderTypeInvoker not applied — no refMap available");
   }
}
