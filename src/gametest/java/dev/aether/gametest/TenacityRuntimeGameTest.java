package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.TenacityBrightnessModule;
import dev.aether.module.impl.TenacityCrosshairModule;
import dev.aether.module.impl.TenacityFlightModule;
import dev.aether.module.impl.TenacitySpeedModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

public class TenacityRuntimeGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "TenacityRuntimeGameTest");
      try {
         world.getClientWorld().waitForChunksRender();
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(4);

         ModuleManager m = AetherClient.modules();

         // ===== TEST 1: Brightness via REAL tick events =====
         // Enable the module, wait for actual client ticks to fire (not manual onTick calls),
         // then verify gamma actually changed. This proves onTick() executes via the event system.
         context.runOnClient(mc -> {
            m.tenacityBrightness.brightness.set(80.0);
            m.tenacityBrightness.setEnabled(true);
            require(m.tenacityBrightness.isEnabled(), "Brightness should be enabled");
            require(mc.options.gamma().get().floatValue() == 1.0F, "Gamma should still be default 1.0 before tick (got " + mc.options.gamma().get() + ")");
         });
         context.waitTicks(3); // Let END_CLIENT_TICK fire 3 times
         context.runOnClient(mc -> {
            float gamma = mc.options.gamma().get().floatValue();
            require(gamma != 1.0, "Brightness onTick must change gamma via real tick events (got " + gamma + ")");
            m.tenacityBrightness.setEnabled(false);
         });

         // ===== TEST 2: Flight via REAL tick events =====
         context.runOnClient(mc -> {
            require(mc.player.getAbilities().mayfly == false, "mayfly should be false before enabling");
            m.tenacityFlight.setEnabled(true);
            require(mc.player.getAbilities().mayfly == false, "mayfly should still be false before tick fires");
         });
         context.waitTicks(3);
         context.runOnClient(mc -> {
            require(mc.player.getAbilities().mayfly == true, "Flight onTick must set mayfly via real tick events");
            require(mc.player.getAbilities().flying == true, "Flight onTick must set flying via real tick events");
            m.tenacityFlight.setEnabled(false);
         });
         context.waitTicks(2);
         context.runOnClient(mc -> {
            require(mc.player.getAbilities().mayfly == false, "Flight onDisable must clear mayfly");
            require(mc.player.getAbilities().flying == false, "Flight onDisable must clear flying");
         });

         // ===== TEST 3: Crosshair via REAL tick events =====
         context.runOnClient(mc -> {
            require(mc.options.hideGui == false, "GUI should be visible before Crosshair");
            m.tenacityCrosshair.toggle.set(true);
            m.tenacityCrosshair.setEnabled(true);
            require(mc.options.hideGui == false, "hideGui should still be false before tick fires");
         });
         context.waitTicks(3);
         context.runOnClient(mc -> {
            require(mc.options.hideGui == true, "Crosshair onTick must hide GUI via real tick events");
            m.tenacityCrosshair.setEnabled(false);
            m.tenacityCrosshair.toggle.set(false);
         });
         context.waitTicks(3);
         context.runOnClient(mc -> {
            require(mc.options.hideGui == false, "Crosshair onDisable must restore GUI");
         });

         // ===== TEST 4: Speed via REAL tick events =====
         context.runOnClient(mc -> {
            m.tenacitySpeed.speed.set(5.0);
            m.tenacitySpeed.setEnabled(true);
         });
         context.waitTicks(3);
         context.runOnClient(mc -> {
            require(m.tenacitySpeed.isActive(), "Speed onTick must set active flag via real tick events");
            require(mc.player.isShiftKeyDown() == false, "Speed onTick must clear shift key");
            m.tenacitySpeed.setEnabled(false);
         });

         // ===== TEST 5: KillAura via REAL tick events =====
         // Verify a combat module's onTick runs without crashing when enabled
         context.runOnClient(mc -> {
            m.tenacityKillAura.setEnabled(true);
            require(m.tenacityKillAura.isEnabled(), "KillAura should be enabled");
         });
         context.waitTicks(5); // START_CLIENT_TICK fires onCombatTick
         context.runOnClient(mc -> {
            require(m.tenacityKillAura.isEnabled(), "KillAura should still be enabled after combat ticks");
            m.tenacityKillAura.setEnabled(false);
         });

         // ===== TEST 6: Verify NO Tenacity module throws on real tick =====
         // Enable ALL Tenacity modules, wait for ticks, verify none got auto-disabled or crashed
         context.runOnClient(mc -> {
            int count = 0;
            for (dev.aether.module.Module mod : m.all()) {
               if (mod.getClass().getName().contains("Tenacity")) {
                  mod.setEnabled(true);
                  count++;
               }
            }
            require(count >= 140, "Expected >=140 Tenacity modules, got " + count);
         });
         context.waitTicks(5);
         context.runOnClient(mc -> {
            int stillEnabled = 0;
            int disabled = 0;
            for (dev.aether.module.Module mod : m.all()) {
               if (mod.getClass().getName().contains("Tenacity")) {
                  if (mod.isEnabled()) stillEnabled++;
                  else { disabled++; System.out.println("DISABLED AFTER TICK: " + mod.getName()); }
               }
            }
            require(disabled == 0, "No Tenacity module should be disabled after ticks (disabled=" + disabled + ")");
            // Disable them all
            for (dev.aether.module.Module mod : m.all()) {
               if (mod.getClass().getName().contains("Tenacity")) mod.setEnabled(false);
            }
         });

         context.takeScreenshot("tenacity-runtime-verified");

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
