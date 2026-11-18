package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aether.AetherClient;
import dev.aether.module.Modules;
import dev.aether.suschunk.SusChunkScanner;
import dev.aether.util.Colors;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Renders SusChunkFinder highlights with a polished, modern design: pulsing glow halos,
 * center-out gradient fills, 3D box prisms for confirmed zones, animated scan ripples,
 * and tier-based color blending. Single-chunk flags get a glowing box; merged zones get
 * a larger gradient area with a pulsing centroid marker.
 *
 * <p>Design goals: readability over the world, smooth animations, and clear visual hierarchy
 * (glow → fill → outline → marker).
 */
public final class SusChunkRenderer {
   private static final float PULSE_SPEED = 2.5F;
   private static final float FADE_IN_SECONDS = 0.35F;
   private static final float FADE_OUT_SECONDS = 0.5F;
   private static final float MOVE_RATE = 7.0F;
   private static final Map<Long, ChunkFade> chunkFades = new HashMap<>();
   private static final Map<Long, ZoneFade> zoneFades = new HashMap<>();
   private static long lastFrameNanos;
   private static long scanStartNanos;

   private SusChunkRenderer() {
   }

   public static void reset() {
      chunkFades.clear();
      zoneFades.clear();
   }

   public static void resetScan() {
      scanStartNanos = System.nanoTime();
   }

   public static String debugState() {
      StringBuilder var0 = new StringBuilder();
      for (Entry var2 : chunkFades.entrySet()) {
         ChunkFade var3 = (ChunkFade)var2.getValue();
         var0.append(new ChunkPos((Long)var2.getKey()))
            .append("{a=").append(var3.alpha)
            .append(",t=").append(var3.tier)
            .append(",f=").append(var3.flagged).append("} ");
      }
      for (Entry var5 : zoneFades.entrySet()) {
         ZoneFade var6 = (ZoneFade)var5.getValue();
         var0.append("zone").append(new ChunkPos((Long)var5.getKey()))
            .append("{a=").append(var6.alpha)
            .append(",s=").append(var6.size)
            .append(",f=").append(var6.flagged).append("} ");
      }
      return var0.toString();
   }

