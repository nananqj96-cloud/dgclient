package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.BlockHelper;
import dev.aether.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public class DoubleAnchorModule extends Module {
   public final KeybindSetting activateKey = this.addSetting(
      new KeybindSetting("Activate Key", "Hold to run the combo", 72)
   );
   public final SliderSetting timing = this.addSetting(
      new SliderSetting(
         "Timing", "Delay between the two blows", 120.0, 40.0, 400.0, 10.0, "ms"
      )
   );
   public final SliderSetting detonateSlot = this.addSetting(
      new SliderSetting(
         "Detonate Slot",
         "Hotbar slot to hold while detonating (anything but glowstone)",
         1.0,
         1.0,
         9.0,
         1.0
      )
   );
   public final BooleanSetting rePlace = this.addSetting(
      new BooleanSetting(
         "Re-Place",
         "Place a fresh anchor for the second blow if the first is consumed",
         true
      )
   );
   public final BooleanSetting switchBack = this.addSetting(
      new BooleanSetting("Switch Back", "Return to the previous hotbar slot when done", true)
   );
   private int step;
   private int tickCounter;
   private BlockPos targetPos;
   private int previousSlot = -1;

   public DoubleAnchorModule() {
      super("Double Anchor", "Two rapid anchor charge→detonate cycles", Category.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @Override
   public void onTick() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null && client.level != null && client.screen == null) {
         if (this.step > 0) {
            this.processStep(client);
         } else if (this.keyDown(client) && client.hitResult instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
            BlockPos pos = hit.getBlockPos();
            if (BlockHelper.isBlockAt(pos, Blocks.RESPAWN_ANCHOR)) {
               this.targetPos = pos.immutable();
               this.previousSlot = client.player.getInventory().getSelectedSlot();
               this.step = 1;
               this.tickCounter = 0;
            }
         }
      } else if (this.step != 0) {
         this.resetState();
      }
   }

   private void processStep(Minecraft client) {
      this.tickCounter++;
      BlockHitResult live = client.hitResult instanceof BlockHitResult bhr ? bhr : null;
      switch (this.step) {
         case 1:
            if (!this.lookingAtTarget(live)) {
               this.resetState();
               return;
            }

            if (BlockHelper.isAnchorUncharged(this.targetPos)) {
               InventoryHelper.swapToItem(Items.GLOWSTONE);
               BlockHelper.interactBlock(live, true);
            }

            this.advance(2);
            break;
         case 2:
            if (!this.lookingAtTarget(live)) {
               this.resetState();
               return;
            }

            InventoryHelper.swap(this.detonateSlot.getInt() - 1);
            BlockHelper.interactBlock(live, true);
            this.advance(3);
            break;
         case 3:
            if (this.tickCounter >= this.gapTicks()) {
               this.advance(4);
            }
            break;
         case 4:
            if (BlockHelper.isBlockAt(this.targetPos, Blocks.RESPAWN_ANCHOR)) {
               if (this.lookingAtTarget(live) && BlockHelper.isAnchorUncharged(this.targetPos)) {
                  InventoryHelper.swapToItem(Items.GLOWSTONE);
                  BlockHelper.interactBlock(live, true);
               }

               this.advance(5);
            } else if (this.rePlace.get() && live != null && InventoryHelper.getHotbarSlot(Items.RESPAWN_ANCHOR) >= 0) {
               InventoryHelper.swapToItem(Items.RESPAWN_ANCHOR);
               BlockHelper.interactBlock(live, true);
               this.targetPos = live.getBlockPos().relative(live.getDirection()).immutable();
               this.advance(6);
            } else {
               this.finish(client);
            }
            break;
         case 5:
            if (this.lookingAtTarget(live) && BlockHelper.isBlockAt(this.targetPos, Blocks.RESPAWN_ANCHOR)) {
               InventoryHelper.swap(this.detonateSlot.getInt() - 1);
               BlockHelper.interactBlock(live, true);
            }

            this.finish(client);
            break;
         case 6:
            if (BlockHelper.isBlockAt(this.targetPos, Blocks.RESPAWN_ANCHOR)) {
               InventoryHelper.swapToItem(Items.GLOWSTONE);
               if (live != null) {
                  BlockHelper.interactBlock(live, true);
               }

               this.advance(5);
            } else {
               this.finish(client);
            }
            break;
         default:
            this.finish(client);
      }
   }

   private boolean lookingAtTarget(BlockHitResult live) {
      return live != null && live.getType() == Type.BLOCK && live.getBlockPos().equals(this.targetPos);
   }

   private void advance(int next) {
      this.step = next;
      this.tickCounter = 0;
   }

   private int gapTicks() {
      return Math.max(1, this.timing.getInt() / 50);
   }

   private void finish(Minecraft client) {
      if (this.switchBack.get() && this.previousSlot >= 0) {
         InventoryHelper.swap(this.previousSlot);
      }

      this.resetState();
   }

   private boolean keyDown(Minecraft client) {
      int key = this.activateKey.get();
      if (key == -1) {
         return false;
      } else {
         long handle = client.getWindow().handle();
         return key <= 7 ? GLFW.glfwGetMouseButton(handle, key) == 1 : GLFW.glfwGetKey(handle, key) == 1;
      }
   }

   private void resetState() {
      this.step = 0;
      this.tickCounter = 0;
      this.targetPos = null;
      this.previousSlot = -1;
   }
}
