package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.impl.ArmorTrimHiderModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

public class ArmorTrimHiderGameTest implements FabricClientGameTest {
   private static final String TRIM = "[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]";

   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();

      try {
         world.getClientWorld().waitForChunksRender();
         context.runOnClient(mc -> mc.options.pauseOnLostFocus = false);
         context.getInput().resizeWindow(1280, 720);
         context.waitTicks(2);
         TestServerContext server = world.getServer();
         context.runOnClient(mc -> require(AetherClient.modules().armorTrimHider != null, "ArmorTrimHider registered"));
         server.runCommand("gamemode creative @a");
         server.runCommand("time set day");
         server.runCommand("weather clear");
         server.runCommand("fill -8 0 -8 8 0 8 minecraft:white_concrete");
         server.runCommand("tp @a 0 1 0 0 0");
         server.runCommand(
            "item replace entity @a armor.head with minecraft:diamond_helmet[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @a armor.chest with minecraft:diamond_chestplate[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @a armor.legs with minecraft:diamond_leggings[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @a armor.feet with minecraft:diamond_boots[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand("summon minecraft:armor_stand 2.5 1 3.5 {ShowArms:1b,NoGravity:1b,Rotation:[180f,0f]}");
         server.runCommand(
            "item replace entity @e[type=armor_stand,limit=1] armor.head with minecraft:diamond_helmet[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @e[type=armor_stand,limit=1] armor.chest with minecraft:diamond_chestplate[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @e[type=armor_stand,limit=1] armor.legs with minecraft:diamond_leggings[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         server.runCommand(
            "item replace entity @e[type=armor_stand,limit=1] armor.feet with minecraft:diamond_boots[minecraft:trim={material:\"minecraft:gold\",pattern:\"minecraft:sentry\"}]"
         );
         context.runOnClient(mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            AetherClient.modules().blockOutline.setEnabled(false);
            AetherClient.modules().armorTrimHider.setEnabled(false);
         });
         context.waitTicks(10);
         context.takeScreenshot("atrim-01-baseline");
         context.runOnClient(mc -> {
            ArmorTrimHiderModule m = AetherClient.modules().armorTrimHider;
            m.mode.set("Hide");
            m.ownArmor.set(Boolean.valueOf(true));
            m.setEnabled(true);
            ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
            ArmorTrim any = m.mapTrim(chest, null);
            require(m.mapTrim(chest, any) == null, "Hide maps trim to null");
         });
         context.waitTicks(5);
         context.takeScreenshot("atrim-02-hide-all");
         context.runOnClient(mc -> AetherClient.modules().armorTrimHider.ownArmor.set(Boolean.valueOf(false)));
         context.waitTicks(5);
         context.takeScreenshot("atrim-03-hide-others-only");
         context.runOnClient(mc -> {
            ArmorTrimHiderModule m = AetherClient.modules().armorTrimHider;
            m.mode.set("Random");
            m.ownArmor.set(Boolean.valueOf(true));
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            ArmorTrim r1 = m.mapTrim(boots, null);
            ArmorTrim r2 = m.mapTrim(boots, null);
            require(r1 != null, "Random adds a trim to untrimmed armor");
            require(r1.equals(r2), "Random is stable for the same item");
         });
         context.waitTicks(5);
         context.takeScreenshot("atrim-04-random");
      } catch (Throwable var61) {
         if (world != null) {
            try {
               world.close();
            } catch (Throwable var5) {
               var61.addSuppressed(var5);
            }
         }

         throw var61;
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
