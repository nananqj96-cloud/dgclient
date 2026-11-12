package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.ModeSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class CoordSnapperModule extends Module {
   public final ModeSetting format = this.addSetting(
      new ModeSetting(
         "Format",
         "Clipboard format.",
         "X Y Z",
         "X Y Z",
         "JSON",
         "Command"
      )
   );
   public final ModeSetting target = this.addSetting(
      new ModeSetting(
         "Target",
         "Which coordinates to copy.",
         "Looked-at Block",
         "Looked-at Block",
         "Player"
      )
   );
   public final KeybindSetting copyKey = this.addSetting(
      new KeybindSetting("Copy Key", "Press to copy the coordinates.", -1)
   );
   public final BooleanSetting notify = this.addSetting(
      new BooleanSetting("Notify", "Show a confirmation when copied.", true)
   );
   private String lastCopied = "";

   public CoordSnapperModule() {
      super("CoordSnapper", "Copies looked-at coordinates with one key.", Category.MISC);
   }

   public String lastCopied() {
      return this.lastCopied;
   }

   @Override
   public boolean onKeyPress(int keyCode) {
      if (!this.copyKey.matches(keyCode)) {
         return false;
      } else {
         this.snap();
         return true;
      }
   }

   private void snap() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         BlockPos pos = this.resolvePos(mc);
         if (pos == null) {
            if (this.notify.get()) {
               AetherClient.notifications().pushInfo("CoordSnapper · no block in view");
            }
         } else {
            String var4 = this.format.get();

            String text = switch (var4) {
               case "JSON" -> "{\"x\": " + pos.getX() + ", \"y\": " + pos.getY() + ", \"z\": " + pos.getZ() + "}";
               case "Command" -> "/tp " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
               default -> pos.getX() + " " + pos.getY() + " " + pos.getZ();
            };
            mc.keyboardHandler.setClipboard(text);
            this.lastCopied = text;
            if (this.notify.get()) {
               AetherClient.notifications().pushInfo("Copied · " + text);
            }
         }
      }
   }

   private BlockPos resolvePos(Minecraft mc) {
      if (this.target.is("Player")) {
         return mc.player.blockPosition();
      } else {
         HitResult hit = mc.hitResult;
         if (hit instanceof BlockHitResult block && hit.getType() == Type.BLOCK) {
            return block.getBlockPos();
         }

         return null;
      }
   }
}
