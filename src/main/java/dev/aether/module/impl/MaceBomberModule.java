package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class MaceBomberModule extends Module {
   public final SliderSetting height = this.addSetting(
      new SliderSetting(
         "Height",
         "Minimum fall distance before it smashes",
         8.0,
         3.0,
         30.0,
         1.0,
         "m"
      )
   );
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Dive style",
         "Direct",
         "Direct",
         "Spiral",
         "Delayed"
      )
   );
   public final BooleanSetting autoMace = this.addSetting(
      new BooleanSetting("Auto Mace", "Swap to a mace in your hotbar before smashing", true)
   );
   public final BooleanSetting switchBack = this.addSetting(
      new BooleanSetting("Switch Back", "Return to the previous slot after the smash", false)
   );
   private int spiralDir = 1;
   private int spiralTimer;
   private int cooldown;
   private int returnSlot = -1;
   private boolean strafing;

   public MaceBomberModule() {
      super("Mace Bomber", "Times a fully-charged mace smash on the way down", Category.COMBAT);
   }

   @Override
   protected void onDisable() {
      this.releaseStrafe();
      this.cooldown = 0;
      this.returnSlot = -1;
   }

   @Override
   public void onTick() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null && client.screen == null) {
         if (this.cooldown > 0) {
            this.cooldown--;
         }

         if (client.player.onGround()) {
            this.releaseStrafe();
            this.cooldown = 0;
            this.returnSlot = -1;
         } else {
            LivingEntity target = this.crosshairTarget(client);
            if (target == null) {
               this.releaseStrafe();
            } else {
               if (this.mode.is("Spiral")) {
                  this.spiral(client);
               } else {
                  this.releaseStrafe();
               }

               if (this.cooldown <= 0
                  && !(client.player.fallDistance < (double)this.height.getFloat())
                  && (!this.mode.is("Delayed") || !(client.player.getDeltaMovement().y > -0.5))) {
                  if (!client.player.getMainHandItem().is(Items.MACE)) {
                     if (!this.autoMace.get()) {
                        return;
                     }

                     if (this.returnSlot < 0) {
                        this.returnSlot = client.player.getInventory().getSelectedSlot();
                     }

                     if (!InventoryHelper.swapToItem(Items.MACE)) {
                        return;
                     }
                  }

                  if (!(client.player.getAttackStrengthScale(0.5F) < 1.0F)) {
                     client.gameMode.attack(client.player, target);
                     client.player.swing(InteractionHand.MAIN_HAND);
                     this.cooldown = 6;
                     if (this.switchBack.get() && this.returnSlot >= 0) {
                        InventoryHelper.swap(this.returnSlot);
                     }

                     this.returnSlot = -1;
                  }
               }
            }
         }
      } else {
         this.releaseStrafe();
      }
   }

   private LivingEntity crosshairTarget(Minecraft client) {
      if (client.hitResult != null && client.hitResult.getType() == Type.ENTITY) {
         if (!(((EntityHitResult)client.hitResult).getEntity() instanceof LivingEntity living)) {
            return null;
         } else {
            return living != client.player && living.isAlive() ? living : null;
         }
      } else {
         return null;
      }
   }

   private void spiral(Minecraft client) {
      if (--this.spiralTimer <= 0) {
         this.spiralDir = -this.spiralDir;
         this.spiralTimer = 6 + client.player.tickCount % 7;
      }

      client.options.keyLeft.setDown(this.spiralDir < 0);
      client.options.keyRight.setDown(this.spiralDir > 0);
      this.strafing = true;
   }

   private void releaseStrafe() {
      if (this.strafing) {
         Minecraft client = Minecraft.getInstance();
         if (client.options != null) {
            client.options.keyLeft.setDown(false);
            client.options.keyRight.setDown(false);
         }

         this.strafing = false;
      }
   }
}
