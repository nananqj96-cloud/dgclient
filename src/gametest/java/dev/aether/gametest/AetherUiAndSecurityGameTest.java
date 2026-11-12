package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.config.ConfigShareService;
import dev.aether.config.ConfigStore;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.module.Category;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public class AetherUiAndSecurityGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();
      try {
         world.getClientWorld().waitForChunksRender();
         context.runOnClient(mc -> mc.options.pauseOnLostFocus = false);
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(4);

         TestServerContext server = world.getServer();
         server.runCommand("time set day");
         server.runCommand("weather clear");

         // 1. Verify Aether identity & zero-lag defaults
         context.runOnClient(mc -> {
            require("aetherclient".equals(AetherClient.MOD_ID), "MOD_ID must be aetherclient");
            require("Aether Client".equals(AetherClient.NAME), "NAME must be Aether Client");
            require(!AetherClient.modules().clickGui.blur.get(), "Background blur must default to OFF for zero-lag FPS");
            require("Dashboard".equals(AetherClient.modules().clickGui.layout.get()), "ClickGUI layout must default to Dashboard");
            require("Aether".equals(AetherClient.themes().current().getName()), "Default theme must be Aether");
            AetherClient.modules().fullbright.setEnabled(true);
            AetherClient.modules().storageEsp.setEnabled(true);
            AetherClient.modules().playerEsp.setEnabled(true);
         });
         context.waitTicks(4);
         context.takeScreenshot("aether-01-hud-overview");

         // 2. Verify 100% offline ConfigShareService round-trip
         context.runOnClient(mc -> {
            ConfigStore store = AetherClient.configStore();
            store.save(0);
            store.rename(0, "Tournament");
            String exportedJson = store.export(0);
            require(exportedJson != null && exportedJson.contains("aetherclient-config"), "Exported JSON must use aetherclient-config format");
            ConfigShareService.Result tokenRes = ConfigShareService.export(exportedJson).join();
            require(tokenRes.ok() && tokenRes.message().startsWith(ConfigShareService.PREFIX), "Offline export token must start with AETHER1:");
            ConfigShareService.Result decoded = ConfigShareService.importCode(tokenRes.message()).join();
            require(decoded.ok() && decoded.config() != null, "Offline importCode must decode AETHER1 token");
            ConfigStore.ImportResult slotRes = store.importInto(1, decoded.config());
            require(slotRes.ok(), "Decoded offline token must import cleanly into slot 1");
         });

         // 3. Verify Unified Sidebar Dashboard Window (Combat, Render + expanded settings, Themes Studio, Configs)
         context.runOnClient(mc -> {
            ClickGuiScreen.state().selectCategory(Category.COMBAT);
            ClickGuiScreen.state().setExpanded("AutoCrystal@COMBAT", true);
            mc.setScreen(new ClickGuiScreen());
         });
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(8);
         context.takeScreenshot("aether-02-dashboard-combat");

         context.runOnClient(mc -> {
            ClickGuiScreen.state().setExpanded("AutoCrystal@COMBAT", false);
            ClickGuiScreen.state().selectThemesTab();
         });
         context.waitTicks(6);
         context.takeScreenshot("aether-03-dashboard-themes");

         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               cg.configPanel().open();
            }
         });
         context.waitTicks(6);
         context.takeScreenshot("aether-04-config-profiles");

         // 4. Switch live to Redesigned Draggable Category Columns mode
         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               cg.configPanel().close();
               cg.toggleLayoutMode();
               require(!cg.isDashboardMode(), "toggleLayoutMode must switch to Columns mode");
            }
         });
         context.waitTicks(8);
         context.takeScreenshot("aether-05-columns-layout");

         // Switch back to Dashboard & close cleanly
         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               cg.toggleLayoutMode();
               require(cg.isDashboardMode(), "toggleLayoutMode must switch back to Dashboard mode");
            }
            AetherClient.modules().storageEsp.setEnabled(false);
            AetherClient.modules().playerEsp.setEnabled(false);
            mc.setScreen(null);
         });
         context.waitForScreen(null);
         context.waitTicks(2);
      } finally {
         world.close();
      }
   }

   private static void require(boolean condition, String msg) {
      if (!condition) {
         throw new AssertionError("FAILED: " + msg);
      }
   }
}