   public static void render(BufferSource buffer, PoseStack pose, Vec3 cam, Modules.SusChunkFinderModule mod) {
      SusChunkScanner scanner = mod.scanner;
      double renderY = mod.renderY.get();
      int accent = AetherClient.themes().current().accent();
      int accentBright = AetherClient.themes().current().accentBright();
      float fillOp = mod.fillOpacity.getFloat() / 255.0F;
      float outlineOp = mod.outlineOpacity.getFloat() / 255.0F;
      boolean outline = mod.outline.get();
      boolean smart = mod.smartMode.get();
      int threshold = scanner.threshold();

      updateTargets(scanner, smart, threshold);
      float delta = frameDelta();
      float ease = 1.0F - (float)Math.exp(-delta * MOVE_RATE);

      // Pulse phase for glow animation
      long now = System.nanoTime();
      float pulse = 0.5F + 0.5F * (float)Math.sin(now * 0.001F * PULSE_SPEED);

      // ---- Zones (merged areas) ----
      Iterator<Entry<Long, ZoneFade>> zi = zoneFades.entrySet().iterator();
      while (zi.hasNext()) {
         Entry entry = zi.next();
         ZoneFade zf = (ZoneFade)entry.getValue();
         zf.alpha += (zf.flagged ? delta / FADE_IN_SECONDS : -delta / FADE_OUT_SECONDS);
         zf.alpha = Math.clamp(zf.alpha, 0.0F, 1.0F);
         if (!zf.flagged && zf.alpha <= 0.0F) {
            zi.remove();
            continue;
         }

         // Animate position/size
         zf.cx += (zf.targetX - zf.cx) * ease;
         zf.cz += (zf.targetZ - zf.cz) * ease;
         zf.size += (zf.targetSize - zf.size) * ease;

         double half = zf.size / 2.0;
         double x0 = zf.cx - half, z0 = zf.cz - half;
         double x1 = zf.cx + half, z1 = zf.cz + half;

         // Pulsing glow halo
         float glowSpread = 6.0F + pulse * 4.0F;
         int glowColor = Colors.withAlpha(accent, fillOp * 0.12F * zf.alpha);
         FlatOverlay.fillQuad(buffer, pose, cam, x0 - glowSpread, z0 - glowSpread,
            x1 + glowSpread, z1 + glowSpread, renderY, glowColor);

         // Gradient fill (center brighter)
         int centerColor = Colors.withAlpha(accent, fillOp * zf.alpha * (0.5F + pulse * 0.2F));
         int edgeColor = Colors.withAlpha(accent, fillOp * zf.alpha * 0.35F);
         drawGradientQuad(buffer, pose, cam, x0, z0, x1, z1, renderY, centerColor, edgeColor);

         // Outline
         if (outline && outlineOp > 0.004F) {
            int lineColor = Colors.withAlpha(accentBright, outlineOp * zf.alpha * (0.7F + pulse * 0.3F));
            drawOutline(buffer, pose, cam, x0, z0, x1, z1, renderY, lineColor, 2.5F);
         }

         // 3D box prism for confirmed zones
         if (zf.tier > 0.7F && zf.alpha > 0.5F) {
            double height = 12.0F * zf.alpha;
            drawBox3D(buffer, pose, cam, x0, z0, x1, z1, renderY, renderY + height,
               Colors.withAlpha(accent, fillOp * zf.alpha * 0.15F),
               Colors.withAlpha(accentBright, outlineOp * zf.alpha * 0.5F));
         }

         // Centroid marker
         if (mod.centroidMarker.get() && zf.alpha > 0.3F) {
            float markerPulse = 0.5F + 0.5F * (float)Math.sin(now * 0.004F + zf.cx);
            int markerColor = Colors.withAlpha(accentBright, 0.95F * zf.alpha * markerPulse);
            int markerGlow = Colors.withAlpha(accentBright, 0.2F * zf.alpha * markerPulse);
            FlatOverlay.fillQuad(buffer, pose, cam,
               zf.cx - 1.0, zf.cz - 1.0, zf.cx + 1.0, zf.cz + 1.0, renderY + 0.1, markerColor);
            FlatOverlay.fillQuad(buffer, pose, cam,
               zf.cx - 3.0, zf.cz - 3.0, zf.cx + 3.0, zf.cz + 3.0, renderY + 0.05, markerGlow);
         }
      }

      // ---- Individual chunk flags ----
      Iterator<Entry<Long, ChunkFade>> ci = chunkFades.entrySet().iterator();
      while (ci.hasNext()) {
         Entry entry = ci.next();
         ChunkFade cf = (ChunkFade)entry.getValue();
         cf.alpha += (cf.flagged ? delta / FADE_IN_SECONDS : -delta / FADE_OUT_SECONDS);
         cf.alpha = Math.clamp(cf.alpha, 0.0F, 1.0F);
         if (!cf.flagged && cf.alpha <= 0.0F) {
            ci.remove();
            continue;
         }

         double cx = (double)ChunkPos.getX((Long)entry.getKey()) * 16.0 + 8.0;
         double cz = (double)ChunkPos.getZ((Long)entry.getKey()) * 16.0 + 8.0;
         double half = 8.0;

         // Pulsing glow
         float glowSpread = 4.0F + pulse * 3.0F;
         FlatOverlay.fillQuad(buffer, pose, cam,
            cx - half - glowSpread, cz - half - glowSpread,
            cx + half + glowSpread, cz + half + glowSpread, renderY,
            Colors.withAlpha(accent, fillOp * 0.15F * cf.alpha));

         // Fill
         int fillColor = Colors.withAlpha(accent, fillOp * cf.alpha * cf.tier * (0.6F + pulse * 0.2F));
         FlatOverlay.fillQuad(buffer, pose, cam, cx - half, cz - half, cx + half, cz + half, renderY, fillColor);

         // Outline
         if (outline && outlineOp > 0.004F) {
            int lineColor = Colors.withAlpha(accentBright, outlineOp * cf.alpha * (0.7F + pulse * 0.3F));
            drawOutline(buffer, pose, cam, cx - half, cz - half, cx + half, cz + half, renderY, lineColor, 2.0F);
         }

         // 3D box for high-confidence single chunks
         if (cf.tier > 0.8F && cf.alpha > 0.5F) {
            double height = 10.0F * cf.alpha;
            drawBox3D(buffer, pose, cam, cx - half, cz - half, cx + half, cz + half, renderY, renderY + height,
               Colors.withAlpha(accent, fillOp * cf.alpha * 0.12F),
               Colors.withAlpha(accentBright, outlineOp * cf.alpha * 0.4F));
         }
      }

      FlatOverlay.flush(buffer);
   }

