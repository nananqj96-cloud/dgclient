package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.impl.SpawnerProtectModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class SpawnerProtectGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "SpawnerProtectGameTest");

      try {
         context.runOnClient(mc -> {
            SpawnerProtectModule sp = AetherClient.modules().spawnerProtect;
            require(sp != null, "SpawnerProtect registered");
            require(sp.getTargetStackCount() == 3.0F, "default Stacks To Deposit = 3");
            require(sp.getScanRange() == 64.0F, "default Trigger Range = 64");
            require(sp.getBreakRange() == 5.5F, "default Break Range = 5.5");
            require(sp.getDetectBlockUpdates(), "Detect Block Updates on by default");
            sp.setEnabled(true);
            require(sp.isEnabled(), "SpawnerProtect enabled in test");
            sp.setEnabled(false);
            require(!sp.isEnabled(), "SpawnerProtect disabled in test");
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
