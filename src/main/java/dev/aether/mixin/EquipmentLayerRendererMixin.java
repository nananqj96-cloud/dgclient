package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.ArmorTrimHiderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EquipmentLayerRenderer.class})
public class EquipmentLayerRendererMixin {
   @ModifyExpressionValue(
      method = {"renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"
      )}
   )
   private Object aetherclient$armorTrim(Object original, @Local(argsOnly = true) ItemStack itemStack, @Local(argsOnly = true) Object renderState) {
      ModuleManager modules = AetherClient.modules();
      if (modules == null) {
         return original;
      } else {
         ArmorTrimHiderModule module = modules.armorTrimHider;
         if (module != null && module.isEnabled()) {
            return !module.affectsOwn() && aetherclient$isLocalPlayer(renderState) ? original : module.mapTrim(itemStack, (ArmorTrim)original);
         } else {
            return original;
         }
      }
   }

   private static boolean aetherclient$isLocalPlayer(Object renderState) {
      if (!(renderState instanceof AvatarRenderState avatar)) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         return mc.player != null && avatar.id == mc.player.getId();
      }
   }
}