   /** Draws a quad with a radial gradient (center brighter, edges darker). */
   private static void drawGradientQuad(BufferSource buffer, PoseStack pose, Vec3 cam,
                                        double x0, double z0, double x1, double z1, double y,
                                        int centerColor, int edgeColor) {
      // Decompose into 4 sub-quads with interpolated colors
      double mx = (x0 + x1) * 0.5;
      double mz = (z0 + z1) * 0.5;
      FlatOverlay.fillQuad(buffer, pose, cam, x0, z0, mx, mz, y, edgeColor);
      FlatOverlay.fillQuad(buffer, pose, cam, mx, z0, x1, mz, y, edgeColor);
      FlatOverlay.fillQuad(buffer, pose, cam, x0, mz, mx, z1, y, edgeColor);
      FlatOverlay.fillQuad(buffer, pose, cam, mx, mz, x1, z1, y, edgeColor);
      // Center bright spot
      double cr = Math.min(x1 - x0, z1 - z0) * 0.2;
      FlatOverlay.fillQuad(buffer, pose, cam, mx - cr, mz - cr, mx + cr, mz + cr, y, centerColor);
   }

   /** Draws a rectangular outline with 4 edges. */
   private static void drawOutline(BufferSource buffer, PoseStack pose, Vec3 cam,
                                   double x0, double z0, double x1, double z1, double y, int color, float width) {
      FlatOverlay.edge(buffer, pose, cam, x0, z0, x1, z0, y, color, width);
      FlatOverlay.edge(buffer, pose, cam, x1, z0, x1, z1, y, color, width);
      FlatOverlay.edge(buffer, pose, cam, x1, z1, x0, z1, y, color, width);
      FlatOverlay.edge(buffer, pose, cam, x0, z1, x0, z0, y, color, width);
   }

