package dev.aether.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.render.AccessoryRenderer;
import dev.aether.render.BlockEspRenderer;
import dev.aether.render.BlockOutlineRenderer;
import dev.aether.render.ChunkFinderRenderer;
import dev.aether.render.ClusterEspRenderer;
import dev.aether.render.EntityEspRenderer;
import dev.aether.render.HitParticleRenderer;
import dev.aether.render.HoleEspRenderer;
import dev.aether.render.JumpCircleRenderer;
import dev.aether.render.MiscBlockEspRenderer;
import dev.aether.render.NetheriteFinderRenderer;
import dev.aether.render.NetheriteProbabilityRenderer;
import dev.aether.render.StorageEspRenderer;
import dev.aether.render.SusChunkRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Inject(
      method = {"renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$customBlockOutline(BufferSource var1, PoseStack var2, boolean var3, LevelRenderState var4, CallbackInfo var5) {
      ModuleManager var6 = AetherClient.modules();
      if (var6 != null) {
         if (var3 && var6.susChunkFinder.isEnabled()) {
            SusChunkRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.susChunkFinder);
         }

         if (var3 && var6.chunkFinder != null && var6.chunkFinder.isEnabled()) {
            ChunkFinderRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.chunkFinder);
         }

         if (var3 && var6.storageEsp != null && var6.storageEsp.isEnabled()) {
            StorageEspRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.storageEsp);
         }

         if (var3 && var6.blockEsp != null && var6.blockEsp.isEnabled()) {
            BlockEspRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.blockEsp);
         }

         if (var3 && var6.clusterEsp != null && var6.clusterEsp.isEnabled()) {
            ClusterEspRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.clusterEsp);
         }

         if (var3 && var6.netheriteFinder != null && var6.netheriteFinder.isEnabled()) {
            NetheriteFinderRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.netheriteFinder);
            NetheriteProbabilityRenderer.renderBoxes(var1, var2, var4.cameraRenderState.pos, var6.netheriteFinder);
         }

         if (var3 && var6.spawnerNametags != null && var6.spawnerNametags.isEnabled()) {
            MiscBlockEspRenderer.renderSpawners(var1, var2, var4.cameraRenderState.pos, var6.spawnerNametags);
         }

         if (var3 && var6.debugHoleEsp != null && var6.debugHoleEsp.isEnabled()) {
            HoleEspRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.debugHoleEsp);
         }

         if (var3 && var6.playerEsp != null && var6.playerEsp.isEnabled()) {
            EntityEspRenderer.renderPlayers(var1, var2, var4.cameraRenderState.pos, var6.playerEsp);
         }

         if (var3 && var6.mobEsp != null && var6.mobEsp.isEnabled()) {
            EntityEspRenderer.renderMobs(var1, var2, var4.cameraRenderState.pos, var6.mobEsp);
         }

         if (var3 && var6.jumpCircles != null && var6.jumpCircles.isEnabled()) {
            JumpCircleRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.jumpCircles);
         }

         if (var3 && var6.hitParticles != null && var6.hitParticles.isEnabled()) {
            HitParticleRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.hitParticles);
         }

         if (var3 && var6.customAccessories != null && var6.customAccessories.isEnabled()) {
            AccessoryRenderer.render(var1, var2, var4.cameraRenderState.pos, var6.customAccessories);
         }

         if (var6.freecam != null && var6.freecam.isActive()) {
            var5.cancel();
         } else if (var6.blockOutline.isEnabled()) {
            var5.cancel();
            BlockOutlineRenderState var7 = var4.blockOutlineRenderState;
            if (var7 != null && var7.isTranslucent() == var3) {
               BlockOutlineRenderer.render(var1, var2, var7, var4.cameraRenderState.pos, var6.blockOutline);
            }
         }
      }
   }
}
