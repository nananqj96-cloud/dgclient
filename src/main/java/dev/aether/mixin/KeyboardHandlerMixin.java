package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardHandler.class})
public class KeyboardHandlerMixin {
   @Inject(
      method = {"keyPress(JILnet/minecraft/client/input/KeyEvent;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$dispatchModuleKeybinds(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      if (AetherClient.modules() != null && action == 1) {
         if (minecraft.screen == null && minecraft.level != null) {
            if (AetherClient.modules().onKeyPressed(keyEvent.key())) {
               ci.cancel();
            }
         } else if ((keyEvent.key() == 340 || keyEvent.key() == 344) && minecraft.screen != null && shiftOpensGui(minecraft.screen)) {
            minecraft.setScreen(new ClickGuiScreen(minecraft.screen));
            ci.cancel();
         }
      }
   }

   private static boolean shiftOpensGui(Screen screen) {
      boolean var10000;
      if (!(screen instanceof ClickGuiScreen)
         && !(screen instanceof ChatScreen)
         && !(screen instanceof AbstractContainerScreen)
         && !(screen instanceof AbstractSignEditScreen)
         && !(screen instanceof BookEditScreen)) {
         if (screen.getFocused() instanceof EditBox editBox && editBox.isFocused()) {
            return false;
         }

         var10000 = true;
      } else {
         var10000 = false;
      }

      return var10000;
   }
}