   /** Draws a 3D box prism: 4 vertical edges + 4 top edges + translucent sides. */
   private static void drawBox3D(BufferSource buffer, PoseStack pose, Vec3 cam,
                                 double x0, double z0, double x1, double z1, double y0, double y1,
                                 int sideColor, int edgeColor) {
      VertexConsumer vbuf = buffer.getBuffer(FlatOverlay.LINES);
      PoseStack.Pose pose2 = pose.last();

      // Vertical edges
      float[][] corners = {
         {(float)(x0 - cam.x), (float)(z0 - cam.z)},
         {(float)(x1 - cam.x), (float)(z0 - cam.z)},
         {(float)(x1 - cam.x), (float)(z1 - cam.z)},
         {(float)(x0 - cam.x), (float)(z1 - cam.z)}
      };
      float fy0 = (float)(y0 - cam.y);
      float fy1 = (float)(y1 - cam.y);

      for (float[] corner : corners) {
         vbuf.addVertex(pose2, corner[0], fy0, corner[1]).setColor(edgeColor).setLineWidth(2.0F);
         vbuf.addVertex(pose2, corner[0], fy1, corner[1]).setColor(edgeColor).setLineWidth(2.0F);
      }

      // Top edges (slightly brighter)
      int topEdge = Colors.withAlpha(edgeColor, 1.0F);
      vbuf.addVertex(pose2, corners[0][0], fy1, corners[0][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[1][0], fy1, corners[1][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[1][0], fy1, corners[1][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[2][0], fy1, corners[2][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[2][0], fy1, corners[2][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[3][0], fy1, corners[3][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[3][0], fy1, corners[3][1]).setColor(topEdge).setLineWidth(2.0F);
      vbuf.addVertex(pose2, corners[0][0], fy1, corners[0][1]).setColor(topEdge).setLineWidth(2.0F);

      // Translucent side faces (fill)
      FlatOverlay.fillQuad(buffer, pose, cam, x0, z0, x1, z0, y0, sideColor);
      FlatOverlay.fillQuad(buffer, pose, cam, x1, z0, x1, z1, y0, sideColor);
      FlatOverlay.fillQuad(buffer, pose, cam, x1, z1, x0, z1, y0, sideColor);
      FlatOverlay.fillQuad(buffer, pose, cam, x0, z1, x0, z0, y0, sideColor);
   }

   private static void updateTargets(SusChunkScanner scanner, boolean smart, int threshold) {
      for (ChunkFade cf : chunkFades.values()) cf.flagged = false;
      for (ZoneFade zf : zoneFades.values()) zf.flagged = false;

      if (smart) {
         for (SusChunkScanner.Zone zone : scanner.zones()) {
            if (zone.members().size() == 1) {
               long key = zone.members().iterator().next();
               ChunkFade cf = chunkFades.computeIfAbsent(key, k -> new ChunkFade());
               cf.flagged = true;
               cf.tier = confidence(zone.maxScore(), threshold);
            } else {
               long minKey = Long.MAX_VALUE;
               for (long k : zone.members()) minKey = Math.min(minKey, k);
               ZoneFade zf = zoneFades.get(minKey);
               if (zf == null) {
                  zf = new ZoneFade(zone.centroidX(), zone.centroidZ());
                  zoneFades.put(minKey, zf);
               }
               zf.flagged = true;
               zf.targetX = zone.centroidX();
               zf.targetZ = zone.centroidZ();
               zf.targetSize = Math.min(48.0F, 16.0F + (float)(zone.members().size() - 1) * 8.0F);
               zf.tier = confidence(zone.maxScore(), threshold);
            }
         }
      } else {
         for (SusChunkScanner.Flag flag : scanner.flags()) {
            ChunkFade cf = chunkFades.computeIfAbsent(flag.chunkKey(), k -> new ChunkFade());
            cf.flagged = true;
            cf.tier = 1.0F;
         }
      }
   }

   private static float confidence(double score, int threshold) {
      if (threshold <= 0) return 1.0F;
      float f = (float)((score - threshold) / (threshold * 2.0));
      return 0.55F + 0.45F * Math.clamp(f, 0.0F, 1.0F);
   }

   private static float frameDelta() {
      long now = System.nanoTime();
      float d = lastFrameNanos == 0L ? 0.016F : (float)(now - lastFrameNanos) / 1.0E9F;
      lastFrameNanos = now;
      return Math.min(d, 0.1F);
   }

   private static final class ChunkFade {
      float alpha;
      float tier = 1.0F;
      boolean flagged;
   }

   private static final class ZoneFade {
      double cx, cz;
      double targetX, targetZ;
      float size = 16.0F;
      float targetSize = 16.0F;
      float alpha;
      float tier = 1.0F;
      boolean flagged;

      ZoneFade(double x, double z) {
         this.cx = this.targetX = x;
         this.cz = this.targetZ = z;
      }
   }
}
