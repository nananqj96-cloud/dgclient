package fr.cake.client.mixin;
import fr.cake.client.AvatarSkins;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(LivingEntityRenderer.class)
public abstract class AvatarSkinMixin {
 @org.spongepowered.asm.mixin.injection.Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
 private void cake$ownAvatar(net.minecraft.world.entity.LivingEntity entity,net.minecraft.client.renderer.entity.state.LivingEntityRenderState state,float partial,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){fr.cake.client.CombatEffects.capture(entity,state);var mc=net.minecraft.client.Minecraft.getInstance();var s=fr.cake.client.CakeClient.settings;if(s!=null&&s.skinEnabled&&entity==mc.player&&fr.cake.client.Edition.skin(fr.cake.client.Edition.PLAN,s.avatarSkin))state.isInvisible=false;}

 @SuppressWarnings({"unchecked","rawtypes"})
 @Redirect(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
 private void cake$model(SubmitNodeCollector collector,Model model,Object state,PoseStack stack,RenderType type,int light,int overlay,int color,TextureAtlasSprite sprite,int outline,ModelFeatureRenderer.CrumblingOverlay crumble){stack.pushPose();fr.cake.client.CombatEffects.pose(state,stack);fr.cake.client.BackCosmetics.submit(state,stack,collector,light);if(!AvatarSkins.submit(state,stack,collector))collector.submitModel(model,state,stack,type,light,fr.cake.client.CombatEffects.tinted(state)?net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY:overlay,fr.cake.client.CombatEffects.tint(state,color),sprite,outline,crumble);stack.popPose();}
 @SuppressWarnings({"unchecked","rawtypes"})
 @Redirect(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"))
 private void cake$layers(RenderLayer layer,PoseStack stack,SubmitNodeCollector collector,int light,EntityRenderState state,float yaw,float pitch){if(AvatarSkins.state(state)==null&&!(layer instanceof net.minecraft.client.renderer.entity.layers.CapeLayer&&fr.cake.client.BackCosmetics.replacesCape(state)))layer.submit(stack,collector,light,state,yaw,pitch);}
}
