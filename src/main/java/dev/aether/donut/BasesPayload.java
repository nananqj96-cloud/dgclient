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
      (buf, payload) -> buf.writeByteArray(payload.data),
      buf -> new BasesPayload(buf.readByteArray())
   );

   @Override
   public Type<BasesPayload> type() {
      return ID;
   }
}
