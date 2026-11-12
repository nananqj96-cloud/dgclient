package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FakeMediaModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AvatarRenderer.class})
public final class PlayerEntityRendererMixin {
   @Inject(
      method = {"extractRenderState"},
      at = {@At("TAIL")}
   )
   private void aether$fakeMediaBadge(Avatar var1, AvatarRenderState var2, float var3, CallbackInfo var4) {
      FakeMediaModule var5 = module();
      Minecraft var6 = Minecraft.getInstance();
      if (var5 != null && var5.hasVisibleBadge() && var6.player != null && var2.id == var6.player.getId()) {
         if (var2.scoreText != null) {
            var2.scoreText = var5.decorate(var2.scoreText);
         } else if (var2.nameTag != null) {
            var2.scoreText = var5.decorate(var2.nameTag);
         }

         var2.nameTag = null;
      }
   }

   @Inject(
      method = {"shouldShowName"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void aether$showOwnMediaLabel(Avatar var1, double var2, CallbackInfoReturnable<Boolean> var4) {
      FakeMediaModule var5 = module();
      Minecraft var6 = Minecraft.getInstance();
      if (var5 != null && var5.hasVisibleBadge() && var6.player != null && var1.getId() == var6.player.getId()) {
         var4.setReturnValue(true);
      }
   }

   private static FakeMediaModule module() {
      ModuleManager var0 = AetherClient.modules();
      return var0 == null ? null : var0.fakeMedia;
   }
}
