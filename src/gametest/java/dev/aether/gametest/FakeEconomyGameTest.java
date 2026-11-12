package dev.aether.gametest;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.FakePayModule;
import dev.aether.module.impl.FakeStatsModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;

public class FakeEconomyGameTest implements FabricClientGameTest {
   public void runTest(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder().create();

      try {
         world.getClientWorld().waitForChunksRender();
         context.runOnClient(mc -> mc.options.pauseOnLostFocus = false);
         context.getInput().resizeWindow(1600, 900);
         context.waitTicks(2);
         TestServerContext server = world.getServer();
         context.runOnClient(mc -> {
            ModuleManager m = AetherClient.modules();
            require(m.fakePay != null, "FakePay registered");
            require(m.fakeStats != null, "FakeStats registered");
         });
         server.runCommand("scoreboard objectives add donut dummy {\"text\":\"DonutSMP\",\"color\":\"gold\"}");
         server.runCommand("scoreboard objectives modify donut numberformat blank");
         server.runCommand("scoreboard objectives setdisplay sidebar donut");
         addLine(server, "t1", "$ ", "green", "100", 6);
         addLine(server, "t2", "* ", "light_purple", "200", 5);
         addLine(server, "t3", "# ", "red", "300", 4);
         addLine(server, "t4", "% ", "gray", "400", 3);
         addLine(server, "t5", "= ", "yellow", "5m5s", 2);
         server.runCommand("scoreboard players set play.donutsmp.net donut 1");
         context.waitTicks(5);
         context.runOnClient(mc -> {
            FakeStatsModule fs = AetherClient.modules().fakeStats;
            fs.money.set("777m");
            fs.moneyLine.set(1.0);
            fs.shards.set("9,999");
            fs.shardsLine.set(2.0);
            fs.kills.set("1,000");
            fs.killsLine.set(3.0);
            fs.deaths.set("0");
            fs.deathsLine.set(4.0);
            fs.playtime.set("365d 12h");
            fs.playtimeLine.set(5.0);
            fs.setEnabled(true);
            fs.beginSidebar();
            require(draw(fs, "$ 100").contains("777M"), "line 1 = money abbreviated");
            require(draw(fs, "* 200").equals("* 9,999"), "line 2 = shards");
            require(draw(fs, "# 300").equals("# 1,000"), "line 3 = kills");
            require(draw(fs, "% 400").equals("% 0"), "line 4 = deaths");
            require(draw(fs, "= 5m5s").equals("= 365d 12h"), "line 5 = playtime");
            require(draw(fs, "play.donutsmp.net").equals("play.donutsmp.net"), "line 6 = footer untouched");
         });
         context.waitTicks(2);
         context.takeScreenshot("fakestats-icon-sidebar");
         context.runOnClient(mc -> {
            FakeStatsModule fs = AetherClient.modules().fakeStats;
            FakePayModule fp = AetherClient.modules().fakePay;
            fs.money.set("1m");
            fp.currency.set("$");
            fp.feedback.set("Both");
            fp.setEnabled(true);
            double before = fs.getLiveBalance();
            require(before == 1000000.0, "live balance seeded to 1m, got " + before);
            mc.player.connection.sendCommand("pay Notch 250k");
            require(fs.getLiveBalance() == 750000.0, "pay deducted 250k: 1m -> " + fs.getLiveBalance());
         });
         context.waitTicks(2);
         context.takeScreenshot("fakepay-paid-newcolors");
         context.runOnClient(mc -> {
            FakeStatsModule fs = AetherClient.modules().fakeStats;
            mc.player.connection.sendCommand("pay Notch 999m");
            require(fs.getLiveBalance() == 750000.0, "insufficient pay left balance untouched");
            mc.player.connection.sendCommand("pay " + mc.player.getGameProfile().name() + " 1");
            require(fs.getLiveBalance() == 750000.0, "self-pay blocked");
         });
         context.runOnClient(mc -> mc.player.connection.sendCommand("bal"));
         context.waitTicks(2);
         context.takeScreenshot("fakestats-bal-command");
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

   private static void addLine(TestServerContext server, String team, String prefix, String color, String holder, int score) {
      server.runCommand("team add " + team);
      server.runCommand("team modify " + team + " prefix {\"text\":\"" + prefix + "\",\"color\":\"" + color + "\"}");
      server.runCommand("team join " + team + " " + holder);
      server.runCommand("scoreboard players set " + holder + " donut " + score);
   }

   private static String draw(FakeStatsModule fs, String line) {
      return fs.rewriteForDraw(Component.literal(line)).getString();
   }

   private static void require(boolean condition, String what) {
      if (!condition) {
         throw new AssertionError("FAILED: " + what);
      }
   }
}
