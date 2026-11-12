package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;

public final class HomeMethodModule extends Module {
   public final KeybindSetting trigger = this.addSetting(new KeybindSetting("Trigger", "Runs the home sequence while the module is enabled.", -1));
   public final BooleanSetting requireChunkFinder = this.addSetting(
      new BooleanSetting("Require Chunk Finder", "Only starts automatically while rising over a Chunk Finder result.", true)
   );
   public final SliderSetting digY = this.addSetting(
      new SliderSetting("Dig Y", "Y level that starts the home 2, RTP and return sequence.", -2.0, -64.0, 64.0, 1.0)
   );
   private final Queue<HomeMethodModule.QueuedCommand> commands = new ArrayDeque<>();
   private boolean homeOneArmed;
   private boolean depthSequenceQueued;
   private boolean risingLatch;

   public HomeMethodModule() {
      super("Auto Home Method", "Fly over a result, save home 1, dig down, then run the return sequence.", Category.MISC);
   }

   @Override
   protected void onEnable() {
      this.resetSequence();
   }

   @Override
   protected void onDisable() {
      this.resetSequence();
   }

   @Override
   public boolean onKeyPress(int i) {
      if (!this.trigger.matches(i)) {
         return false;
      } else {
         this.startFlyOverSequence();
         return true;
      }
   }

   @Override
   public void onTick() {
      Minecraft var1 = Minecraft.getInstance();
      LocalPlayer var2 = var1.player;
      if (var2 != null && var1.level != null) {
         this.drainCommands(var1);
         boolean var3 = !var2.isFallFlying() && !var2.getAbilities().flying && !var2.onGround() && var2.getDeltaMovement().y > 0.05;
         if (var3 && !this.risingLatch && !this.homeOneArmed && this.commands.isEmpty() && this.allowedChunk(var2)) {
            this.startFlyOverSequence();
         }

         this.risingLatch = var3;
         if (this.homeOneArmed && !this.depthSequenceQueued && var2.getY() <= (double)this.digY.getInt()) {
            this.queueDepthSequence();
         }
      }
   }

   private boolean allowedChunk(LocalPlayer localPlayer) {
      if (!this.requireChunkFinder.get()) {
         return true;
      } else {
         ChunkFinderModule var2 = AetherClient.modules().chunkFinder;
         if (var2 != null && var2.isEnabled()) {
            ChunkPos var3 = localPlayer.chunkPosition();
            return var2.isFlagged(var3.x, var3.z);
         } else {
            return false;
         }
      }
   }

   private void startFlyOverSequence() {
      Minecraft var1 = Minecraft.getInstance();
      if (var1.player != null && var1.level != null) {
         this.commands.clear();
         long var2 = System.currentTimeMillis();
         this.commands.offer(new HomeMethodModule.QueuedCommand(var2, "delhome 1"));
         this.commands.offer(new HomeMethodModule.QueuedCommand(var2 + 350L, "sethome 1"));
         this.homeOneArmed = true;
         this.depthSequenceQueued = var1.player.getY() <= (double)this.digY.getInt();
         if (this.depthSequenceQueued) {
            this.queueDepthCommands(var2 + 700L);
         }

         if (AetherClient.notifications() != null) {
            AetherClient.notifications().pushInfo("Auto Home Method · home 1 queued, dig to Y " + this.digY.getInt());
         }
      }
   }

   private void queueDepthSequence() {
      this.depthSequenceQueued = true;
      this.queueDepthCommands(System.currentTimeMillis());
      if (AetherClient.notifications() != null) {
         AetherClient.notifications().pushInfo("Auto Home Method · dig depth reached, return queued");
      }
   }

   private void queueDepthCommands(long l) {
      this.commands.offer(new HomeMethodModule.QueuedCommand(l, "delhome 2"));
      this.commands.offer(new HomeMethodModule.QueuedCommand(l + 350L, "sethome 2"));
      this.commands.offer(new HomeMethodModule.QueuedCommand(l + 850L, "rtp"));
      this.commands.offer(new HomeMethodModule.QueuedCommand(l + 2100L, "home 2"));
      this.commands.offer(new HomeMethodModule.QueuedCommand(l + 3200L, "home 1"));
   }

   private void drainCommands(Minecraft minecraft) {
      long var2 = System.currentTimeMillis();

      while (!this.commands.isEmpty() && this.commands.peek().atMillis() <= var2) {
         HomeMethodModule.QueuedCommand var4 = this.commands.poll();
         if (minecraft.getConnection() != null) {
            minecraft.getConnection().sendCommand(var4.command());
         }

         if ("home 1".equals(var4.command())) {
            this.homeOneArmed = false;
         }
      }
   }

   private void resetSequence() {
      this.commands.clear();
      this.homeOneArmed = false;
      this.depthSequenceQueued = false;
      this.risingLatch = false;
   }

   private static record QueuedCommand(long atMillis, String command) {
   }
}
