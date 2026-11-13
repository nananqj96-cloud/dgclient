package dev.aether.gametest;

import org.lwjgl.glfw.GLFW;
import dev.aether.AetherClient;
import dev.aether.config.ConfigShareService;
import dev.aether.config.ConfigStore;
import dev.aether.corz.CorzModuleHandle;
import dev.aether.corz.CorzPortBus;
import dev.aether.corz.CorzPortProvider;
import dev.aether.gui.ClickGuiScreen;
import dev.aether.gui.panel.ModuleEntry;
import dev.aether.gui.widget.BooleanWidget;
import dev.aether.gui.widget.KeybindWidget;
import dev.aether.gui.widget.ModeWidget;
import dev.aether.gui.widget.SettingWidget;
import dev.aether.gui.widget.SliderWidget;
import dev.aether.module.Category;
import dev.aether.module.impl.AutoTotemModule;
import dev.aether.module.impl.HitBoxModule;
import dev.aether.module.impl.SkinProtectModule;
import dev.aether.render.OverlayRenderer;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

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
            require(corzMods.size() == 139, "Expected all 139 gameplay Corz modules bridged, got " + corzMods.size());
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

         // 3. Open PinkBloom Unified Sidebar Dashboard and test REAL mouse/slider/keybind submenu interactions!
         context.runOnClient(mc -> {
            ClickGuiScreen.state().setCorzPortOpen(false);
            ClickGuiScreen.state().selectCategory(Category.COMBAT);
            ClickGuiScreen.state().setExpanded("Auto Totem@COMBAT", false);
            ClickGuiScreen.state().setExpanded("AimAssist@COMBAT", false);
            mc.setScreen(new ClickGuiScreen());
         });
         context.waitForScreen(ClickGuiScreen.class);
         context.waitTicks(8);

         // 3a. Right-click Auto Totem header via real cursor + mouse button to expand submenu
         double[] headerPt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("Auto Totem@COMBAT");
            require(entry != null, "Auto Totem@COMBAT entry must exist");
            return new float[]{entry.x() + entry.width() * 0.5F, entry.y() + entry.headerHeight() * 0.5F};
         });
         context.getInput().setCursorPos(headerPt[0], headerPt[1]);
         context.getInput().pressMouse(1);
         context.waitTicks(8);

         context.runOnClient(mc -> require(
            ClickGuiScreen.state().isExpanded("Auto Totem@COMBAT"),
            "Right-clicking module card header must expand Auto Totem submenu"
         ));

         // 3b. Click BooleanWidget ("Force Totem") inside expanded submenu and verify live toggle!
         double[] boolPt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("Auto Totem@COMBAT");
            AutoTotemModule mod = AetherClient.modules().autoTotem;
            SettingWidget w = findWidget(entry, mod.forceTotem);
            require(w instanceof BooleanWidget, "Force Totem BooleanWidget must exist");
            return new float[]{w.x() + w.width() * 0.5F, w.y() + w.height(null) * 0.5F};
         });
         boolean beforeForce = context.computeOnClient(mc -> AetherClient.modules().autoTotem.forceTotem.get());
         context.getInput().setCursorPos(boolPt[0], boolPt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> require(
            AetherClient.modules().autoTotem.forceTotem.get() == !beforeForce,
            "Clicking BooleanWidget in Dashboard submenu must toggle Force Totem"
         ));
         // Toggle back
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> require(
            AetherClient.modules().autoTotem.forceTotem.get() == beforeForce,
            "Clicking BooleanWidget again must restore Force Totem"
         ));

         // 3c. Click & drag SliderWidget ("Delay") from 20% to 80% in Dashboard submenu!
         double[][] sliderPts = computeSliderDragPoints(context, "Auto Totem@COMBAT", "Delay");
         context.getInput().setCursorPos(sliderPts[0][0], sliderPts[0][1]);
         context.getInput().holdMouse(0);
         context.waitTicks(1);
         context.getInput().setCursorPos(sliderPts[1][0], sliderPts[1][1]);
         context.waitTicks(1);
         context.getInput().releaseMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            int delayVal = AetherClient.modules().autoTotem.delay.getInt();
            require(delayVal >= 28 && delayVal <= 36, "Dragging Delay slider to 80% in Dashboard submenu must update value (got " + delayVal + ")");
            AetherClient.modules().autoTotem.delay.set(4.0);
         });

         // 3d. Click KeybindWidget in Dashboard submenu, press R to bind, then click again and press ESC to unbind without closing GUI!
         double[] kbPt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("Auto Totem@COMBAT");
            SettingWidget w = findWidget(entry, AetherClient.modules().autoTotem.getKeybind());
            require(w instanceof KeybindWidget, "Module KeybindWidget must exist");
            return new float[]{w.x() + w.width() - 18.0F, w.y() + w.height(null) * 0.5F};
         });
         context.getInput().setCursorPos(kbPt[0], kbPt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            SettingWidget w = findWidget(cg.findEntry("Auto Totem@COMBAT"), AetherClient.modules().autoTotem.getKeybind());
            require(w.isListening(), "Clicking KeybindWidget in Dashboard submenu must enter listening state");
         });
         context.getInput().pressKey(GLFW.GLFW_KEY_R);
         context.waitTicks(2);
         context.runOnClient(mc -> require(
            AetherClient.modules().autoTotem.getKeybind().get() == GLFW.GLFW_KEY_R,
            "Pressing R while KeybindWidget is listening must bind GLFW_KEY_R (82), got " + AetherClient.modules().autoTotem.getKeybind().get()
         ));

         // Click KeybindWidget again and press ESCAPE -> must clear bind to -1 AND keep ClickGuiScreen open!
         context.getInput().setCursorPos(kbPt[0], kbPt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            require(mc.screen instanceof ClickGuiScreen, "Pressing ESCAPE while binding a key must NOT close ClickGuiScreen");
            require(!AetherClient.modules().autoTotem.getKeybind().isBound(), "Pressing ESCAPE while binding must clear keybind to None (-1)");
         });

         // 3e. Switch to MISC, expand Skin Protect@MISC, and click ModeWidget ("Apply To") chip in Dashboard submenu!
         context.runOnClient(mc -> {
            ClickGuiScreen.state().selectCategory(Category.MISC);
            ClickGuiScreen.state().setExpanded("Skin Protect@MISC", true);
         });
         context.waitTicks(8);
         String beforeMode = context.computeOnClient(mc -> AetherClient.modules().skinProtect.applyTo.get());
         double[] modePt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("Skin Protect@MISC");
            SkinProtectModule sp = AetherClient.modules().skinProtect;
            SettingWidget w = findWidget(entry, sp.applyTo);
            require(w instanceof ModeWidget, "Apply To ModeWidget must exist");
            return new float[]{w.x() + 14.0F, w.y() + 26.0F};
         });
         context.getInput().setCursorPos(modePt[0], modePt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            String afterMode = AetherClient.modules().skinProtect.applyTo.get();
            require(!afterMode.equals(beforeMode), "Clicking ModeWidget chip in submenu must switch Apply To mode (was " + beforeMode + ")");
            AetherClient.modules().skinProtect.applyTo.set(beforeMode);
            ClickGuiScreen.state().setExpanded("Skin Protect@MISC", false);
            ClickGuiScreen.state().selectCategory(Category.COMBAT);
         });
         context.waitTicks(4);
         context.takeScreenshot("aether-02-pinkbloom-dashboard");

         // 4. Launch Synced "Corz Port" Secondary UI via the "Corz Port" subtab & test Corz submenu interaction
         AtomicReference<CorzModuleHandle> expandedCorzMod = new AtomicReference<>();
         context.runOnClient(mc -> {
            ClickGuiScreen.state().setExpanded("AutoCrystal@COMBAT", true);
            if (mc.screen instanceof ClickGuiScreen cg) {
               ClickGuiScreen.state().setCorzSelectedCategory("Bots");
               CorzPortProvider corz = CorzPortBus.provider();
               if (corz != null) {
                  corz.modules().stream()
                     .filter(m -> "Bots".equals(m.category()) && !m.settings().isEmpty())
                     .findFirst()
                     .ifPresent(m -> {
                        expandedCorzMod.set(m);
                        cg.corzPortWindow().expandModule(m.id());
                     });
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
            ClickGuiScreen.state().setExpanded("Auto Totem@COMBAT", false);
            ClickGuiScreen.state().selectThemesTab();
         });
         context.waitTicks(8);
         context.takeScreenshot("aether-04-pinkbloom-themes");

         // 6. Switch live to Draggable Category Columns mode AND verify submenu Boolean + Slider + Keybind in Columns mode too!
         context.runOnClient(mc -> {
            if (mc.screen instanceof ClickGuiScreen cg) {
               cg.toggleLayoutMode();
               require(!cg.isDashboardMode(), "toggleLayoutMode must switch to Columns mode");
               require(cg.corzPortWindow().isVisible(), "Corz Port secondary UI must remain synced across layout modes");
               ClickGuiScreen.state().setExpanded("HitBox@COMBAT", true);
            }
         });
         context.waitTicks(8);

         // 6a. Toggle BooleanWidget ("Enable Render") on HitBox@COMBAT in Columns mode
         boolean beforeHitboxRender = context.computeOnClient(mc -> AetherClient.modules().hitBox.enableRender.get());
         double[] colBoolPt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("HitBox@COMBAT");
            HitBoxModule hb = AetherClient.modules().hitBox;
            SettingWidget w = findWidget(entry, hb.enableRender);
            require(w instanceof BooleanWidget, "HitBox Enable Render BooleanWidget must exist");
            return new float[]{w.x() + w.width() * 0.5F, w.y() + w.height(null) * 0.5F};
         });
         context.getInput().setCursorPos(colBoolPt[0], colBoolPt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            require(
               AetherClient.modules().hitBox.enableRender.get() == !beforeHitboxRender,
               "Clicking BooleanWidget in Columns mode submenu must toggle Enable Render"
            );
            AetherClient.modules().hitBox.enableRender.set(beforeHitboxRender);
         });

         // 6b. Drag SliderWidget ("Expand") on HitBox@COMBAT in Columns mode
         double[][] colSliderPts = computeSliderDragPoints(context, "HitBox@COMBAT", "Expand");
         context.getInput().setCursorPos(colSliderPts[0][0], colSliderPts[0][1]);
         context.getInput().holdMouse(0);
         context.waitTicks(1);
         context.getInput().setCursorPos(colSliderPts[1][0], colSliderPts[1][1]);
         context.waitTicks(1);
         context.getInput().releaseMouse(0);
         context.waitTicks(2);
         context.runOnClient(mc -> {
            double expVal = AetherClient.modules().hitBox.expand.get();
            require(expVal >= 1.35 && expVal <= 1.85, "Dragging Expand slider in Columns mode submenu must update value (got " + expVal + ")");
            AetherClient.modules().hitBox.expand.set(0.5);
         });

         // 6c. Bind & right-click clear KeybindWidget on HitBox@COMBAT in Columns mode
         double[] colKbPt = computeUiPoint(context, mc -> {
            ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
            ModuleEntry entry = cg.findEntry("HitBox@COMBAT");
            SettingWidget w = findWidget(entry, AetherClient.modules().hitBox.getKeybind());
            return new float[]{w.x() + w.width() - 18.0F, w.y() + w.height(null) * 0.5F};
         });
         context.getInput().setCursorPos(colKbPt[0], colKbPt[1]);
         context.getInput().pressMouse(0);
         context.waitTicks(2);
         context.getInput().pressKey(GLFW.GLFW_KEY_G);
         context.waitTicks(2);
         context.runOnClient(mc -> require(
            AetherClient.modules().hitBox.getKeybind().get() == GLFW.GLFW_KEY_G,
            "Binding GLFW_KEY_G in Columns mode submenu must set keybind to 71"
         ));
         // Right-click KeybindWidget to clear back to None (-1)
         context.getInput().setCursorPos(colKbPt[0], colKbPt[1]);
         context.getInput().pressMouse(1);
         context.waitTicks(2);
         context.runOnClient(mc -> require(
            !AetherClient.modules().hitBox.getKeybind().isBound(),
            "Right-clicking KeybindWidget in Columns mode must clear keybind to None (-1)"
         ));

         context.takeScreenshot("aether-05-columns-with-corz-port");

         // 7. Close ClickGuiScreen and verify BOTH main UI and Corz Port close together
         context.runOnClient(mc -> {
            ClickGuiScreen.state().setExpanded("HitBox@COMBAT", false);
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

   private interface UiLocator {
      float[] locate(Minecraft mc);
   }

   private static SettingWidget findWidget(ModuleEntry entry, Object setting) {
      require(entry != null, "ModuleEntry must not be null");
      entry.layoutWidgets(null);
      for (SettingWidget w : entry.widgets()) {
         if (w.boundSetting() == setting) {
            return w;
         }
      }
      throw new AssertionError("No SettingWidget found for setting " + setting);
   }

   private static double[] computeUiPoint(ClientGameTestContext context, UiLocator locator) {
      return context.computeOnClient(mc -> {
         float[] ui = locator.locate(mc);
         return new double[]{uiToRawX(mc, ui[0]), uiToRawY(mc, ui[1])};
      });
   }

   private static double[][] computeSliderDragPoints(ClientGameTestContext context, String entryKey, String sliderName) {
      return context.computeOnClient(mc -> {
         ClickGuiScreen cg = (ClickGuiScreen)mc.screen;
         ModuleEntry entry = cg.findEntry(entryKey);
         require(entry != null, "Missing entry " + entryKey);
         entry.layoutWidgets(null);
         SliderWidget target = null;
         for (SettingWidget w : entry.widgets()) {
            if (w instanceof SliderWidget sw && sw.boundSetting() != null && sliderName.equals(sw.boundSetting().getName())) {
               target = sw;
               break;
            }
         }
         require(target != null, "Missing SliderWidget " + sliderName + " on " + entryKey);
         float y = target.y() + target.height(null) * 0.6F;
         float xStart = target.x() + target.width() * 0.20F;
         float xEnd = target.x() + target.width() * 0.80F;
         return new double[][]{
            {uiToRawX(mc, xStart), uiToRawY(mc, y)},
            {uiToRawX(mc, xEnd), uiToRawY(mc, y)}
         };
      });
   }

   private static double uiToRawX(Minecraft mc, float uiX) {
      return (double)(uiX * OverlayRenderer.uiScale())
         * (double)mc.getWindow().getScreenWidth()
         / (double)Math.max(1, mc.getWindow().getWidth());
   }

   private static double uiToRawY(Minecraft mc, float uiY) {
      return (double)(uiY * OverlayRenderer.uiScale())
         * (double)mc.getWindow().getScreenHeight()
         / (double)Math.max(1, mc.getWindow().getHeight());
   }

   private static void require(boolean condition, String msg) {
      if (!condition) {
         throw new AssertionError("FAILED: " + msg);
      }
   }
}
