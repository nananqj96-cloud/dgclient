package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.StorageEspRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.IconListSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class StorageEspModule extends Module {
   public final ModeSetting mode = this.addSetting(
      new ModeSetting(
         "Mode",
         "Box style — hollow outline or translucent fill",
         "Outline",
         "Outline",
         "Full"
      )
   );
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Max distance a container is highlighted", 256.0, 16.0, 512.0, 8.0)
   );
   public final SliderSetting highlightAlpha = this.addSetting(
      new SliderSetting("Highlight Alpha", "Box opacity (0-255)", 200.0, 0.0, 255.0, 1.0)
   );
   public final BooleanSetting tracers = this.addSetting(
      new BooleanSetting("Tracers", "Draw lines from the crosshair to each container", false)
   );
   public final IconListSetting containers = this.addSetting(
      new IconListSetting("Containers", "Which container types to highlight")
   );
   public final BooleanSetting hideOpened;
   private final Set<BlockPos> interactedBlocks = new HashSet<>();

   public StorageEspModule() {
      super("StorageESP", "Highlights chests, barrels, shulkers", Category.RENDER);

      for (StorageEspModule.StorageType var4 : StorageEspModule.StorageType.values()) {
         this.containers.add(var4.key, var4.label, var4.icon, var4.defaultEnabled, var4.defaultColor);
      }

      this.hideOpened = this.addSetting(
         new BooleanSetting(
            "Hide Opened", "Don't highlight containers you've already opened", false
         )
      );
   }

   public boolean isTypeEnabled(StorageEspModule.StorageType var1) {
      return this.containers.isEnabled(var1.key);
   }

   public int colorFor(StorageEspModule.StorageType var1) {
      return this.containers.color(var1.key);
   }

   public boolean hideOpened() {
      return this.hideOpened.get();
   }

   public boolean isInteracted(int var1, int var2, int var3) {
      return !this.interactedBlocks.isEmpty() && this.interactedBlocks.contains(new BlockPos(var1, var2, var3));
   }

   public void clearInteractions() {
      this.interactedBlocks.clear();
   }

   public void trackInteraction(BlockPos var1) {
      if (var1 != null) {
         Minecraft var4 = Minecraft.getInstance();
         if (var4.level != null) {
            this.interactedBlocks.add(var1.immutable());
            BlockEntity var5 = var4.level.getBlockEntity(var1);
            ChestType var2;
            BlockState var3;
            if (var5 instanceof ChestBlockEntity
               && (var3 = var4.level.getBlockState(var1)).getBlock() instanceof ChestBlock
               && (var2 = (ChestType)var3.getValue(ChestBlock.TYPE)) != ChestType.SINGLE) {
               Direction var6 = (Direction)var3.getValue(ChestBlock.FACING);
               BlockPos var7 = var1.relative(var2 == ChestType.LEFT ? var6.getClockWise() : var6.getCounterClockWise());
               this.interactedBlocks.add(var7);
            }
         }
      }
   }

   @Override
   protected void onEnable() {
      this.interactedBlocks.clear();
   }

   @Override
   public void onTick() {
      StorageEspRenderer.scan(this);
   }

   @Override
   protected void onDisable() {
      StorageEspRenderer.clear();
   }

   public static enum StorageType {
      CHEST("chest", "Chest", Items.CHEST, -22016, true),
      TRAPPED("trapped", "Trapped Chest", Items.TRAPPED_CHEST, -65536, true),
      ENDER("ender", "Ender Chest", Items.ENDER_CHEST, -8912641, true),
      SHULKER("shulker", "Shulker Box", Items.SHULKER_BOX, -47873, true),
      BARREL("barrel", "Barrel", Items.BARREL, -7842560, true),
      SPAWNER("spawner", "Spawner", Items.SPAWNER, -16711936, true),
      HOPPER("hopper", "Hopper", Items.HOPPER, -7829368, false),
      FURNACE("furnace", "Furnace", Items.FURNACE, -7566196, false);

      final String key;
      final String label;
      final Item icon;
      final int defaultColor;
      final boolean defaultEnabled;

      private StorageType(String nullxx, String nullxxx, Item nullxxxx, int nullxxxxx, boolean nullxxxxxx) {
         this.key = nullxx;
         this.label = nullxxx;
         this.icon = nullxxxx;
         this.defaultColor = nullxxxxx;
         this.defaultEnabled = nullxxxxxx;
      }
   }
}
