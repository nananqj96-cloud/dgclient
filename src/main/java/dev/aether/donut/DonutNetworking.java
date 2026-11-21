package dev.aether.donut;

import dev.aether.AetherClient;
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
      try {
         PayloadTypeRegistry.playS2C().register(BasesPayload.ID, BasesPayload.CODEC);
      } catch (IllegalArgumentException e) {
         // DonutLarp mod is also installed and already registered this payload type — skip the type registration
         AetherClient.LOGGER.warn("Payload type {} already registered (DonutLarp present); skipping type registration", BasesPayload.ID);
      }
      ClientPlayNetworking.registerGlobalReceiver(BasesPayload.ID,
         (payload, context) -> context.client().execute(() -> BaseStore.handle(payload.data())));

      try {
         PayloadTypeRegistry.playS2C().register(ActivityPayload.ID, ActivityPayload.CODEC);
      } catch (IllegalArgumentException e) {
         // DonutLarp mod is also installed and already registered this payload type — skip the type registration
         AetherClient.LOGGER.warn("Payload type {} already registered (DonutLarp present); skipping type registration", ActivityPayload.ID);
      }
      ClientPlayNetworking.registerGlobalReceiver(ActivityPayload.ID,
         (payload, context) -> context.client().execute(() -> ActivityStore.handle(payload.data())));

      ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
         BaseStore.clear();
         ActivityStore.clear();
      });
   }
}
