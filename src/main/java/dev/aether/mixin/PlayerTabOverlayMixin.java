package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PlayerTabOverlay.class})
public class PlayerTabOverlayMixin {
   @ModifyReturnValue(
      method = {"getNameForDisplay"},
      at = {@At("RETURN")}
   )
   private Component aetherclient$protectTabName(Component original, PlayerInfo playerInfo) {
      ModuleManager modules = AetherClient.modules();
      if (modules == null) {
         return original;
      } else {
         GameProfile profile = playerInfo.getProfile();
         String name = profile == null ? null : profile.name();
         Component result = original;
         if (modules.nameProtect != null && modules.nameProtect.isEnabled() && name != null && !name.isEmpty()) {
            String replacement = modules.nameProtect.replacementForDisplay(name);
            if (replacement != null) {
               result = Component.literal(replacement);
            }
         }

         if (modules.fakeRoles != null && name != null) {
            result = modules.fakeRoles.decorateTab(result, name);
         }

         return result;
      }
   }
}
