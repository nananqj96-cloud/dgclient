package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.InventoryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.lwjgl.glfw.GLFW;

public class MaceSwapModule extends Module {
   public final BooleanSetting windBurst = this.addSetting(new BooleanSetting("Wind Burst", "", true));
   public final BooleanSetting breach = this.addSetting(new BooleanSetting("Breach", "", true));
   public final BooleanSetting onlySword = this.addSetting(new BooleanSetting("Only Sword", "", false));
   public final BooleanSetting onlyAxe = this.addSetting(new BooleanSetting("Only Axe", "", false));
   public final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", "", true));
   public final SliderSetting switchDelay = this.addSetting(new SliderSetting("Switch Delay", "", 0.0, 0.0, 20.0, 1.0));
   private int previousSlot = -1;
   private int tickCounter;
   private boolean waitingToSwapBack;
   private boolean attackedThisTick;

   public MaceSwapModule() {
      super("Mace Swap", "Switches to mace when attacking", Category.COMBAT);
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
      if (client.player != null && client.gameMode != null) {
         if (this.waitingToSwapBack) {
            this.tickCounter++;
            if (this.tickCounter >= this.switchDelay.getInt()) {
               InventoryHelper.swap(this.previousSlot);
               this.resetState();
            }
         } else {
            long handle = client.getWindow().handle();
            boolean lmb = GLFW.glfwGetMouseButton(handle, 0) == 1;
            if (lmb
               && client.screen == null
               && client.hitResult != null
               && client.hitResult.getType() == Type.ENTITY
               && !(client.player.getAttackStrengthScale(0.5F) < 1.0F)
               && (!this.onlySword.get() || this.isSword(client.player.getMainHandItem().getItem()))
               && (!this.onlyAxe.get() || client.player.getMainHandItem().getItem() instanceof AxeItem)
               && !client.player.getMainHandItem().is(Items.MACE)
               && InventoryHelper.getHotbarSlot(Items.MACE) != -1) {
               this.previousSlot = client.player.getInventory().getSelectedSlot();
               InventoryHelper.swapToItem(Items.MACE);
               EntityHitResult entityHit = (EntityHitResult)client.hitResult;
               client.gameMode.attack(client.player, entityHit.getEntity());
               client.player.swing(InteractionHand.MAIN_HAND);
               if (this.switchBack.get()) {
                  this.waitingToSwapBack = true;
                  this.tickCounter = 0;
               } else {
                  this.previousSlot = -1;
               }
            }
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

   private void resetState() {
      this.previousSlot = -1;
      this.tickCounter = 0;
      this.waitingToSwapBack = false;
      this.attackedThisTick = false;
   }
}
