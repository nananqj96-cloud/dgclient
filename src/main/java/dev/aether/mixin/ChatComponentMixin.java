package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin({ChatComponent.class})
public class ChatComponentMixin {
   @ModifyVariable(
      method = {"addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private Component aetherclient$censorChat(Component component) {
      ModuleManager modules = AetherClient.modules();
      if (modules == null) {
         return component;
      } else {
         Component result = component;
         if (modules.fakeRoles != null) {
            result = modules.fakeRoles.decorateChat(component);
         }

         if (modules.nameProtect != null && modules.nameProtect.isEnabled()) {
            result = modules.nameProtect.censorChat(result);
         }

         return result;
      }
   }
}
