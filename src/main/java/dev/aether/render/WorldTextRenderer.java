package dev.aether.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aether.module.impl.SkyLyricsModule;
import dev.aether.module.impl.SkyLyricsModule.Line;
import dev.aether.module.impl.SkyLyricsModule.Snapshot;
import dev.aether.util.Colors;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Renders Sky Lyrics as real 3D glyph quads in world space.
 *
 * <p>Each glyph is rasterized once with AWT into a 512×512 atlas (using the client's bundled
 * Xuong-Regular font — the same face the HUD uses), uploaded as a dynamic texture, and drawn as
 * textured quads through the stock emissive-translucent render type. Because the quads live in
 * the world render pass ({@code LevelRendererMixin.renderBlockOutline}), blocks and geometry
 * between the camera and the lyrics occlude them naturally — no draw-order hacks.
 *
 * <p>Lines are arranged on a ring around the player at a configurable distance and height. The
 * ring rotates continuously, and each line is tilted slightly out of vertical. Words turn from
 * gray to the "sung" color exactly when their timestamp is reached.
 */
public final class WorldTextRenderer {
   private static final int ATLAS_SIZE = 512;
   private static final int CELLS = 16;
   private static final int CELL = ATLAS_SIZE / CELLS;
   private static final float BASE_FONT_SIZE = 28.0F;
   private static final float LINE_HEIGHT = 38.0F;
   private static final float LINE_GAP = 0.62F;
   private static final float MAX_LINE_CHARS = 140.0F;
   private static final int LIGHT = 15728880;

   private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("aetherclient", "textures/sky_lyrics/atlas");
   private static final RenderType SKY_LYRICS = RenderTypes.entityTranslucentEmissive(ATLAS_ID);

   private static final Map<Character, Glyph> GLYPHS = new HashMap<>();
   private static volatile boolean atlasReady;
   private static int nextCell;
   private static float ringAngle;
   private static long lastNano;

   private WorldTextRenderer() {
   }

   /** Renders the current lyric snapshot as depth-tested world-space glyph quads. */
   public static void render(BufferSource buffers, PoseStack poseStack, Vec3 camera, SkyLyricsModule module) {
      ensureAtlas();
      if (!atlasReady) {
         return;
      }
      Snapshot snapshot = module.snapshot();
      if (snapshot == null) {
         return;
      }
      List<Line> lines = snapshot.lines();
      if (lines.isEmpty()) {
         return;
      }
      int currentLine = Math.max(0, snapshot.currentLine());
      int visible = (int)Math.clamp(module.visibleLines.getInt(), 1.0, 5.0);
      int half = (visible - 1) / 2;
      int start = Math.max(0, currentLine - half);
      int end = Math.min(lines.size() - 1, currentLine + (visible - 1 - half));
      if (start > end) {
         return;
      }

      float scale = module.textScale.getFloat();
      float radius = module.distance.getFloat();
      float height = module.height.getFloat();
      float tilt = (float)Math.toRadians(module.maxRotation.getInt());
      float rotation = module.rotationSpeed.getFloat();
      boolean glow = module.glow.get();
      int gray = module.grayColor.get();
      int white = module.whiteColor.get();

      long now = System.nanoTime();
      if (lastNano != 0L) {
         float dt = (now - lastNano) / 1.0E9F;
         if (dt > 0.1F) {
            dt = 0.1F;
         }
         ringAngle += rotation * dt;
      }
      lastNano = now;

      VertexConsumer vc = buffers.getBuffer(SKY_LYRICS);
      PoseStack.Pose pose = poseStack.last();
      for (int i = start; i <= end; i++) {
         int offset = i - currentLine;
         float fade = lineFade(offset, visible);
         if (fade <= 0.02F) {
            continue;
         }
         Line line = lines.get(i);
         float halfW = textWidth(line.text(), scale) * 0.5F;
         if (halfW > MAX_LINE_CHARS) {
            halfW = MAX_LINE_CHARS;
         }
         float halfH = LINE_HEIGHT * scale * 0.5F;
         float a = ringAngle + offset * LINE_GAP;
         float ca = (float)Math.cos(a);
         float sa = (float)Math.sin(a);

         // World-space position of this line on the ring around the camera.
         float px = (float)camera.x + ca * radius;
         float py = (float)camera.y + height;
         float pz = (float)camera.z + sa * radius;
         // Local axes: right (tangent), up (tilted), front (facing camera).
         float rx = -sa, ry = 0.0F, rz = ca;
         float nx = -ca, ny = 0.0F, nz = -sa;
         float ct = (float)Math.cos(tilt);
         float st = (float)Math.sin(tilt);
         float ux = nx * st, uy = ct, uz = nz * st;
         float fx = nx * ct, fy = -st, fz = nz * ct;
         // Nudge slightly deeper so block surfaces win depth ties.
         float ox = px - fx * 0.02F;
         float oy = py - fy * 0.02F;
         float oz = pz - fz * 0.02F;

         boolean isCurrent = i == currentLine;
         int currentWord = isCurrent ? snapshot.currentWord() : -1;
         float cursor = -halfW;
         int wi = 0;
         for (String word : line.words()) {
            float wordW = textWidth(word, scale);
            int color;
            float alpha;
            if (isCurrent) {
               if (wi < currentWord) {
                  color = white;
                  alpha = 0.62F;
               } else if (wi == currentWord) {
                  color = white;
                  alpha = 1.0F;
               } else {
                  color = gray;
                  alpha = 0.5F;
               }
            } else {
               color = gray;
               alpha = 0.5F;
            }
            int argb = Colors.withAlpha(color, alpha * fade);
            if (glow) {
               renderWordWorld(vc, pose, word, cursor, -halfH, halfH, scale, ox, oy, oz, rx, ry, rz, ux, uy, uz, fx, fy, fz, Colors.withAlpha(color, 0.22F * fade), 1.08F);
            }
            renderWordWorld(vc, pose, word, cursor, -halfH, halfH, scale, ox, oy, oz, rx, ry, rz, ux, uy, uz, fx, fy, fz, argb, 1.0F);
            cursor += wordW + textWidth(" ", scale);
            wi++;
         }
      }
   }

