package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.NetheriteFinderModule;
import dev.aether.module.impl.SpawnerProtectModule;
import dev.aether.render.BlockEspRenderer;
import dev.aether.render.ClusterEspRenderer;
import dev.aether.render.StorageEspRenderer;
import dev.aether.util.TpsTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public class ClientPacketListenerMixin {
   @Inject(
      method = {"handleSetTime"},
      at = {@At("HEAD")}
   )
   private void aetherclient$trackTps(ClientboundSetTimePacket var1, CallbackInfo var2) {
      TpsTracker.onTimePacket();
   }

   @Inject(
      method = {"sendCommand"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$fakeCommands(String var1, CallbackInfo var2) {
      ModuleManager var3 = AetherClient.modules();
      if (var3 != null) {
         try {
            if (var3.fakePay != null && var3.fakePay.tryIntercept(var1)) {
               var2.cancel();
               return;
            }

            if (var3.fakeStats != null && var3.fakeStats.tryInterceptBalance(var1)) {
               var2.cancel();
            }
         } catch (Exception var5) {
         }
      }
   }

   @Inject(
      method = {"handleLevelChunkWithLight"},
      at = {@At("TAIL")}
   )
   private void aetherclient$blockEntityChunk(ClientboundLevelChunkWithLightPacket var1, CallbackInfo var2) {
      BlockEspRenderer.markChunkDirty(var1.getX(), var1.getZ());
      ClusterEspRenderer.markChunkDirty(var1.getX(), var1.getZ());
      StorageEspRenderer.markChunkDirty(var1.getX(), var1.getZ());
      ModuleManager var3 = AetherClient.modules();
      if (var3 != null && var3.spawnerNametags != null && var3.spawnerNametags.isEnabled()) {
         var3.spawnerNametags.markChunkDirty(var1.getX(), var1.getZ());
      }

      markSusChunkDirty(var1.getX(), var1.getZ());
   }

   private static SpawnerProtectModule spawnerProtect() {
      ModuleManager var0 = AetherClient.modules();
      return var0 != null ? var0.spawnerProtect : null;
   }

   private static NetheriteFinderModule netheriteFinder() {
      ModuleManager modules = AetherClient.modules();
      return modules != null ? modules.netheriteFinder : null;
   }

   private static void markSusChunkDirty(int var0, int var1) {
      ModuleManager var2 = AetherClient.modules();
      if (var2 != null && var2.susChunkFinder != null && var2.susChunkFinder.isEnabled()) {
         var2.susChunkFinder.scanner.requestRescan(var0, var1);
      }
   }

   private static void markSpawnerNotifierDirty() {
      ModuleManager var0 = AetherClient.modules();
      if (var0 != null && var0.spawnerNametags != null && var0.spawnerNametags.isEnabled()) {
         var0.spawnerNametags.markDirty();
      }
   }

   @Inject(
      method = {"handleBlockDestruction"},
      at = {@At("HEAD")}
   )
   private void aetherclient$spawnerProtectDestruction(ClientboundBlockDestructionPacket var1, CallbackInfo var2) {
      SpawnerProtectModule var3 = spawnerProtect();
      if (var3 != null && var3.isEnabled() && Minecraft.getInstance().isSameThread()) {
         try {
            var3.onBlockDestructionPacket(var1.getId(), var1.getPos());
         } catch (Exception var5) {
         }
      }
   }

   @Inject(
      method = {"handleBlockUpdate"},
      at = {@At("HEAD")}
   )
   private void aetherclient$spawnerProtectBlockUpdate(ClientboundBlockUpdatePacket var1, CallbackInfo var2) {
      BlockEspRenderer.markDirty();
      ClusterEspRenderer.markDirty();
      BlockPos var3 = var1.getPos();
      BlockEspRenderer.markChunkDirty(var3.getX() >> 4, var3.getZ() >> 4);
      ClusterEspRenderer.markChunkDirty(var3.getX() >> 4, var3.getZ() >> 4);
      StorageEspRenderer.markChunkDirty(var3.getX() >> 4, var3.getZ() >> 4);
      markSpawnerNotifierDirty();
      markSusChunkDirty(var3.getX() >> 4, var3.getZ() >> 4);
      NetheriteFinderModule finder = netheriteFinder();
      if (finder != null) {
         finder.onServerBlockUpdate(var3, var1.getBlockState());
      }

      SpawnerProtectModule var4 = spawnerProtect();
      if (var4 != null && var4.isEnabled() && var4.detectBlockUpdatesEnabled() && Minecraft.getInstance().isSameThread()) {
         try {
            var4.onServerBlockUpdate(var1.getPos(), var1.getBlockState(), false);
         } catch (Exception var7) {
         }
      }
   }

   @Inject(
      method = {"handleChunkBlocksUpdate"},
      at = {@At("HEAD")}
   )
   private void aetherclient$spawnerProtectSectionUpdate(ClientboundSectionBlocksUpdatePacket var1, CallbackInfo var2) {
      BlockEspRenderer.markDirty();
      ClusterEspRenderer.markDirty();
      markSpawnerNotifierDirty();
      SpawnerProtectModule var3 = spawnerProtect();
      boolean var4 = var3 != null && var3.isEnabled() && var3.detectBlockUpdatesEnabled() && Minecraft.getInstance().isSameThread();

      try {
         var1.runUpdates((var2x, var3xx) -> {
            BlockEspRenderer.markChunkDirty(var2x.getX() >> 4, var2x.getZ() >> 4);
            ClusterEspRenderer.markChunkDirty(var2x.getX() >> 4, var2x.getZ() >> 4);
            StorageEspRenderer.markChunkDirty(var2x.getX() >> 4, var2x.getZ() >> 4);
            markSusChunkDirty(var2x.getX() >> 4, var2x.getZ() >> 4);
            NetheriteFinderModule finder = netheriteFinder();
            if (finder != null) {
               finder.onServerBlockUpdate(var2x, var3xx);
            }

            if (var4) {
               var3.onServerBlockUpdate(var2x, var3xx, true);
            }
         });
      } catch (Exception var6) {
      }
   }
}
