package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class StaffListGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();

      try {
         world.getClientWorld().waitForChunksRender();
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            ModuleManager m = AetherClient.modules();
            require(m.staffList != null, "StaffList registered");
            m.staffList.setEnabled(true);
            require(m.staffList.isEnabled(), "StaffList enabled in test");
            m.staffList.setEnabled(false);
            require(!m.staffList.isEnabled(), "StaffList disabled in test");
         });
      } catch (Throwable var6) {
         if (world != null) {
            try {
               world.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (world != null) {
         world.close();
      }
   }

   private static void require(boolean condition, String message) {
      if (!condition) {
         throw new AssertionError(message);
      }
   }
}