   /** Emits one glyph quad per character, positions computed in world space via the line's basis. */
   private static void renderWordWorld(
      VertexConsumer vc,
      PoseStack.Pose pose,
      String word,
      float startX,
      float y0,
      float y1,
      float scale,
      float ox, float oy, float oz,
      float rx, float ry, float rz,
      float ux, float uy, float uz,
      float fx, float fy, float fz,
      int argb,
      float sizeScale
   ) {
      float cursor = startX;
      for (int i = 0; i < word.length(); i++) {
         char c = word.charAt(i);
         if (c == ' ') {
            cursor += glyphFor(c).advance() * scale;
            continue;
         }
         Glyph g = glyphFor(c);
         float w = g.advance() * scale * sizeScale;
         float h0 = y0 * sizeScale;
         float h1 = y1 * sizeScale;
         float cx = cursor + w * 0.5F;
         cursor += g.advance() * scale;
         float x0 = cx - w * 0.5F;
         float x1 = cx + w * 0.5F;
         // Quad corners in local space (x right, y up, z front=0), transformed to world.
         float[][] local = {
            {x0, h0, 0.0F}, {x1, h0, 0.0F}, {x1, h1, 0.0F}, {x0, h1, 0.0F}
         };
         float[] uv = {g.u0(), g.v1(), g.u1(), g.v1(), g.u1(), g.v0(), g.u0(), g.v0()};
         for (int v = 0; v < 4; v++) {
            float lx = local[v][0], ly = local[v][1], lz = local[v][2];
            float wx = ox + lx * rx + ly * ux + lz * fx;
            float wy = oy + lx * ry + ly * uy + lz * fy;
            float wz = oz + lx * rz + ly * uz + lz * fz;
            tex(vc, pose, wx, wy, wz, uv[v * 2], uv[v * 2 + 1], argb);
         }
      }
   }

   private static float lineFade(int offset, int visible) {
      float max = visible * 0.6F;
      float t = Math.abs(offset) / max;
      return (float)Math.clamp(1.0 - 0.22F * t, 0.35F, 1.0F);
   }

