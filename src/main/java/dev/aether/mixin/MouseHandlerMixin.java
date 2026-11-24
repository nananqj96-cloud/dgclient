package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.aether.AetherClient;
import dev.aether.hud.HudDragController;
import dev.aether.module.ModuleManager;
import dev.aether.util.CpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseHandlerMixin {
   @Shadow
   private double accumulatedDX;
   @Shadow
   private double accumulatedDY;

   @Inject(
      method = {"turnPlayer"},
      at = {@At("HEAD")}
   )
   private void aetherclient$aimAssist(double var1, CallbackInfo var3) {
      // Aim assist computePixels stub - Anubis-internal API disabled
   }

   @Inject(
      method = {"onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$onButton(long var1, MouseButtonInfo var3, int var4, CallbackInfo var5) {
      Minecraft var6 = Minecraft.getInstance();
      if (var1 == var6.getWindow().handle()) {
         if (var4 == 1 && var6.screen == null) {
            CpsTracker.onClick(var3.button());
            ModuleManager var7 = AetherClient.modules();
            if (var7 != null && var6.level != null) {
               var7.onKeyPressed(var3.button());
            }
         }

         if (var6.screen instanceof ChatScreen && var3.button() == 0 && AetherClient.hud() != null) {
            if (var4 == 1) {
               if (HudDragController.tryStartDrag(AetherClient.hud())) {
                  var5.cancel();
               }
            } else if (var4 == 0 && HudDragController.isDragging()) {
               HudDragController.stopDrag();
               AetherClient.config().save();
               var5.cancel();
            }
         }
      }
   }

   @ModifyExpressionValue(
      method = {"turnPlayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
      )},
      slice = {@Slice(
         from = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Options;sensitivity()Lnet/minecraft/client/OptionInstance;"
         )
      )}
   )
   private Object aetherclient$zoomSensitivity(Object var1) {
      // Zoom currentFactor stub - Anubis-internal API disabled
      return var1;
   }

   @Inject(
      method = {"onScroll(JDD)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$onScroll(long var1, double var3, double var5, CallbackInfo var7) {
      Minecraft var8 = Minecraft.getInstance();
      if (var1 == var8.getWindow().handle()
         && var8.screen instanceof ChatScreen
         && AetherClient.hud() != null
         && HudDragController.tryResize(AetherClient.hud(), var5)) {
         var7.cancel();
      }
   }
}
