package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.NetheriteFinderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public class MultiPlayerGameModeAttackMixin {
   @Unique
   private BlockPos aetherclient$pendingPlacement;
   @Unique
   private BlockPos aetherclient$pendingMinedDebris;

   @Inject(
      method = {"attack"},
      at = {@At("HEAD")}
   )
   private void aetherclient$onAttack(Player player, Entity target, CallbackInfo ci) {
      if (player == Minecraft.getInstance().player && target != player) {
         ModuleManager modules = AetherClient.modules();
         if (modules != null && modules.hitParticles != null && modules.hitParticles.isEnabled()) {
            modules.hitParticles.onHit(target);
         }
      }
   }

   @Inject(
      method = {"destroyBlock"},
      at = {@At("HEAD")}
   )
   private void aetherclient$beginBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      this.aetherclient$pendingMinedDebris = null;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.level.getBlockState(pos).is(Blocks.ANCIENT_DEBRIS)) {
         this.aetherclient$pendingMinedDebris = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
      }
   }

   @Inject(
      method = {"destroyBlock"},
      at = {@At("RETURN")}
   )
   private void aetherclient$finishBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      BlockPos mined = this.aetherclient$pendingMinedDebris;
      this.aetherclient$pendingMinedDebris = null;
      if (mined != null && Boolean.TRUE.equals(cir.getReturnValue())) {
         ModuleManager modules = AetherClient.modules();
         NetheriteFinderModule finder = modules == null ? null : modules.netheriteFinder;
         if (finder != null) {
            finder.recordMinedDebris(mined);
         }
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("HEAD")}
   )
   private void aetherclient$beginPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
      this.aetherclient$pendingPlacement = null;
      Minecraft mc = Minecraft.getInstance();
      if (player == mc.player && mc.level != null) {
         if ((hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem()).getItem() instanceof BlockItem) {
            BlockPos clicked = hit.getBlockPos();
            BlockPos target = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
            this.aetherclient$pendingPlacement = new BlockPos(target.getX(), target.getY(), target.getZ());
         }
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("RETURN")}
   )
   private void aetherclient$finishPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
      BlockPos target = this.aetherclient$pendingPlacement;
      this.aetherclient$pendingPlacement = null;
      if (target != null && cir.getReturnValue() != null && ((InteractionResult)cir.getReturnValue()).consumesAction()) {
         ModuleManager modules = AetherClient.modules();
         NetheriteFinderModule finder = modules == null ? null : modules.netheriteFinder;
         if (finder != null) {
            finder.recordPlayerPlacement(target);
         }
      }
   }
}
