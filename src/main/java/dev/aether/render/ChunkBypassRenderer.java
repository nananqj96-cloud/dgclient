package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.ChunkBypassModule;
import dev.aether.util.Colors;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders Chunk Bypass flags with the main SusChunkFinder visual design — flat chunk quads at a
 * fixed render height, fade in/out, smart zone merging and centroid markers — but drawn in purple.
 */
public final class ChunkBypassRenderer {
   private static final int PURPLE = 0xA855F7;
   private static final int PURPLE_BRIGHT = 0xC084FC;
   private static final float FADE_IN_SECONDS = 0.3F;
   private static final float FADE_OUT_SECONDS = 0.45F;
   private static final float MOVE_RATE = 7.0F;
   private static final Map<Long, ChunkBypassRenderer.ChunkFade> chunkFades = new HashMap<>();
   private static final Map<Long, ChunkBypassRenderer.ZoneFade> zoneFades = new HashMap<>();
   private static long lastFrameNanos;

   private ChunkBypassRenderer() {
   }

   public static void reset() {
      chunkFades.clear();
      zoneFades.clear();
   }

   public static String debugState() {
      StringBuilder var0 = new StringBuilder();

      for (Entry<?, ?> var2 : chunkFades.entrySet()) {
         ChunkBypassRenderer.ChunkFade var3 = (ChunkBypassRenderer.ChunkFade)var2.getValue();
         var0.append(new ChunkPos((Long)var2.getKey()))
            .append("{a=")
            .append(var3.alpha)
            .append(",f=")
            .append(var3.flagged)
            .append("} ");
      }

      for (Entry<?, ?> var5 : zoneFades.entrySet()) {
         ChunkBypassRenderer.ZoneFade var6 = (ChunkBypassRenderer.ZoneFade)var5.getValue();
         var0.append("zone")
            .append(new ChunkPos((Long)var5.getKey()))
            .append("{a=")
            .append(var6.alpha)
            .append(",s=")
            .append(var6.size)
            .append(",f=")
            .append(var6.flagged)
            .append("} ");
      }

      return var0.toString();
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 camera, ChunkBypassModule module) {
      double range = module.range.get();
      double range2 = range * range;
      double renderY = module.renderY.get();
      float fillAlpha = module.fillOpacity.getFloat() / 255.0F;
      float outlineAlpha = module.outlineOpacity.getFloat() / 255.0F;
      boolean outline = module.outline.get();
      boolean smartMode = module.smartMode.get();
      int minHits = module.minHits();
      updateTargets(module, smartMode, minHits);
      float delta = frameDelta();
      float move = 1.0F - (float)Math.exp((double)(-delta * MOVE_RATE));
      Minecraft mc = Minecraft.getInstance();
      double px = mc.player != null ? mc.player.getX() : 0.0;
      double pz = mc.player != null ? mc.player.getZ() : 0.0;

      Iterator<?> var15 = chunkFades.entrySet().iterator();

      while (var15.hasNext()) {
         Entry<?, ?> var16 = (Entry<?, ?>)var15.next();
         ChunkBypassRenderer.ChunkFade var17 = (ChunkBypassRenderer.ChunkFade)var16.getValue();
         var17.alpha = var17.alpha + (var17.flagged ? delta / FADE_IN_SECONDS : -delta / FADE_OUT_SECONDS);
         var17.alpha = Math.clamp(var17.alpha, 0.0F, 1.0F);
         if (!var17.flagged && var17.alpha <= 0.0F) {
            var15.remove();
         } else {
            long key = (Long)var16.getKey();
            double x = (double)ChunkPos.getX(key) * 16.0;
            double z = (double)ChunkPos.getZ(key) * 16.0;
            double dx = px - (x + 8.0);
            double dz = pz - (z + 8.0);
            if (dx * dx + dz * dz <= range2) {
               drawQuad(bufferSource, poseStack, camera, x, z, x + 16.0, z + 16.0, renderY, PURPLE, fillAlpha * var17.alpha, outline ? outlineAlpha * var17.alpha : 0.0F);
            }
         }
      }

      Iterator<?> var22 = zoneFades.entrySet().iterator();

      while (var22.hasNext()) {
         Entry<?, ?> var23e = (Entry<?, ?>)var22.next();
         ChunkBypassRenderer.ZoneFade var23 = (ChunkBypassRenderer.ZoneFade)var23e.getValue();
         var23.alpha = var23.alpha + (var23.flagged ? delta / FADE_IN_SECONDS : -delta / FADE_OUT_SECONDS);
         var23.alpha = Math.clamp(var23.alpha, 0.0F, 1.0F);
         if (!var23.flagged && var23.alpha <= 0.0F) {
            var22.remove();
         } else {
            var23.centerX = var23.centerX + (var23.targetX - var23.centerX) * (double)move;
            var23.centerZ = var23.centerZ + (var23.targetZ - var23.centerZ) * (double)move;
            var23.size = var23.size + (var23.targetSize - var23.size) * move;
            double dx = px - var23.centerX;
            double dz = pz - var23.centerZ;
            if (dx * dx + dz * dz <= range2) {
               double half = (double)var23.size / 2.0;
               drawQuad(
                  bufferSource,
                  poseStack,
                  camera,
                  var23.centerX - half,
                  var23.centerZ - half,
                  var23.centerX + half,
                  var23.centerZ + half,
                  renderY,
                  PURPLE,
                  fillAlpha * var23.alpha,
                  outline ? outlineAlpha * var23.alpha : 0.0F
               );
               if (module.centroidMarker.get()) {
                  drawCircleMarker(bufferSource, poseStack, camera, var23.centerX, var23.centerZ, renderY + 0.05, 2.0, Colors.withAlpha(PURPLE_BRIGHT, 0.95F * var23.alpha));
               }
            }
         }
      }

      FlatOverlay.flush(bufferSource);
   }

