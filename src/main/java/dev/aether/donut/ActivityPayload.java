package dev.aether.donut;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.util.Identifier;

/** S2C payload carrying the packed {@code donutlarp:activity} blob. */
public record ActivityPayload(byte[] data) implements CustomPacketPayload {
   public static final Type<ActivityPayload> ID = new Type<>(Identifier.of("donutlarp", "activity"));
   public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ActivityPayload> CODEC = StreamCodec.of(
      (payload, buf) -> buf.writeByteArray(payload.data),
      buf -> new ActivityPayload(buf.readByteArray())
   );

   @Override
   public Type<ActivityPayload> type() {
      return ID;
   }
}
