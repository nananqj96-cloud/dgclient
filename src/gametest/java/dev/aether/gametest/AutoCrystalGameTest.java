package dev.aether.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aether.AetherClient;
import dev.aether.gui.widget.KeybindWidget;
import dev.aether.module.impl.AutoCrystalModule;
import dev.aether.settings.KeybindSetting;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class AutoCrystalGameTest implements FabricClientGameTest {
   private static final int TRIGGER_KEY = 71;
   private static final BlockPos BASE = new BlockPos(0, 1, 0);
   private static final AABB CRYSTAL_BOX = new AABB(-0.5, 1.5, -0.5, 1.5, 5.5, 2.5);

   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = GameTestWorldHelper.createWorld(context, "AutoCrystalGameTest");

      try {
         world.getClientWorld().waitForChunksRender();
         context.getInput().resizeWindow(1280, 720);
         context.waitTicks(2);
         TestServerContext server = world.getServer();
         context.runOnClient(mc -> require(AetherClient.modules().autoCrystal != null, "AutoCrystal registered"));
         this.buildArena(context, server, "obsidian");
         context.runOnClient(mc -> {
            AutoCrystalModule ac = AetherClient.modules().autoCrystal;
            ac.activateKey.set(Integer.valueOf(71));
            ac.placeDelay.set(0.0);
            ac.breakDelay.set(0.0);
            ac.obsidianDelay.set(0.0);
            ac.autoObsidian.set(Boolean.valueOf(true));
            ac.switchBack.set(Boolean.valueOf(false));
            ac.range.set(1.0);
            ac.setEnabled(true);
         });
         context.takeScreenshot("autocrystal-aim");
         context.runOnClient(mc -> require(isAimingAtBase(mc.hitResult), "aim lands on the base, got " + describeHit(mc.hitResult)));
         context.getInput().holdKey(71);
         context.waitTicks(6);
         int[] capturedId = new int[1];
         context.runOnClient(
            mc -> {
               List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, CRYSTAL_BOX);
               if (crystals.size() != 1) {
                  String diag = "hand="
                     + mc.player.getMainHandItem().getItem()
                     + " off="
                     + mc.player.getOffhandItem().getItem()
                     + " sel="
                     + mc.player.getInventory().getSelectedSlot()
                     + " base="
                     + mc.level.getBlockState(new BlockPos(0, 1, 0))
                     + " above="
                     + mc.level.getBlockState(new BlockPos(0, 2, 0))
                     + " trigHeld="
                     + InputConstants.isKeyDown(mc.getWindow(), 71)
                     + " enabled="
                     + AetherClient.modules().autoCrystal.isEnabled()
                     + " hit="
                     + describeHit(mc.hitResult);
                  require(false, "one crystal placed on the obsidian base, got " + crystals.size() + " | " + diag);
               }

               capturedId[0] = crystals.get(0).getId();
            }
         );
         context.takeScreenshot("autocrystal-placed");
         context.runOnClient(mc -> AetherClient.modules().autoCrystal.range.set(3.0));
         context.waitTicks(6);
         context.runOnClient(
            mc -> {
               List<EndCrystal> survivors = mc.level
                  .getEntitiesOfClass(EndCrystal.class, CRYSTAL_BOX)
                  .stream()
                  .filter(c -> c.getId() == capturedId[0])
                  .toList();
               if (!survivors.isEmpty()) {
                  double dist = mc.player.getEyePosition().distanceTo(survivors.get(0).position());
                  require(
                     false,
                     "crystal id "
                        + capturedId[0]
                        + " should have been broken | dist="
                        + String.format(Locale.ROOT, "%.2f", dist)
                        + " hit="
                        + describeHit(mc.hitResult)
                        + " hand="
                        + mc.player.getMainHandItem().getItem()
                  );
               }
            }
         );
         context.takeScreenshot("autocrystal-broken");
         context.getInput().releaseKey(71);
         context.waitTicks(2);
         this.buildArena(context, server, "stone");
         context.runOnClient(mc -> AetherClient.modules().autoCrystal.range.set(1.0));
         context.runOnClient(mc -> require(isAimingAtBase(mc.hitResult), "aim lands on the stone block, got " + describeHit(mc.hitResult)));
         context.getInput().holdKey(71);
         context.waitTicks(8);
         context.runOnClient(mc -> {
            boolean obiTop = mc.level.getBlockState(new BlockPos(0, 2, 0)).is(Blocks.OBSIDIAN);
            boolean obiSide = mc.level.getBlockState(new BlockPos(0, 1, 1)).is(Blocks.OBSIDIAN);
            require(obiTop || obiSide, "auto-obsidian laid a base (expected at 0,2,0 or 0,1,1)");
            int crystals = mc.level.getEntitiesOfClass(EndCrystal.class, new AABB(-0.5, 1.5, -0.5, 1.5, 6.5, 2.5)).size();
            require(crystals >= 1, "a crystal was placed on the auto-laid obsidian, got " + crystals);
         });
         context.takeScreenshot("autocrystal-obsidian");
         context.getInput().releaseKey(71);
         context.runOnClient(mc -> AetherClient.modules().autoCrystal.setEnabled(false));
         context.waitTicks(2);
         context.runOnClient(mc -> {
            KeybindSetting kb = new KeybindSetting("Test", "", -1);
            KeybindWidget widget = new KeybindWidget(AetherClient.themes(), kb);
            widget.setBounds(0.0F, 0.0F, 100.0F);
            require(widget.mouseClicked(10.0F, 10.0F, 0), "arming left-click is consumed");
            require(widget.isListening(), "widget listens after the arming click");
            require(widget.mouseClicked(10.0F, 10.0F, 1), "the bind right-click is consumed");
            require(!widget.isListening(), "widget stops listening once bound");
            require(kb.get() == 1, "right-click bound RMB, got " + kb.get());
            require(kb.keyName().equals("RMB"), "the bind shows as 'RMB', got " + kb.keyName());
            widget.mouseClicked(10.0F, 10.0F, 0);
            require(widget.keyPressed(86), "a keyboard key still binds");
            require(kb.get() == 86, "V bound, got " + kb.get());
            widget.mouseClicked(10.0F, 10.0F, 0);
            widget.keyPressed(256);
            require(kb.get() == -1, "ESC clears the bind");
         });
      } catch (Throwable var61) {
         if (world != null) {
            try {
               world.close();
            } catch (Throwable var51) {
               var61.addSuppressed(var51);
            }
         }

         throw var61;
      }

      if (world != null) {
         world.close();
      }
   }

   private void buildArena(ClientGameTestContext context, TestServerContext server, String baseBlock) {
      server.runCommand("kill @e[type=minecraft:end_crystal]");
      server.runCommand("fill -20 0 -20 20 0 20 minecraft:stone");
      server.runCommand("fill -20 1 -20 20 8 20 minecraft:air");
      server.runCommand("setblock 0 1 0 minecraft:" + baseBlock);
      server.runCommand("gamemode creative @a");
      server.runCommand("clear @a");
      server.runCommand("give @a minecraft:end_crystal 64");
      server.runCommand("give @a minecraft:obsidian 64");
      server.runCommand("tp @a 0.5 1 2.5 180 15");
      context.waitTicks(5);
      context.runOnClient(mc -> aimAt(mc.player, 0.5, 2.0, 0.5));
      context.waitTicks(3);
   }

   private static void aimAt(LocalPlayer player, double tx, double ty, double tz) {
      Vec3 eye = player.getEyePosition();
      double dx = tx - eye.x;
      double dy = ty - eye.y;
      double dz = tz - eye.z;
      double horiz = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horiz)));
      player.setYRot(yaw);
      player.setXRot(pitch);
      player.setYHeadRot(yaw);
   }

   private static boolean isAimingAtBase(HitResult hit) {
      if (hit instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK && bhr.getBlockPos().equals(BASE)) {
         return true;
      }

      return false;
   }

   private static String describeHit(HitResult hit) {
      if (hit instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
         return "block " + bhr.getBlockPos() + " face " + bhr.getDirection();
      }

      return hit == null ? "null" : hit.getType().toString();
   }

   private static void require(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("FAILED: " + what);
      }
   }
}