   /** Full vertex element set required by the emissive-translucent render type. */
   private static void tex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int argb) {
      vc.addVertex(pose, x, y, z)
         .setColor(argb)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(LIGHT)
         .setNormal(pose, 0.0F, 0.0F, 1.0F);
   }

   private static float textWidth(String s, float scale) {
      float w = 0.0F;
      for (int i = 0; i < s.length(); i++) {
         w += glyphFor(s.charAt(i)).advance();
      }
      return w * scale;
   }

   /** Builds the glyph atlas (once) and uploads it as a dynamic texture. */
   private static synchronized void ensureAtlas() {
      if (atlasReady) {
         return;
      }
      try {
         Minecraft mc = Minecraft.getInstance();
         if (mc == null) {
            return;
         }
         Font font = loadFont();
         BufferedImage image = rasterize(font);
         NativeImage nativeImage = new NativeImage(ATLAS_SIZE, ATLAS_SIZE, false);
         int[] rgb = image.getRGB(0, 0, ATLAS_SIZE, ATLAS_SIZE, null, 0, ATLAS_SIZE);
         for (int i = 0; i < rgb.length; i++) {
            nativeImage.setPixel(i % ATLAS_SIZE, i / ATLAS_SIZE, rgb[i]);
         }
         TextureManager texMgr = mc.getTextureManager();
         texMgr.register(ATLAS_ID, new DynamicTexture(() -> "aetherclient:sky_lyrics_atlas", nativeImage));
         atlasReady = true;
      } catch (Throwable t) {
         // Font/AWT failure — leave the atlas unbuilt; render() simply returns.
      }
   }

   private static Font loadFont() throws Exception {
      try (InputStream in = WorldTextRenderer.class.getResourceAsStream("/assets/aetherclient/fonts/Xuong-Regular.ttf")) {
         if (in == null) {
            throw new IllegalStateException("Bundled Xuong-Regular.ttf not found on classpath");
         }
         Font font = Font.createFont(Font.TRUETYPE_FONT, in);
         return font.deriveFont(Font.PLAIN, BASE_FONT_SIZE);
      }
   }

   /** Rasterizes the printable ASCII + Latin-1 supplement ranges into the atlas. */
   private static BufferedImage rasterize(Font font) {
      BufferedImage image = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
         g.setColor(Color.WHITE);
         FontMetrics fm = g.getFontMetrics(font);
         for (int c = 0x20; c <= 0x7E; c++) {
            addGlyph(g, fm, font, (char)c);
         }
         for (int c = 0xA0; c <= 0xFF; c++) {
            addGlyph(g, fm, font, (char)c);
         }
      } finally {
         g.dispose();
      }
      return image;
   }

   private static void addGlyph(Graphics2D g, FontMetrics fm, Font font, char c) {
      if (GLYPHS.containsKey(c) || nextCell >= CELLS * CELLS) {
         return;
      }
      int col = nextCell % CELLS;
      int row = nextCell / CELLS;
      nextCell++;

      String str = String.valueOf(c);
      Rectangle2D bounds = fm.getStringBounds(str, g);
      float pad = 3.0F;
      AffineTransform at = new AffineTransform();
      at.translate(
         col * CELL + pad - (float)bounds.getX(),
         row * CELL + pad - (float)bounds.getY()
      );
      g.drawString(str, at);

      float advance = (float)Math.ceil(fm.stringWidth(str));
      float u0 = col / (float)CELLS;
      float u1 = (col + 1) / (float)CELLS;
      float v0 = row / (float)CELLS;
      float v1 = (row + 1) / (float)CELLS;
      GLYPHS.put(c, new Glyph(u0, v0, u1, v1, advance));
   }

   private static Glyph glyphFor(char c) {
      Glyph g = GLYPHS.get(c);
      if (g != null) {
         return g;
      }
      // Character outside the rasterized ranges (rare) — fall back to a visible stand-in.
      Glyph q = GLYPHS.get('?');
      return q != null ? q : GLYPHS.get(' ');
   }

   private record Glyph(float u0, float v0, float u1, float v1, float advance) {
   }
}
