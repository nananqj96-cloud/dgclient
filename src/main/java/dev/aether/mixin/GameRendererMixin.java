package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FreecamModule;
import dev.aether.render.BlurHook;
import dev.aether.render.OverlayRenderer;
import dev.aether.render.WorldProjection;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class GameRendererMixin {
   @Inject(
      method = {"render(Lnet/minecraft/client/DeltaTracker;Z)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrameNumber()V",
         shift = Shift.AFTER
      )}
   )
   private void aetherclient$renderOverlay(DeltaTracker deltaTracker, boolean bl, CallbackInfo ci) {
      OverlayRenderer.render();
   }

   @Inject(
      method = {"renderLevel(Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V"
      )}
   )
   private void aetherclient$captureProjection(DeltaTracker deltaTracker, CallbackInfo ci, @Local(ordinal = 0) Matrix4f matrix4f) {
      WorldProjection.capture(matrix4f, deltaTracker.getGameTimeDeltaPartialTick(false));
   }

   @ModifyArg(
      method = {"render(Lnet/minecraft/client/DeltaTracker;Z)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/GlobalSettingsUniform;update(IIDJLnet/minecraft/client/DeltaTracker;ILnet/minecraft/client/Camera;Z)V"
      ),
      index = 5
   )
   private int aetherclient$overrideBlurRadius(int blurriness) {
      return BlurHook.apply(blurriness);
   }

   @ModifyReturnValue(
      method = {"getFov(Lnet/minecraft/client/Camera;FZ)F"},
      at = {@At("RETURN")}
   )
   private float aetherclient$zoomFov(float original) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null && modules.zoom != null) {
         double factor = modules.zoom.currentFactor();
         return factor > 1.0001 ? (float)((double)original / factor) : original;
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
      method = {"getFov(Lnet/minecraft/client/Camera;FZ)F"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;lerp(FFF)F",
         ordinal = 0
      )}
   )
   private float aetherclient$customFov(float sprintMultiplier) {
      ModuleManager modules = AetherClient.modules();
      return modules != null && modules.customFov != null ? modules.customFov.fovMultiplier(sprintMultiplier) : sprintMultiplier;
   }

   @ModifyExpressionValue(
      method = {"renderItemInHand"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"
      )}
   )
   private boolean aetherclient$freecamRenderFirstPersonHands(boolean isFirstPerson) {
      FreecamModule freecam = FreecamModule.get();
      return freecam != null && freecam.isActive() && freecam.renderHands() ? true : isFirstPerson;
   }
}
