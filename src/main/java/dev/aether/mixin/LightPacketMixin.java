package dev.aether.mixin;

import dev.aether.suschunk.ServerLightCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPacketListener.class})
public class LightPacketMixin {
   @Inject(
      method = {"handleLightUpdatePacket"},
      at = {@At("TAIL")}
   )
   private void aetherclient$captureLightUpdate(ClientboundLightUpdatePacket packet, CallbackInfo ci) {
      ServerLightCache.get().ingest(packet.getX(), packet.getZ(), packet.getLightData(), Minecraft.getInstance().level);
   }

   @Inject(
      method = {"handleLevelChunkWithLight"},
      at = {@At("TAIL")}
   )
   private void aetherclient$captureChunkLight(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
      ServerLightCache.get().ingest(packet.getX(), packet.getZ(), packet.getLightData(), Minecraft.getInstance().level);
   }
}
