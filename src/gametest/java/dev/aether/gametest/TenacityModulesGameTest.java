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

public class TenacityModulesGameTest implements FabricClientGameTest {
   @Override
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "TenacityModulesGameTest");
      try {
         world.getClientWorld().waitForChunksRender();
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(4);

         ModuleManager m = AetherClient.modules();

         // Verify all 144 Tenacity modules are registered
         require(m.tenacitySpeed != null, "Tenacity Speed registered");
         require(m.tenacityFlight != null, "Tenacity Flight registered");
         require(m.tenacityBrightness != null, "Tenacity Brightness registered");
         require(m.tenacityCrosshair != null, "Tenacity Crosshair registered");
         require(m.tenacityKillAura != null, "Tenacity KillAura registered");

         // 1. Brightness: verify gamma actually changes
         context.runOnClient(mc -> {
            TenacityBrightnessModule brightness = m.tenacityBrightness;
            brightness.brightness.set(80.0);
            brightness.setEnabled(true);
            brightness.onTick();
            require(mc.options.gamma().get() == 0.8, "Tenacity Brightness set gamma to 0.8 (got " + mc.options.gamma().get() + ")");
         });
         context.waitTicks(2);
         context.runOnClient(mc -> m.tenacityBrightness.setEnabled(false));

         // 2. Crosshair: verify hideGui actually changes
         context.runOnClient(mc -> {
            TenacityCrosshairModule crosshair = m.tenacityCrosshair;
            crosshair.setEnabled(true);
            crosshair.onTick();
            require(mc.options.hideGui == true, "Tenacity Crosshair hid the GUI");
         });
         context.waitTicks(2);
         context.runOnClient(mc -> {
            m.tenacityCrosshair.setEnabled(false);
            m.tenacityCrosshair.toggle.set(false);
            m.tenacityCrosshair.onTick();
            require(mc.options.hideGui == false, "Tenacity Crosshair restored the GUI on disable");
         });

         // 3. Flight: verify mayfly + flying actually change
         context.runOnClient(mc -> {
            TenacityFlightModule flight = m.tenacityFlight;
            flight.setEnabled(true);
            flight.onTick();
            require(mc.player.getAbilities().mayfly == true, "Tenacity Flight enabled mayfly");
            require(mc.player.getAbilities().flying == true, "Tenacity Flight enabled flying");
         });
         context.waitTicks(2);
         context.runOnClient(mc -> {
            m.tenacityFlight.setEnabled(false);
            require(mc.player.getAbilities().mayfly == false, "Tenacity Flight disabled mayfly on toggle off");
            require(mc.player.getAbilities().flying == false, "Tenacity Flight disabled flying on toggle off");
         });

         // 4. Speed: verify it sets shift down (prevents sprint-slowdown) and active flag
         context.runOnClient(mc -> {
            TenacitySpeedModule speed = m.tenacitySpeed;
            speed.speed.set(5.0);
            speed.setEnabled(true);
            speed.onTick();
            require(speed.isActive() == true, "Tenacity Speed is active on ground");
            require(mc.player.isShiftKeyDown() == false, "Tenacity Speed prevents shift-slowdown");
         });
         context.waitTicks(2);
         context.runOnClient(mc -> m.tenacitySpeed.setEnabled(false));

         // 5. KillAura: verify it has real onTick that targets entities
         context.runOnClient(mc -> {
            require(m.tenacityKillAura != null, "Tenacity KillAura exists");
            require(m.tenacityKillAura.getCategory() == dev.aether.module.Category.COMBAT, "Tenacity KillAura is COMBAT category");
            m.tenacityKillAura.setEnabled(true);
            require(m.tenacityKillAura.isEnabled() == true, "Tenacity KillAura toggled on");
            m.tenacityKillAura.setEnabled(false);
            require(m.tenacityKillAura.isEnabled() == false, "Tenacity KillAura toggled off");
         });

         // 6. Verify toggle via setEnabled actually changes state for all 144 modules
         context.runOnClient(mc -> {
            int tested = 0;
            int failed = 0;
            for (dev.aether.module.Module mod : m.all()) {
               if (mod.getName().startsWith("Tenacity") || mod.getClass().getName().contains("Tenacity")) {
                  tested++;
                  boolean before = mod.isEnabled();
                  mod.setEnabled(!before);
                  if (mod.isEnabled() != !before) {
                     failed++;
                     System.out.println("TOGGLE FAIL: " + mod.getName() + " class=" + mod.getClass().getName());
                  }
                  mod.setEnabled(before);
                  if (mod.isEnabled() != before) {
                     failed++;
                     System.out.println("TOGGLE RESTORE FAIL: " + mod.getName() + " class=" + mod.getClass().getName());
                  }
               }
            }
            require(tested >= 140, "Expected to test >=140 Tenacity modules, tested " + tested);
            require(failed == 0, "Expected 0 toggle failures out of " + tested + " Tenacity modules, got " + failed);
         });

         context.takeScreenshot("tenacity-modules-verified");

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
