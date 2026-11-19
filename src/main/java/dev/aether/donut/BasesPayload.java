package dev.aether.donut;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** S2C payload carrying the packed {@code donutlarp:bases} blob. */
public record BasesPayload(byte[] data) implements CustomPacketPayload {
   public static final Type<BasesPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("donutlarp", "bases"));
   public static final StreamCodec<RegistryFriendlyByteBuf, BasesPayload> CODEC = StreamCodec.of(
      (buf, payload) -> buf.writeBytes(payload.data),
      buf -> {
         byte[] bytes = new byte[buf.readableBytes()];
         buf.readBytes(bytes);
         return new BasesPayload(bytes);
      }
   );

   @Override
   public Type<BasesPayload> type() {
      return ID;
   }
}
