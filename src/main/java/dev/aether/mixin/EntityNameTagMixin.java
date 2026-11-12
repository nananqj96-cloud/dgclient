package dev.aether.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FakeRolesModule;
import dev.aether.module.impl.NameTagsModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityRenderer.class})
public class EntityNameTagMixin {
   @Inject(
      method = {"submitNameTag"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$nameTag(
      EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci
   ) {
      ModuleManager modules = AetherClient.modules();
      if (modules != null && state.nameTag != null) {
         NameTagsModule nameTags = modules.nameTags;
         boolean nameTagsOn = nameTags != null && nameTags.isEnabled();
         if (nameTagsOn) {
            String display = state.nameTag.getString();
            if (isLocalPlayer(display)) {
               if (nameTags.hideOwnTag.get() || nameTags.players.get() && nameTags.self.get()) {
                  ci.cancel();
                  return;
               }
            } else if (isOnlinePlayer(display)) {
               if (nameTags.players.get() || nameTags.hidePlayerTags.get()) {
                  ci.cancel();
                  return;
               }
            } else if (nameTags.hideOtherTags.get()) {
               ci.cancel();
               return;
            }
         }

         if (modules.nameProtect != null && modules.nameProtect.isEnabled()) {
            String replacement = modules.nameProtect.replacementForDisplay(state.nameTag.getString());
            if (replacement != null) {
               state.nameTag = Component.literal(replacement);
            }
         }

         FakeRolesModule fakeRoles = modules.fakeRoles;
         if (fakeRoles != null && isLocalPlayer(state.nameTag.getString())) {
            state.nameTag = fakeRoles.decorateNametag(state.nameTag);
         }
      }
   }

   private static boolean isLocalPlayer(String display) {
      if (display != null && !display.isEmpty()) {
         LocalPlayer self = Minecraft.getInstance().player;
         if (self == null) {
            return false;
         } else {
            String name = self.getGameProfile().name();
            return name != null && !name.isEmpty() && display.contains(name);
         }
      } else {
         return false;
      }
   }

   private static boolean isOnlinePlayer(String display) {
      if (display != null && !display.isEmpty()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.getConnection() == null) {
            return false;
         } else {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
               GameProfile profile = info.getProfile();
               String name = profile == null ? null : profile.name();
               if (name != null && !name.isEmpty() && display.contains(name)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }
}
