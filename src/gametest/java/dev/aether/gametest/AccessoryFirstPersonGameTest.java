package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.impl.CustomAccessoriesModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;

public class AccessoryFirstPersonGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();

      try {
         world.getClientWorld().waitForChunksRender();
         context.runOnClient(mc -> mc.options.pauseOnLostFocus = false);
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(2);
         world.getServer().runCommand("tp @a 500 -60 500 -90 0");
         world.getClientWorld().waitForChunksRender();
         context.runOnClient(mc -> {
            AetherClient.modules().susChunkFinder.setEnabled(false);
            AetherClient.modules().blockOutline.setEnabled(false);
            AetherClient.modules().motionBlur.setEnabled(false);
            if (mc.player != null) {
               mc.player.setYRot(-90.0F);
               mc.player.setYBodyRot(-90.0F);
               mc.player.setXRot(0.0F);
            }

            CustomAccessoriesModule acc = AetherClient.modules().customAccessories;
            require(acc != null, "CustomAccessories registered");
            acc.firstPerson.set(Boolean.valueOf(false));
            acc.color.set(Integer.valueOf(-49508));
            acc.rainbow.set(Boolean.valueOf(false));
            acc.glow.set(75.0);
            acc.cape.set(Boolean.valueOf(true));
            acc.capeStyle.set("Aether");
            acc.capePhysics.set(Boolean.valueOf(true));
            acc.trail.set(Boolean.valueOf(false));
            acc.aura.set(Boolean.valueOf(true));
            acc.auraStyle.set("Orbit");
            acc.crown.set(Boolean.valueOf(true));
            acc.setEnabled(true);
         });
         context.waitTicks(20);
         context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
         context.waitTicks(6);
         context.takeScreenshot("accessories-fp-1-thirdperson-control");
         context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
         context.waitTicks(6);
         context.takeScreenshot("accessories-fp-2-firstperson-hidden");
         context.runOnClient(mc -> {
            if (mc.player != null) {
               mc.player.setXRot(60.0F);
            }
         });
         context.waitTicks(6);
         context.takeScreenshot("accessories-fp-3-firstperson-feet-aura-toggleoff");
         context.runOnClient(mc -> AetherClient.modules().customAccessories.firstPerson.set(Boolean.valueOf(true)));
         context.waitTicks(6);
         context.takeScreenshot("accessories-fp-4-firstperson-feet-toggleon");
         context.runOnClient(mc -> {
            CustomAccessoriesModule acc = AetherClient.modules().customAccessories;
            acc.firstPerson.set(Boolean.valueOf(false));
            acc.setEnabled(false);
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

   private static void require(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("FAILED: " + what);
      }
   }
}
