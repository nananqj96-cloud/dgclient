package dev.aether.donut;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dev.aether.AetherClient;

/**
 * Client-side cache of underground player activity reported by the DonutLarp server via the
 * {@code donutlarp:activity} S2C packet. Ported faithfully from the DonutLarp addon's
 * {@code ActivityStore}. Data expires after 5 seconds of no update.
 */
public final class ActivityStore {
   private static volatile List<Activity> current = List.of();
   private static volatile long updated;

   private ActivityStore() {
   }

   public static List<Activity> all() {
      return System.currentTimeMillis() - updated > 5000L ? List.of() : current;
   }

   public static void clear() {
      current = List.of();
   }

   /** Decodes a {@code donutlarp:activity} payload. */
   public static void handle(byte[] data) {
      ArrayList<Activity> list = new ArrayList<>();
      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
         int n = in.readInt();
         for (int i = 0; i < n; ++i) {
            list.add(new Activity(in.readInt(), in.readInt(), in.readInt()));
         }
      } catch (IOException e) {
         AetherClient.LOGGER.warn("Bad donutlarp:activity packet", e);
         return;
      }
      current = List.copyOf(list);
      updated = System.currentTimeMillis();
   }

   public record Activity(int chunkX, int chunkZ, int players) {
   }
}
