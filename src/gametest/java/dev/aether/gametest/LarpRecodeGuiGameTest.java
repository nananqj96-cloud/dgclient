package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.gui.LarpRecodeGui;
import dev.aether.module.Module;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

/**
 * Renders the Larp Recode GUI in all four themes (Ocean, Cherry Blossom, Green Forest, Charcoal)
 * plus one settings-expanded view, and captures PNG screenshots as CI artifacts.
 *
 * <p>The screenshots are saved by the Fabric gametest framework to
 * {@code build/run/clientGameTest/screenshots/} and uploaded by the CI workflow.
 */
public class LarpRecodeGuiGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "LarpRecodeGuiGameTest");
      try {
         world.getClientWorld().waitForChunksRender();
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(4);

         context.runOnClient(mc -> {
            AetherClient.modules().clickGui.larpRecodeGui.set(true);
            mc.setScreen(new ClickGuiScreen());
         });
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(3);

         ClickGuiScreen cg = null;
         context.runOnClient(mc -> {
            cg = (ClickGuiScreen) mc.screen;
         });
         require(cg != null, "ClickGuiScreen must be open");

         LarpRecodeGui larp = cg.larpGui();
         require(larp != null, "LarpRecodeGui must be accessible");
         require(larp.themeCount() == 4, "LarpRecodeGui must have 4 themes, got " + larp.themeCount());

         // Capture each theme.
         String[] themeNames = {"Ocean", "Cherry Blossom", "Green Forest", "Charcoal"};
         for (int t = 0; t < larp.themeCount(); t++) {
            context.runOnClient(mc -> larp.setThemeIndex(t));
            context.waitTicks(2);
            context.takeScreenshot("larp-recode-" + (t + 1) + "-" + themeNames[t].replace(" ", "-"));
         }

         // Capture a settings-expanded view (Ocean theme, expand DL Sus Finder).
         context.runOnClient(mc -> {
            larp.setThemeIndex(0);
            Module dlSus = AetherClient.modules().dlSusFinder;
            if (dlSus != null) {
               larp.toggleExpanded(dlSus);
            }
         });
         context.waitTicks(2);
         context.takeScreenshot("larp-recode-5-settings-expanded");

         // Clean up.
         context.runOnClient(mc -> {
            AetherClient.modules().clickGui.larpRecodeGui.set(false);
            mc.setScreen(null);
         });
         context.waitTicks(2);
      } finally {
         world.close();
      }
   }

   private static void require(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("FAILED: " + what);
      }
   }
}
