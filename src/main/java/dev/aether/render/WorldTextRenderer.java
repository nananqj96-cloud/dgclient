package dev.aether.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.aether.module.impl.SkyLyricsModule;
import dev.aether.module.impl.SkyLyricsModule.Line;
import dev.aether.module.impl.SkyLyricsModule.Snapshot;
import dev.aether.util.Colors;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontRenderContext;
import java.awt.Graphics2D;
import java.awt.GlyphVector;
import java.awt.RenderingHints;
import java.awt.Shape;
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
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
      float rotation = (float)module.rotationSpeed.get();
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

         Vector3f pos = new Vector3f(
            camera.x + ca * radius,
            camera.y + height,
            camera.z + sa * radius
         );
         Vector3f right = new Vector3f(-sa, 0.0F, ca);
         Vector3f norm = new Vector3f(-ca, 0.0F, -sa);
         float ct = (float)Math.cos(tilt);
         float st = (float)Math.sin(tilt);
         Vector3f up = new Vector3f(norm.x * st, ct, norm.z * st);
         Vector3f front = new Vector3f(norm.x * ct, -st, norm.z * ct);

         Quaternionf q = new Quaternionf().setFromNormalizedBasis(right, up, front);
         poseStack.push();
         // Nudge slightly deeper so block surfaces win depth ties (blocks always render on top).
         poseStack.translate(
            pos.x - front.x * 0.02F,
            pos.y - front.y * 0.02F,
            pos.z - front.z * 0.02F
         );
         poseStack.mulPose(q);
         PoseStack.Pose pose = poseStack.last();

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
               renderWord(vc, pose, word, cursor, -halfH, halfH, scale, Colors.withAlpha(color, 0.22F * fade), 1.08F);
            }
            renderWord(vc, pose, word, cursor, -halfH, halfH, scale, argb, 1.0F);
            cursor += wordW + textWidth(" ", scale);
            wi++;
         }
         poseStack.pop();
      }
   }

   private static float lineFade(int offset, int visible) {
      float max = visible * 0.6F;
      float t = Math.abs(offset) / max;
      return (float)Math.clamp(1.0 - 0.22F * t, 0.35F, 1.0F);
   }

   /** Emits one glyph quad per character of the word, laid out along the local X axis. */
   private static void renderWord(
      VertexConsumer vc,
      PoseStack.Pose pose,
      String word,
      float startX,
      float y0,
      float y1,
      float scale,
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
         tex(vc, pose, x0, h0, 0.0F, g.u0(), g.v1(), argb);
         tex(vc, pose, x1, h0, 0.0F, g.u1(), g.v1(), argb);
         tex(vc, pose, x1, h1, 0.0F, g.u1(), g.v0(), argb);
         tex(vc, pose, x0, h1, 0.0F, g.u0(), g.v0(), argb);
      }
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
            nativeImage.setColorArgb(i % ATLAS_SIZE, i / ATLAS_SIZE, rgb[i]);
         }
         TextureManager texMgr = mc.getTextureManager();
         texMgr.registerTexture(ATLAS_ID, new AtlasTexture(nativeImage));
         atlasReady = true;
      } catch (Throwable t) {
         // Font/AWT failure — leave the atlas unbuilt; render() simply returns.
      }
   }

   /** Dynamic texture that uploads a rasterized glyph atlas to the GPU. */
   private static final class AtlasTexture extends AbstractTexture {
      private final NativeImage image;

      AtlasTexture(NativeImage image) {
         this.image = image;
      }

      @Override
      public void upload() {
         GpuTexture gpu = getTexture();
         if (gpu != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, image);
         }
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
         FontRenderContext frc = g.getFontRenderContext();
         for (int c = 0x20; c <= 0x7E; c++) {
            addGlyph(g, frc, font, (char)c);
         }
         for (int c = 0xA0; c <= 0xFF; c++) {
            addGlyph(g, frc, font, (char)c);
         }
      } finally {
         g.dispose();
      }
      return image;
   }

   private static void addGlyph(Graphics2D g, FontRenderContext frc, Font font, char c) {
      if (GLYPHS.containsKey(c) || nextCell >= CELLS * CELLS) {
         return;
      }
      int col = nextCell % CELLS;
      int row = nextCell / CELLS;
      nextCell++;

      GlyphVector gv = frc.getGlyphVector(font, String.valueOf(c));
      Shape outline = gv.getOutline();
      Rectangle2D bounds = gv.getBounds2D();
      float pad = 3.0F;
      AffineTransform at = new AffineTransform();
      at.translate(
         col * CELL + pad - (float)bounds.getX(),
         row * CELL + pad - (float)bounds.getY()
      );
      g.fill(at.createTransformedShape(outline));

      float advance = (float)Math.ceil(font.getStringWidth(String.valueOf(c)));
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
