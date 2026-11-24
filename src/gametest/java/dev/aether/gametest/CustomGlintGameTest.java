package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.impl.CustomGlintModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class CustomGlintGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "CustomGlintGameTest");

      try {
         context.runOnClient(mc -> {
            CustomGlintModule cg = AetherClient.modules().customGlint;
            require(cg != null, "CustomGlint registered");
            cg.setEnabled(false);
            require(!cg.isEnabled(), "CustomGlint disabled by default in test");
            cg.setEnabled(true);
            require(cg.isEnabled(), "CustomGlint enabled in test");
            cg.setEnabled(false);
         });
      } catch (Throwable var71) {
         if (world != null) {
            try {
               world.close();
            } catch (Throwable var61) {
               var71.addSuppressed(var61);
            }
         }

         throw var71;
      }

      if (world != null) {
         world.close();
      }
   }

   private static void require(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("FAILED: " + what);
      }
   }
}
