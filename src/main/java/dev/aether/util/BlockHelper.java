package dev.aether.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockHelper {
   private BlockHelper() {
   }

   public static boolean isBlockAt(BlockPos pos, Block block) {
      Minecraft mc = Minecraft.getInstance();
      return mc.level != null && mc.level.getBlockState(pos).is(block);
   }

   public static boolean isAnchorCharged(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      return isBlockAt(pos, Blocks.RESPAWN_ANCHOR) && (Integer)mc.level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE) != 0;
   }

   public static boolean isAnchorUncharged(BlockPos pos) {
      Minecraft mc = Minecraft.getInstance();
      return isBlockAt(pos, Blocks.RESPAWN_ANCHOR) && (Integer)mc.level.getBlockState(pos).getValue(RespawnAnchorBlock.CHARGE) == 0;
   }

   public static void interactBlock(BlockHitResult hit, boolean swing) {
      interactBlock(hit, InteractionHand.MAIN_HAND, swing);
   }

   public static void interactBlock(BlockHitResult hit, InteractionHand hand, boolean swing) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.gameMode != null) {
         InteractionResult result = mc.gameMode.useItemOn(mc.player, hand, hit);
         if (result.consumesAction() && swing) {
            mc.player.swing(hand);
         }
      }
   }
}
