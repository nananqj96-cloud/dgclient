package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.config.ConfigShareService;
import dev.aether.config.ConfigStore;
import dev.aether.corz.CorzModuleHandle;
import dev.aether.corz.CorzPortBus;
import dev.aether.corz.CorzPortProvider;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.module.Category;
import java.util.List;
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

         // 1. Verify Aether 2.1.0 identity, PinkBloom default theme, and live CorzPortBus bridge
         context.runOnClient(mc -> {
            require("aetherclient".equals(AetherClient.MOD_ID), "MOD_ID must be aetherclient");
            require("Aether Client".equals(AetherClient.NAME), "NAME must be Aether Client");
            require("2.1.0".equals(AetherClient.VERSION), "VERSION must be 2.1.0");
            require(!AetherClient.modules().clickGui.blur.get(), "Background blur must default to OFF for zero-lag FPS");
            require("Dashboard".equals(AetherClient.modules().clickGui.layout.get()), "ClickGUI layout must default to Dashboard");
            require("PinkBloom".equals(AetherClient.themes().current().getName()), "Default theme must be PinkBloom");
            require(AetherClient.themes().current().isBloom(), "PinkBloom theme must have bloom enabled");

            require(CorzPortBus.isAvailable(), "CorzPortBus must be registered by bundled corzport mod");
            CorzPortProvider corz = CorzPortBus.provider();
            List<CorzModuleHandle> corzMods = corz.modules();
            require(corzMods.size() >= 140, "Expected >= 140 bridged Corz modules, got " + corzMods.size());
            require(corz.categories().contains("Bots") && corz.categories().contains("DonutSMP"), "Corz categories must include Bots and DonutSMP");

            // Verify toggling a bridged Corz module works live
            CorzModuleHandle sample = corzMods.stream()
               .filter(m -> "Bots".equals(m.category()))
               .findFirst()
               .orElseThrow();
            boolean before = sample.isEnabled();
            sample.setEnabled(!before);
            require(sample.isEnabled() == !before, "CorzModuleHandle toggle must update live Corz module state");
            sample.setEnabled(before);

            AetherClient.modules().fullbright.setEnabled(true);
            AetherClient.modules().storageEsp.setEnabled(true);
            AetherClient.modules().playerEsp.setEnabled(true);
         });
         context.waitTicks(4);
         context.takeScreenshot("aether-01-pinkbloom-hud");

         // 2. Verify 100% offline ConfigShareService round-trip
         context.runOnClient(mc -> {
            ConfigStore store = AetherClient.configStore();
            store.save(0);
            store.rename(0, "PinkBloomPro");
            String exportedJson = store.export(0);
            require(exportedJson != null && exportedJson.contains("aetherclient-config"), "Exported JSON must use aetherclient-config format");
            ConfigShareService.Result tokenRes = ConfigShareService.export(exportedJson).join();
            require(tokenRes.ok() && tokenRes.message().startsWith(ConfigShareService.PREFIX), "Offline export token must start with AETHER1:");
            ConfigShareService.Result decoded = ConfigShareService.importCode(tokenRes.message()).join();
            require(decoded.ok() && decoded.config() != null, "Offline importCode must decode AETHER1 token");
            ConfigStore.ImportResult slotRes = store.importInto(1, decoded.config());
            require(slotRes.ok(), "Decoded offline token must import cleanly into slot 1");
         });

         // 3. Open PinkBloom Unified Sidebar Dashboard (showing "Corz Port" subtab in left rail)
         context.runOnClient(mc -> {
            ClickGuiScreen.state().setCorzPortOpen(false);
            ClickGuiScreen.state().selectCategory(Category.COMBAT);
            ClickGuiScreen.state().setExpanded("AutoCrystal@COMBAT", true);
            mc.setScreen(new ClickGuiScreen());
         });
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(8);
         context.takeScreenshot("aether-02-pinkbloom-dashboard");

         // 4. Launch Synced "Corz Port" Secondary UI via the "Corz Port" subtab
         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               ClickGuiScreen.state().setCorzSelectedCategory("Bots");
               CorzPortProvider corz = CorzPortBus.provider();
               if (corz != null) {
                  corz.modules().stream()
                     .filter(m -> "Bots".equals(m.category()) && !m.settings().isEmpty())
                     .findFirst()
                     .ifPresent(m -> cg.corzPortWindow().expandModule(m.id()));
               }
               cg.toggleCorzPort();
               require(ClickGuiScreen.state().isCorzPortOpen(), "Corz Port subtab must open secondary UI");
            }
         });
         context.waitTicks(10);
         context.takeScreenshot("aether-03-corz-port-synced-ui");

         // 5. Switch to Themes & Audio Studio while Corz Port stays synced in PinkBloom
         context.runOnClient(mc -> {
            ClickGuiScreen.state().setExpanded("AutoCrystal@COMBAT", false);
            ClickGuiScreen.state().selectThemesTab();
         });
         context.waitTicks(8);
         context.takeScreenshot("aether-04-pinkbloom-themes");

         // 6. Switch live to Draggable Category Columns mode with Corz Port synced
         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               cg.toggleLayoutMode();
               require(!cg.isDashboardMode(), "toggleLayoutMode must switch to Columns mode");
               require(cg.corzPortWindow().isVisible(), "Corz Port secondary UI must remain synced across layout modes");
            }
         });
         context.waitTicks(8);
         context.takeScreenshot("aether-05-columns-with-corz-port");

         // 7. Close ClickGuiScreen and verify BOTH main UI and Corz Port close together
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
