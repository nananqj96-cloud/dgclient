package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;

public class TriggerbotModule extends Module {
   public final SliderSetting minDelay = this.addSetting(
      new SliderSetting("Min Delay", "", 9.0, 0.0, 20.0, 1.0)
   );
   public final SliderSetting maxDelay = this.addSetting(
      new SliderSetting("Max Delay", "", 11.0, 0.0, 20.0, 1.0)
   );
   public final BooleanSetting onlyItem = this.addSetting(new BooleanSetting("Only Item", "", false));
   public final ModeSetting itemFilter = this.addSetting(
      new ModeSetting(
         "Item Filter",
         "",
         "Sword",
         "Sword",
         "Axe",
         "Hand"
      )
   );
   public final BooleanSetting onlyCrit = this.addSetting(new BooleanSetting("Only Crit", "", false));
   public final BooleanSetting checkShield = this.addSetting(new BooleanSetting("Check Shield", "", false));
   private final Random random = new Random();
   private int tickCounter;
   private int currentDelay = 10;

   public TriggerbotModule() {
      super("Triggerbot", "Auto-attacks entities on crosshair", Category.COMBAT);
   }

   @Override
   protected void onEnable() {
      this.tickCounter = 0;
      this.randomizeDelay();
   }

   @Override
   protected void onDisable() {
      this.tickCounter = 0;
   }

   @Override
   public void onTick() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && client.gameMode != null && client.screen == null) {
         if (client.hitResult != null && client.hitResult.getType() == Type.ENTITY) {
            Entity target = ((EntityHitResult)client.hitResult).getEntity();
            if (target instanceof LivingEntity && target != client.player) {
               if (this.onlyItem.get()) {
                  if (this.itemFilter.is("Sword") && !this.isSword(client.player.getMainHandItem().getItem())) {
                     return;
                  }

                  if (this.itemFilter.is("Axe") && !(client.player.getMainHandItem().getItem() instanceof AxeItem)) {
                     return;
                  }

                  if (this.itemFilter.is("Hand") && !client.player.getMainHandItem().isEmpty()) {
                     return;
                  }
               }

               if ((!this.onlyCrit.get() || !client.player.onGround() && !(client.player.fallDistance <= 0.0))
                  && (!this.checkShield.get() || !(target instanceof Player targetPlayer) || !targetPlayer.isBlocking())
                  && !(client.player.getAttackStrengthScale(0.5F) < 1.0F)) {
                  this.tickCounter++;
                  if (this.tickCounter >= this.currentDelay) {
                     client.gameMode.attack(client.player, target);
                     client.player.swing(InteractionHand.MAIN_HAND);
                     this.tickCounter = 0;
                     this.randomizeDelay();
                  }
               }
            }
         } else {
            this.tickCounter = 0;
         }
      }
   }

   private boolean isSword(Item item) {
      return item == Items.WOODEN_SWORD
         || item == Items.STONE_SWORD
         || item == Items.IRON_SWORD
         || item == Items.GOLDEN_SWORD
         || item == Items.DIAMOND_SWORD
         || item == Items.NETHERITE_SWORD;
   }

   private void randomizeDelay() {
      int min = this.minDelay.getInt();
      int max = this.maxDelay.getInt();
      if (max < min) {
         max = min;
      }

      this.currentDelay = min + (max > min ? this.random.nextInt(max - min + 1) : 0);
   }
}
