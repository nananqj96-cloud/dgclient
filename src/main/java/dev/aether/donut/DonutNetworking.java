package dev.aether.donut;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Wires the DonutLarp server-to-client payloads ({@code donutlarp:bases} and
 * {@code donutlarp:activity}) into the Fabric networking stack. Called once during client init.
 *
 * <p>The payloads carry opaque packed byte blobs decoded by {@link BaseStore} and
 * {@link ActivityStore}. On play disconnect both stores are cleared so stale base/activity data
 * does not carry into the next world.
 */
public final class DonutNetworking {
   private DonutNetworking() {
   }

   public static void register() {
      PayloadTypeRegistry.playS2C().register(BasesPayload.ID, BasesPayload.CODEC);
      ClientPlayNetworking.registerGlobalReceiver(BasesPayload.ID,
         (payload, context) -> context.client().execute(() -> BaseStore.handle(payload.data())));

      PayloadTypeRegistry.playS2C().register(ActivityPayload.ID, ActivityPayload.CODEC);
      ClientPlayNetworking.registerGlobalReceiver(ActivityPayload.ID,
         (payload, context) -> context.client().execute(() -> ActivityStore.handle(payload.data())));

      ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
         BaseStore.clear();
         ActivityStore.clear();
      });
   }
}