   private static void drawQuad(
      BufferSource bufferSource,
      PoseStack poseStack,
      Vec3 camera,
      double x0,
      double z0,
      double x1,
      double z1,
      double y,
      int color,
      float fillAlpha,
      float outlineAlpha
   ) {
      FlatOverlay.fillQuad(bufferSource, poseStack, camera, x0, z0, x1, z1, y, Colors.withAlpha(color, fillAlpha));
      if (outlineAlpha > 0.004F) {
         int edge = Colors.withAlpha(color, outlineAlpha);
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, z0, x1, z0, y, edge, 2.5F);
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, z1, x1, z1, y, edge, 2.5F);
         FlatOverlay.edge(bufferSource, poseStack, camera, x0, z0, x0, z1, y, edge, 2.5F);
         FlatOverlay.edge(bufferSource, poseStack, camera, x1, z0, x1, z1, y, edge, 2.5F);
      }
   }

   private static void drawCircleMarker(BufferSource bufferSource, PoseStack poseStack, Vec3 camera, double x, double z, double y, double radius, int color) {
      int segments = 24;
      int fill = Colors.withAlpha(color, 0.24F);

      for (int i = 0; i < segments; i++) {
         double a0 = (double)i * Math.PI * 2.0 / (double)segments;
         double a1 = (double)(i + 1) * Math.PI * 2.0 / (double)segments;
         FlatOverlay.edge(
            bufferSource,
            poseStack,
            camera,
            x + Math.cos(a0) * radius * 1.32,
            z + Math.sin(a0) * radius * 1.32,
            x + Math.cos(a1) * radius * 1.32,
            z + Math.sin(a1) * radius * 1.32,
            y,
            color,
            3.0F
         );
         FlatOverlay.edge(
            bufferSource,
            poseStack,
            camera,
            x + Math.cos(a0) * radius,
            z + Math.sin(a0) * radius,
            x + Math.cos(a1) * radius,
            z + Math.sin(a1) * radius,
            y,
            color,
            2.4F
         );
      }

      FlatOverlay.fillQuad(bufferSource, poseStack, camera, x - 0.22, z - 0.22, x + 0.22, z + 0.22, y, color);
   }

   private static void updateTargets(ChunkBypassModule module, boolean smartMode, int minHits) {
      for (ChunkBypassRenderer.ChunkFade fade : chunkFades.values()) {
         fade.flagged = false;
      }

      for (ChunkBypassRenderer.ZoneFade fade : zoneFades.values()) {
         fade.flagged = false;
      }

      if (smartMode) {
         for (ChunkBypassModule.Zone zone : module.zones()) {
            if (zone.members().size() == 1) {
               long key = zone.members().iterator().next();
               ChunkBypassRenderer.ChunkFade fade = chunkFades.computeIfAbsent(key, k -> new ChunkBypassRenderer.ChunkFade());
               fade.flagged = true;
            } else {
               long min = Long.MAX_VALUE;

               for (long member : zone.members()) {
                  min = Math.min(min, member);
               }

               ChunkBypassRenderer.ZoneFade fade = zoneFades.get(min);
               if (fade == null) {
                  fade = new ChunkBypassRenderer.ZoneFade(zone.centroidX(), zone.centroidZ());
                  zoneFades.put(min, fade);
               }

               fade.flagged = true;
               fade.targetX = zone.centroidX();
               fade.targetZ = zone.centroidZ();
               fade.targetSize = Math.min(48.0F, 16.0F + (float)(zone.members().size() - 1) * 8.0F);
            }
         }
      } else {
         for (ChunkBypassModule.CellFlag flag : module.flags()) {
            if (flag.hits() >= minHits) {
               ChunkBypassRenderer.ChunkFade fade = chunkFades.computeIfAbsent(flag.cellKey(), k -> new ChunkBypassRenderer.ChunkFade());
               fade.flagged = true;
            }
         }
      }
   }

   private static float frameDelta() {
      long now = System.nanoTime();
      float delta = lastFrameNanos == 0L ? 0.016F : (float)(now - lastFrameNanos) / 1.0E9F;
      lastFrameNanos = now;
      return Math.min(delta, 0.1F);
   }

   private static final class ChunkFade {
      float alpha;
      boolean flagged;
   }

   private static final class ZoneFade {
      double centerX;
      double centerZ;
      double targetX;
      double targetZ;
      float size = 16.0F;
      float targetSize = 16.0F;
      float alpha;
      boolean flagged;

      ZoneFade(double x, double z) {
         this.centerX = this.targetX = x;
         this.centerZ = this.targetZ = z;
      }
   }
}
