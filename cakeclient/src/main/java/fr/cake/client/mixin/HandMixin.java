package fr.cake.client.mixin;
import fr.cake.client.Knives;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemInHandRenderer.class)
public abstract class HandMixin {
 @Inject(method="renderArmWithItem",at=@At("HEAD"),cancellable=true)
 private void cake$hand(AbstractClientPlayer player,float partial,float pitch,InteractionHand hand,float swing,ItemStack stack,float equip,PoseStack poses,SubmitNodeCollector collector,int light,CallbackInfo ci){
  if(hand!=InteractionHand.MAIN_HAND||!Knives.eligible()||!stack.is(net.minecraft.tags.ItemTags.SWORDS))return;
  var armPose=Knives.prepare();poses.pushPose();poses.mulPose(armPose);
  var renderer=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);var skin=player.getSkin().body().texturePath();
  if(player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)renderer.renderRightHand(poses,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE));else renderer.renderLeftHand(poses,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));
  poses.popPose();
  if(Knives.supportHand()&&player.getOffhandItem().isEmpty()){
   poses.pushPose();poses.mulPose(Knives.supportArmPose());
   if(player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT)renderer.renderLeftHand(poses,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE));else renderer.renderRightHand(poses,collector,light,skin,player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE));
   poses.popPose();
  }
  Knives.submit(poses,collector);ci.cancel();
 }
}
