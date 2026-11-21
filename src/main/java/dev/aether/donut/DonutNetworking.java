package dev.aether.donut;

import dev.aether.AetherClient;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Wires the DonutLarp server-to-client payloads ({@code donutlarp:bases} and
 * {@code donutlarp:activity}) into the Fabric networking stack. Called once during client init.
 *
 * <p>The payloads carry opaque packed byte blobs decoded by {@link BaseStore} and
 * {@link ActivityStore}. On play disconnect both stores are cleared so stale base/activity data
 * does not carry into the next world.
 *
 * <p>If the DonutLarp mod is also installed, it registers the payload types itself — Aether Client
 * must NOT also register them (Fabric throws on duplicate type registration). Only the global
 * receivers are registered here, which is safe: multiple receivers per payload type are allowed.
 */
public final class DonutNetworking {
   private DonutNetworking() {
   }

   public static void register() {
      // If DonutLarp mod is present, it owns the payload type registrations.
      // We must skip registering the types ourselves to avoid duplicate-registration crashes.
      boolean donutLarpPresent = FabricLoader.getInstance().isModLoaded("donutlarp");

      if (!donutLarpPresent) {
         PayloadTypeRegistry.playS2C().register(BasesPayload.ID, BasesPayload.CODEC);
         PayloadTypeRegistry.playS2C().register(ActivityPayload.ID, ActivityPayload.CODEC);
      } else {
         AetherClient.LOGGER.info("DonutLarp mod detected; skipping payload type registration (receivers only)");
      }

      ClientPlayNetworking.registerGlobalReceiver(BasesPayload.ID,
         (payload, context) -> context.client().execute(() -> BaseStore.handle(payload.data())));

      ClientPlayNetworking.registerGlobalReceiver(ActivityPayload.ID,
         (payload, context) -> context.client().execute(() -> ActivityStore.handle(payload.data())));

      ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> {
         BaseStore.clear();
         ActivityStore.clear();
      });
   }
}
