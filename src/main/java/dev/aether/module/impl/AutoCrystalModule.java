package dev.aether.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.BlockHelper;
import dev.aether.util.InventoryHelper;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class AutoCrystalModule extends Module {
   public final KeybindSetting activateKey = this.addSetting(
      new KeybindSetting("Activate Key", "Hold to place + break (default RMB)", 1)
   );
   public final SliderSetting breakDelay = this.addSetting(
      new SliderSetting(
         "Break Delay", "Ticks between crystal breaks", 1.0, 0.0, 20.0, 1.0, "t"
      )
   );
   public final SliderSetting placeDelay = this.addSetting(
      new SliderSetting(
         "Place Delay", "Ticks between crystal places", 1.0, 0.0, 20.0, 1.0, "t"
      )
   );
   public final BooleanSetting autoObsidian = this.addSetting(
      new BooleanSetting("Auto Obsidian", "Lay an obsidian base when you're not aiming at one", true)
   );
   public final SliderSetting obsidianDelay = this.addSetting(
      new SliderSetting(
         "Obsidian Delay",
         "Ticks between obsidian placements",
         2.0,
         0.0,
         20.0,
         1.0,
         "t"
      )
   );
   public final SliderSetting range = this.addSetting(
      new SliderSetting(
         "Break Range",
         "Max distance to a crystal you'll break (vanilla reach ~3)",
         3.0,
         1.0,
         6.0,
         0.5,
         "m"
      )
   );
   public final BooleanSetting switchBack = this.addSetting(
      new BooleanSetting("Switch Back", "Return to your original hotbar slot when released", true)
   );
   private int breakCooldown;
   private int placeCooldown;
   private int obsidianCooldown;
   private int savedSlot = -1;
   private int lastBrokenId = -1;
   private boolean wasActive;

   public AutoCrystalModule() {
      super(
         "Auto Crystal",
         "Hold RMB to auto place + break crystals, laying an obsidian base when needed",
         Category.COMBAT
      );
   }

   @Override
   protected void onEnable() {
      this.resetAll();
   }

   @Override
   protected void onDisable() {
      this.restoreSlot();
      this.resetAll();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.gameMode != null && mc.level != null && mc.screen == null && this.isTriggerHeld(mc)) {
         this.wasActive = true;
         this.tickCooldowns();
         if (this.activateKey.get() == 1) {
            mc.options.keyUse.setDown(false);
         }

         HitResult hitResult = mc.hitResult;
         if (hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystal crystal) {
            if (this.tryBreak(mc, crystal)) {
               this.lastBrokenId = crystal.getId();
            }

            return;
         }

         if (hitResult instanceof BlockHitResult hit && hit.getType() == Type.BLOCK) {
            BlockPos clicked = hit.getBlockPos();
            if (isCrystalBase(mc.level, clicked)) {
               this.serviceBase(mc, hit, clicked);
            } else if (this.autoObsidian.get()) {
               this.layObsidian(mc, hit, clicked);
            }
         }
      } else {
         this.endActiveHold();
      }
   }

   private void serviceBase(Minecraft mc, BlockHitResult hit, BlockPos base) {
      EndCrystal crystal = crystalOn(mc.level, base);
      if (crystal != null) {
         if (this.tryBreak(mc, crystal)) {
            this.lastBrokenId = crystal.getId();
         }
      } else {
         this.lastBrokenId = -1;
         if (this.placeCooldown == 0 && canFitCrystal(mc.level, base)) {
            InteractionHand hand = this.equip(mc, Items.END_CRYSTAL);
            if (hand != null) {
               BlockHelper.interactBlock(hit, hand, true);
               this.placeCooldown = this.placeDelay.getInt();
            }
         }
      }
   }

   private void layObsidian(Minecraft mc, BlockHitResult hit, BlockPos clicked) {
      if (this.obsidianCooldown == 0) {
         BlockPos target = mc.level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(hit.getDirection());
         if (mc.level.getBlockState(target).canBeReplaced() && canFitCrystal(mc.level, target)) {
            InteractionHand hand = this.equip(mc, Items.OBSIDIAN);
            if (hand != null) {
               BlockHelper.interactBlock(hit, hand, true);
               this.obsidianCooldown = Math.max(1, this.obsidianDelay.getInt());
            }
         }
      }
   }

   private boolean tryBreak(Minecraft mc, EndCrystal crystal) {
      if (this.breakCooldown != 0) {
         return false;
      } else if (crystal.getId() == this.lastBrokenId) {
         return false;
      } else if (mc.player.getEyePosition().distanceTo(crystal.position()) > this.range.get()) {
         return false;
      } else {
         mc.gameMode.attack(mc.player, crystal);
         mc.player.swing(InteractionHand.MAIN_HAND);
         this.breakCooldown = this.breakDelay.getInt();
         return true;
      }
   }

   @Nullable
   private InteractionHand equip(Minecraft mc, Item item) {
      LocalPlayer player = mc.player;
      if (player.getMainHandItem().is(item)) {
         return InteractionHand.MAIN_HAND;
      } else if (player.getOffhandItem().is(item)) {
         return InteractionHand.OFF_HAND;
      } else {
         int slot = InventoryHelper.getHotbarSlot(item);
         if (slot < 0) {
            return null;
         } else {
            if (this.savedSlot < 0) {
               this.savedSlot = player.getInventory().getSelectedSlot();
            }

            InventoryHelper.selectHotbarSlot(slot);
            return InteractionHand.MAIN_HAND;
         }
      }
   }

   private static boolean isCrystalBase(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
   }

   @Nullable
   private static EndCrystal crystalOn(Level level, BlockPos base) {
      BlockPos up = base.above();
      AABB region = new AABB(
         (double)up.getX() + 0.125,
         (double)up.getY() - 0.1,
         (double)up.getZ() + 0.125,
         (double)up.getX() + 0.875,
         (double)up.getY() + 2.5,
         (double)up.getZ() + 0.875
      );
      List<EndCrystal> found = level.getEntitiesOfClass(EndCrystal.class, region, e -> true);
      return found.isEmpty() ? null : found.get(0);
   }

   private static boolean canFitCrystal(Level level, BlockPos base) {
      BlockPos up = base.above();
      if (!level.getBlockState(up).isAir()) {
         return false;
      } else {
         AABB box = new AABB((double)up.getX(), (double)up.getY(), (double)up.getZ(), (double)up.getX() + 1.0, (double)up.getY() + 2.0, (double)up.getZ() + 1.0);
         return level.getEntitiesOfClass(Entity.class, box).isEmpty();
      }
   }

   private void tickCooldowns() {
      if (this.breakCooldown > 0) {
         this.breakCooldown--;
      }

      if (this.placeCooldown > 0) {
         this.placeCooldown--;
      }

      if (this.obsidianCooldown > 0) {
         this.obsidianCooldown--;
      }
   }

   private boolean isTriggerHeld(Minecraft mc) {
      int key = this.activateKey.get();
      if (key == -1) {
         return false;
      } else {
         return key <= 7 ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), key) == 1 : InputConstants.isKeyDown(mc.getWindow(), key);
      }
   }

   private void endActiveHold() {
      if (this.wasActive) {
         this.restoreSlot();
         this.breakCooldown = 0;
         this.placeCooldown = 0;
         this.obsidianCooldown = 0;
         this.lastBrokenId = -1;
         this.wasActive = false;
      }
   }

   private void restoreSlot() {
      if (this.switchBack.get() && this.savedSlot >= 0) {
         InventoryHelper.selectHotbarSlot(this.savedSlot);
      }

      this.savedSlot = -1;
   }

   private void resetAll() {
      this.breakCooldown = 0;
      this.placeCooldown = 0;
      this.obsidianCooldown = 0;
      this.savedSlot = -1;
      this.lastBrokenId = -1;
      this.wasActive = false;
   }
}
