package dev.aether.render;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.Modules;
import dev.aether.module.impl.NameProtectModule;
import dev.aether.module.impl.NameTagsModule;
import dev.aether.module.impl.SpawnerNametagsModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.theme.Theme;
import dev.aether.util.Colors;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import org.joml.Matrix3x2fStack;

public final class WorldNametagRenderer {
   private static final int SPAWNER_ACCENT = -22733;
   private static final int MAX_TAGS = 80;

   private WorldNametagRenderer() {
   }

   public static void render(NVGRenderer nVGRenderer) {
      if (WorldProjection.isValid()) {
         ModuleManager var1 = AetherClient.modules();
         if (var1 != null) {
            Minecraft var2 = Minecraft.getInstance();
            ClientLevel var3 = var2.level;
            LocalPlayer var4 = var2.player;
            if (var3 != null && var4 != null) {
               NameTagsModule var5 = var1.nameTags;
               SpawnerNametagsModule var6 = var1.spawnerNametags;
               boolean var7 = var5 != null && var5.isEnabled();
               boolean var8 = false;
               if (var7 || var8) {
                  float var9 = WorldProjection.partialTick();
                  Theme var10 = AetherClient.themes().current();
                  Modules.HudModule var11 = var1.hud;
                  int var12 = var11 != null && !var11.themeSync.get() ? var11.listColor.get() : var10.accent();
                  ArrayList var13 = new ArrayList();
                  if (var7) {
                     double var14 = var5.range.get();
                     double var16 = var14 * var14;
                     float var18 = var5.scale.getFloat();
                     float var19 = (float)(var5.opacity.get() / 100.0);
                     if (var5.players.get()) {
                        NameProtectModule var20 = var1.nameProtect;
                        boolean var21 = var20 != null && var20.isEnabled();
                        boolean var22 = var5.self.get() && !var2.options.getCameraType().isFirstPerson();

                        for (AbstractClientPlayer var24 : var3.players()) {
                           boolean var25 = var24 == var4;
                           if ((!var25 || var22) && !var24.isSpectator() && var24.isAlive()) {
                              double var26 = var4.distanceToSqr(var24);
                              if (var25 || !(var26 > var16)) {
                                 String var28 = var24.getGameProfile().name();
                                 String var29;
                                 if (var21 && (var29 = var20.replacementForDisplay(var28)) != null) {
                                    var28 = var29;
                                 }

                                 float var30 = -1.0F;
                                 float var31;
                                 if (var5.health.get() && (var31 = var24.getMaxHealth()) > 0.0F) {
                                    var30 = Mth.clamp(var24.getHealth() / var31, 0.0F, 1.0F);
                                 }

                                 String var32 = !var25 && var5.distance.get() ? (int)Math.sqrt(var26) + "m" : null;
                                 if (!var28.isEmpty() || var32 != null || !(var30 < 0.0F)) {
                                    var13.add(entityTag(var24, var9, Math.sqrt(var26), var28, var32, var12, var30, var18, var19));
                                 }
                              }
                           }
                        }
                     }

                     if (var5.items.get()) {
                        for (Entity var41 : var3.entitiesForRendering()) {
                           ItemStack var43;
                           double var44;
                           ItemEntity var46;
                           if (var41 instanceof ItemEntity
                              && (var46 = (ItemEntity)var41).isAlive()
                              && !((var44 = var4.distanceToSqr(var46)) > var16)
                              && !(var43 = var46.getItem()).isEmpty()) {
                              String var48 = itemSuffix(var43.getCount(), var5.itemAmount.get(), var5.distance.get() ? (int)Math.sqrt(var44) + "m" : null);
                              var13.add(entityTag(var46, var9, Math.sqrt(var44), var43.getHoverName().getString(), var48, var12, -1.0F, var18, var19));
                           }
                        }
                     }
                  }

                  if (var8) {
                     int var33 = var6.color.get();

                     for (BlockPos var36 : var6.visiblePositions()) {
                        double var17 = (double)var36.getX() + 0.5;
                        double var39 = (double)var36.getY() + 1.35;
                        double var42 = (double)var36.getZ() + 0.5;
                        double var45 = var4.distanceToSqr(var17, (double)var36.getY() + 0.5, var42);
                        String var47 = (int)Math.sqrt(var45) + "m";
                        var13.add(
                           new WorldNametagRenderer.Tag(Math.sqrt(var45), var17, var39, var42, spawnerName(var3, var36), var47, var33, -1.0F, 1.0F, 0.95F)
                        );
                     }
                  }

                  if (!var13.isEmpty()) {
                     var13.sort(Comparator.comparingDouble(WorldNametagRenderer.Tag::dist));
                     int var34 = Math.min(var13.size(), 80);

                     for (int var35 = var34 - 1; var35 >= 0; var35--) {
                        WorldNametagRenderer.Tag var37 = (WorldNametagRenderer.Tag)var13.get(var35);
                        float[] var38 = WorldProjection.project(var37.wx, var37.wy, var37.wz);
                        if (var38 != null) {
                           drawTag(nVGRenderer, var10, var38[0], var38[1], var37);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static WorldNametagRenderer.Tag entityTag(Entity entity, float f, double d, String string, String string2, int i, float g, float h, float j) {
      double var10 = Mth.lerp((double)f, entity.xOld, entity.getX());
      double var12 = Mth.lerp((double)f, entity.yOld, entity.getY()) + (double)entity.getBbHeight() + 0.5;
      double var14 = Mth.lerp((double)f, entity.zOld, entity.getZ());
      return new WorldNametagRenderer.Tag(d, var10, var12, var14, string, string2, i, g, h, j);
   }

   private static String itemSuffix(int i, boolean bl, String string) {
      StringBuilder var3 = new StringBuilder();
      if (bl && i > 1) {
         var3.append('x').append(i);
      }

      if (string != null) {
         if (var3.length() > 0) {
            var3.append("  ");
         }

         var3.append(string);
      }

      return var3.length() == 0 ? null : var3.toString();
   }

   private static String spawnerName(ClientLevel clientLevel, BlockPos blockPos) {
      try {
         BlockEntity var2 = clientLevel.getBlockEntity(blockPos);
         if (var2 instanceof SpawnerBlockEntity var6) {
            Entity var7 = var6.getSpawner().getOrCreateDisplayEntity(clientLevel, blockPos);
            return var7 != null ? var7.getType().getDescription().getString() + " Spawner" : "Spawner";
         }

         if (var2 instanceof TrialSpawnerBlockEntity var3) {
            Entity var4 = var3.getTrialSpawner().getStateData().getOrCreateDisplayEntity(var3.getTrialSpawner(), clientLevel, var3.getState());
            return var4 != null ? "Trial: " + var4.getType().getDescription().getString() : "Trial Spawner";
         }
      } catch (Exception var5) {
      }

      return "Spawner";
   }

   private static float pillHeight(float f, boolean bl) {
      float var2 = 12.5F * f + 3.5F * f * 2.0F;
      return var2 + (bl ? 3.0F * f + 3.5F * f : 0.0F);
   }

   public static void renderEquipment(GuiGraphics guiGraphics) {
      if (WorldProjection.isValid()) {
         ModuleManager var1 = AetherClient.modules();
         if (var1 != null) {
            NameTagsModule var2 = var1.nameTags;
            if (var2 != null && var2.isEnabled() && var2.players.get()) {
               boolean var3 = var2.armor.get();
               boolean var4 = var2.heldItem.get();
               if (var3 || var4) {
                  Minecraft var5 = Minecraft.getInstance();
                  if (var5.screen == null && !var5.options.hideGui) {
                     ClientLevel var6 = var5.level;
                     LocalPlayer var7 = var5.player;
                     if (var6 != null && var7 != null) {
                        float var8 = WorldProjection.partialTick();
                        double var9 = var2.range.get();
                        double var11 = var9 * var9;
                        float var13 = var2.scale.getFloat();
                        float var14 = pillHeight(var13, var2.health.get());
                        float var15 = OverlayRenderer.uiScale();
                        double var16 = (double)var5.getWindow().getGuiScale();
                        boolean var18 = var2.self.get() && !var5.options.getCameraType().isFirstPerson();
                        ArrayList var19 = new ArrayList();

                        for (AbstractClientPlayer var21 : var6.players()) {
                           boolean var22 = var21 == var7;
                           if ((!var22 || var18) && !var21.isSpectator() && var21.isAlive() && (var22 || !(var7.distanceToSqr(var21) > var11))) {
                              var19.clear();
                              if (var3) {
                                 addItem(var19, var21.getItemBySlot(EquipmentSlot.HEAD));
                                 addItem(var19, var21.getItemBySlot(EquipmentSlot.CHEST));
                                 addItem(var19, var21.getItemBySlot(EquipmentSlot.LEGS));
                                 addItem(var19, var21.getItemBySlot(EquipmentSlot.FEET));
                              }

                              if (var4) {
                                 addItem(var19, var21.getMainHandItem());
                                 addItem(var19, var21.getItemBySlot(EquipmentSlot.OFFHAND));
                              }

                              float[] var23;
                              if (!var19.isEmpty()
                                 && (
                                       var23 = WorldProjection.projectRaw(
                                          Mth.lerp((double)var8, var21.xOld, var21.getX()),
                                          Mth.lerp((double)var8, var21.yOld, var21.getY()) + (double)var21.getBbHeight() + 0.5,
                                          Mth.lerp((double)var8, var21.zOld, var21.getZ())
                                       )
                                    )
                                    != null) {
                                 float var24 = (float)((double)var23[0] / var16);
                                 float var25 = (float)((double)(var23[1] - var14 * var15) / var16) - 3.0F;
                                 float var26 = 11.0F * var13;
                                 float var27 = var26 + 1.5F;
                                 float var28 = (float)var19.size() * var26 + (float)(var19.size() - 1) * 1.5F;
                                 float var29 = var24 - var28 / 2.0F;
                                 float var30 = var25 - var26;
                                 Matrix3x2fStack var31 = guiGraphics.pose();

                                 for (int var32 = 0; var32 < var19.size(); var32++) {
                                    ItemStack var33 = (ItemStack)var19.get(var32);
                                    var31.pushMatrix();
                                    var31.translate(var29 + (float)var32 * var27, var30);
                                    var31.scale(var26 / 16.0F, var26 / 16.0F);
                                    guiGraphics.renderItem(var21, var33, 0, 0, 0);
                                    guiGraphics.renderItemDecorations(var5.font, var33, 0, 0);
                                    var31.popMatrix();
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void addItem(List<ItemStack> list, ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         list.add(itemStack);
      }
   }

   private static void drawTag(NVGRenderer nVGRenderer, Theme theme, float f, float g, WorldNametagRenderer.Tag tag) {
      float var5 = tag.scale;
      int var6 = Colors.lighten(tag.accent, 0.35F);
      float var7 = 12.5F * var5;
      float var8 = 10.0F * var5;
      float var9 = 6.0F * var5;
      float var10 = 3.5F * var5;
      float var11 = 5.0F * var5;
      boolean var12 = tag.healthFrac >= 0.0F;
      float var13 = 3.0F * var5;
      float var14 = nVGRenderer.textWidth(tag.name, var7);
      float var15 = tag.suffix != null ? var11 + nVGRenderer.textWidth(tag.suffix, var8) : 0.0F;
      float var16 = var14 + var15 + var9 * 2.0F;
      float var17 = var7 + var10 * 2.0F;
      float var18 = var17 + (var12 ? var13 + var10 : 0.0F);
      float var19 = f - var16 / 2.0F;
      float var20 = g - var18;
      float var21 = Math.min(6.0F * var5, var18 / 2.0F);
      boolean var22 = tag.opacity < 0.999F;
      if (var22) {
         nVGRenderer.save();
         nVGRenderer.alpha(tag.opacity);
      }

      nVGRenderer.glow(var19, var20, var16, var18, var21, 4.0F, Colors.withAlpha(tag.accent, 0.12F));
      nVGRenderer.rectGradient(var19, var20, var16, var18, var21, theme.background(), theme.backgroundTo(), true);
      float var23 = var20 + var10 + var7 / 2.0F;
      float var24 = var19 + var9;
      var24 += nVGRenderer.textGradient(tag.name, var24, var23, var7, var6, tag.accent);
      if (tag.suffix != null) {
         float var25;
         nVGRenderer.text(tag.suffix, var25 = var24 + var11, var23, var8, theme.textMuted());
      }

      if (var12) {
         float var30 = var20 + var17;
         float var26 = var19 + var9;
         float var27 = var16 - var9 * 2.0F;
         nVGRenderer.rect(var26, var30, var27, var13, var13 / 2.0F, Colors.withAlpha(-16777216, 0.55F));
         int var28 = Colors.lerp(-2080450, -11671924, tag.healthFrac);
         nVGRenderer.rect(var26, var30, Math.max(var13, var27 * tag.healthFrac), var13, var13 / 2.0F, var28);
      }

      if (var22) {
         nVGRenderer.restore();
      }
   }

   private static record Tag(double dist, double wx, double wy, double wz, String name, String suffix, int accent, float healthFrac, float scale, float opacity) {
   }
}
