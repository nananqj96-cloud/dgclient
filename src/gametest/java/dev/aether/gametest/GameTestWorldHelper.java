package dev.aether.gametest;

import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public final class GameTestWorldHelper {
   private GameTestWorldHelper() {
   }

   public static TestSingleplayerContext createWorld(ClientGameTestContext context, String suiteName) {
      context.runOnClient(mc -> {
         mc.options.framerateLimit().set(60);
         mc.options.pauseOnLostFocus = false;
      });
      try {
         TestSingleplayerContext world = context.worldBuilder().create();
         context.runOnClient(mc -> {
            mc.options.framerateLimit().set(120);
            mc.options.pauseOnLostFocus = false;
         });
         return world;
      } catch (Throwable t) {
         System.err.println("=== GAMETEST WORLD LOAD FAILURE IN " + suiteName + ": " + t + " ===");
         for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread th = entry.getKey();
            String name = th.getName();
            if (name.contains("Server")
               || name.contains("Render")
               || name.contains("Netty")
               || name.contains("Worker")
               || name.contains("Gametest")
               || name.contains("Test")) {
               System.err.println("THREAD [" + name + "] state=" + th.getState());
               StackTraceElement[] frames = entry.getValue();
               int limit = Math.min(frames.length, 15);
               for (int i = 0; i < limit; i++) {
                  System.err.println("  at " + frames[i]);
               }
            }
         }
         throw t;
      }
   }
}
