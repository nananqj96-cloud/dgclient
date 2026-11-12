package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FakeStatsModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class GuiSidebarMixin {
   @Inject(
      method = {"displayScoreboardSidebar"},
      at = {@At("HEAD")}
   )
   private void aetherclient$beginSidebar(GuiGraphics guiGraphics, Objective objective, CallbackInfo ci) {
      FakeStatsModule fs = fakeStats();
      if (fs != null) {
         fs.beginSidebar();
      }
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/network/chat/FormattedText;)I",
         ordinal = 1
      )
   )
   private int aetherclient$lineWidth(Font font, FormattedText text) {
      FakeStatsModule fs = fakeStats();
      if (fs != null && text instanceof Component component) {
         try {
            return font.width(fs.rewriteForWidth(component));
         } catch (Exception var6) {
         }
      }

      return font.width(text);
   }

   @ModifyArg(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
         ordinal = 1
      ),
      index = 1
   )
   private Component aetherclient$lineText(Component text) {
      FakeStatsModule fs = fakeStats();
      if (fs != null) {
         try {
            return fs.rewriteForDraw(text);
         } catch (Exception var4) {
         }
      }

      return text;
   }

   private static FakeStatsModule fakeStats() {
      ModuleManager m = AetherClient.modules();
      return m != null && m.fakeStats != null && m.fakeStats.isEnabled() ? m.fakeStats : null;
   }
}
