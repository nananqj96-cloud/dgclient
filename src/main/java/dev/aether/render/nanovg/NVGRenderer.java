package dev.aether.render.nanovg;

import dev.aether.AetherClient;
import dev.aether.util.Colors;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryUtil;

/**
 * High-throughput, zero-stack-churn NanoVG renderer for Aether Client.
 * Uses persistent off-heap NVGColor/NVGPaint structs, integer font IDs (nvgFontFaceId),
 * bounded O(1) textWidth caching, and zero-blur glow primitives for stutter-free FPS.
 */
public final class NVGRenderer {
   public static final String FONT_XUONG = "xuong";
   public static final String FONT_VANILLA = "vanilla";
   private static final int MAX_WIDTH_CACHE = 2048;
   private static final float[] STAR_COS = new float[10];
   private static final float[] STAR_SIN = new float[10];

   static {
      for (int i = 0; i < 10; i++) {
         double angle = (-Math.PI / 2.0) + i * (Math.PI / 5.0);
         STAR_COS[i] = (float)Math.cos(angle);
         STAR_SIN[i] = (float)Math.sin(angle);
      }
   }

   private static NVGRenderer instance;
   private final long ctx;
   private final int xuongFontId;
   private final int vanillaFontId;
   private final List<ByteBuffer> retainedFontData = new ArrayList<>();
   private String activeFont = FONT_XUONG;
   private int activeFontId = -1;
   private final ArrayDeque<Float> alphaStack = new ArrayDeque<>();
   private float appliedAlpha = 1.0F;

   // Persistent off-heap structs (render-thread only -> zero MemoryStack push/pop per draw call)
   private final NVGColor colorA = NVGColor.calloc();
   private final NVGColor colorB = NVGColor.calloc();
   private final NVGPaint paintBuf = NVGPaint.calloc();
   private final Map<WidthKey, Float> widthCache = new HashMap<>(512);

   private record WidthKey(int fontId, int sizeBits, String text) {
   }

   private NVGRenderer() {
      this.ctx = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
      if (this.ctx == 0L) {
         throw new IllegalStateException("Failed to create NanoVG context");
      }
      this.xuongFontId = this.loadFont(FONT_XUONG, "assets/aetherclient/fonts/Xuong-Regular.ttf");
      this.vanillaFontId = this.loadFont(FONT_VANILLA, "assets/aetherclient/fonts/Monocraft.ttf");
      if (this.xuongFontId != -1 && this.vanillaFontId != -1) {
         NanoVG.nvgAddFallbackFontId(this.ctx, this.xuongFontId, this.vanillaFontId);
      }
      this.setFontMode("Xuong");
   }

   public static NVGRenderer get() {
      if (instance == null) {
         instance = new NVGRenderer();
      }
      return instance;
   }

   public long ctx() {
      return this.ctx;
   }

   private int loadFont(String name, String resourcePath) {
      try (InputStream in = NVGRenderer.class.getClassLoader().getResourceAsStream(resourcePath)) {
         if (in == null) {
            AetherClient.LOGGER.warn("Font resource missing: {}", resourcePath);
            return -1;
         }
         byte[] bytes = in.readAllBytes();
         ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
         buffer.put(bytes).flip();
         int handle = NanoVG.nvgCreateFontMem(this.ctx, name, buffer, false);
         if (handle == -1) {
            MemoryUtil.memFree(buffer);
            AetherClient.LOGGER.warn("NanoVG rejected font {}", resourcePath);
            return -1;
         }
         this.retainedFontData.add(buffer);
         return handle;
      } catch (IOException e) {
         AetherClient.LOGGER.error("Failed to load font {}", resourcePath, e);
         return -1;
      }
   }

   public void setFontMode(String mode) {
      if ("Xuong".equals(mode) && this.xuongFontId != -1) {
         this.activeFont = FONT_XUONG;
         this.activeFontId = this.xuongFontId;
      } else if (this.vanillaFontId != -1) {
         this.activeFont = FONT_VANILLA;
         this.activeFontId = this.vanillaFontId;
      } else if (this.xuongFontId != -1) {
         this.activeFont = FONT_XUONG;
         this.activeFontId = this.xuongFontId;
      } else {
         this.activeFont = null;
         this.activeFontId = -1;
      }
   }

   public boolean hasFont() {
      return this.activeFontId != -1;
   }

   private int resolveFontId(String font) {
      if (font == null) {
         return -1;
      }
      if (FONT_XUONG.equals(font)) {
         return this.xuongFontId != -1 ? this.xuongFontId : this.vanillaFontId;
      }
      if (FONT_VANILLA.equals(font)) {
         return this.vanillaFontId != -1 ? this.vanillaFontId : this.xuongFontId;
      }
      return this.activeFontId;
   }

   public void beginFrame(float width, float height, float pixelRatio) {
      this.alphaStack.clear();
      this.appliedAlpha = 1.0F;
      NanoVG.nvgBeginFrame(this.ctx, width, height, pixelRatio);
   }

   public void endFrame() {
      NanoVG.nvgEndFrame(this.ctx);
   }

   public void save() {
      this.alphaStack.push(this.appliedAlpha);
      NanoVG.nvgSave(this.ctx);
   }

   public void restore() {
      if (!this.alphaStack.isEmpty()) {
         this.appliedAlpha = this.alphaStack.pop();
      }
      NanoVG.nvgRestore(this.ctx);
   }

   public void scale(float s) {
      NanoVG.nvgScale(this.ctx, s, s);
   }

   public void translate(float x, float y) {
      NanoVG.nvgTranslate(this.ctx, x, y);
   }

   public void rotate(float radians) {
      NanoVG.nvgRotate(this.ctx, radians);
   }

   public void alpha(float a) {
      this.appliedAlpha = this.appliedAlpha * Math.clamp(a, 0.0F, 1.0F);
      NanoVG.nvgGlobalAlpha(this.ctx, this.appliedAlpha);
   }

   public void scissor(float x, float y, float w, float h) {
      NanoVG.nvgIntersectScissor(this.ctx, x, y, Math.max(0.0F, w), Math.max(0.0F, h));
   }

   public void rect(float x, float y, float w, float h, float radius, int argb) {
      if (w <= 0.0F || h <= 0.0F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      if (radius <= 0.05F) {
         NanoVG.nvgRect(this.ctx, x, y, w, h);
      } else {
         NanoVG.nvgRoundedRect(this.ctx, x, y, w, h, Math.min(radius, Math.min(w, h) * 0.5F));
      }
      NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgFill(this.ctx);
   }

   public void rectVarying(float x, float y, float w, float h, float rtl, float rtr, float rbr, float rbl, int argb) {
      if (w <= 0.0F || h <= 0.0F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgRoundedRectVarying(this.ctx, x, y, w, h, rtl, rtr, rbr, rbl);
      NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgFill(this.ctx);
   }

   public void rectGradient(float x, float y, float w, float h, float radius, int from, int to, boolean vertical) {
      if (w <= 0.0F || h <= 0.0F) {
         return;
      }
      if (from == to) {
         this.rect(x, y, w, h, radius, from);
         return;
      }
      float ex = vertical ? x : x + w;
      float ey = vertical ? y + h : y;
      NanoVG.nvgLinearGradient(this.ctx, x, y, ex, ey, fillCol(this.colorA, from), fillCol(this.colorB, to), this.paintBuf);
      NanoVG.nvgBeginPath(this.ctx);
      if (radius <= 0.05F) {
         NanoVG.nvgRect(this.ctx, x, y, w, h);
      } else {
         NanoVG.nvgRoundedRect(this.ctx, x, y, w, h, Math.min(radius, Math.min(w, h) * 0.5F));
      }
      NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
      NanoVG.nvgFill(this.ctx);
   }

   public void rectVaryingGradient(float x, float y, float w, float h, float rtl, float rtr, float rbr, float rbl, int from, int to) {
      if (w <= 0.0F || h <= 0.0F) {
         return;
      }
      if (from == to) {
         this.rectVarying(x, y, w, h, rtl, rtr, rbr, rbl, from);
         return;
      }
      NanoVG.nvgLinearGradient(this.ctx, x, y, x, y + h, fillCol(this.colorA, from), fillCol(this.colorB, to), this.paintBuf);
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgRoundedRectVarying(this.ctx, x, y, w, h, rtl, rtr, rbr, rbl);
      NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
      NanoVG.nvgFill(this.ctx);
   }

   public void rectOutline(float x, float y, float w, float h, float radius, float stroke, int argb) {
      if (w <= stroke || h <= stroke || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      float rw = w - stroke;
      float rh = h - stroke;
      NanoVG.nvgRoundedRect(this.ctx, x + stroke * 0.5F, y + stroke * 0.5F, rw, rh, Math.min(radius, Math.min(rw, rh) * 0.5F));
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, stroke);
      NanoVG.nvgStroke(this.ctx);
   }

   public void glow(float x, float y, float w, float h, float radius, float spread, int argb) {
      if (spread <= 0.1F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBoxGradient(
         this.ctx,
         x,
         y,
         w,
         h,
         radius,
         spread * 2.0F,
         fillCol(this.colorA, argb),
         fillCol(this.colorB, Colors.withAlpha(argb, 0)),
         this.paintBuf
      );
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgRoundedRect(this.ctx, x - spread, y - spread, w + spread * 2.0F, h + spread * 2.0F, radius + spread);
      NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
      NanoVG.nvgFill(this.ctx);
   }

   public void circle(float cx, float cy, float r, int argb) {
      if (r <= 0.0F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgCircle(this.ctx, cx, cy, r);
      NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgFill(this.ctx);
   }

   public void circleOutline(float cx, float cy, float r, float stroke, int argb) {
      if (r <= 0.0F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgCircle(this.ctx, cx, cy, r);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, stroke);
      NanoVG.nvgStroke(this.ctx);
   }

   public void circleGlow(float cx, float cy, float r, float spread, int argb) {
      if (spread <= 0.1F || Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgRadialGradient(
         this.ctx,
         cx,
         cy,
         r * 0.25F,
         r + spread,
         fillCol(this.colorA, argb),
         fillCol(this.colorB, Colors.withAlpha(argb, 0)),
         this.paintBuf
      );
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgCircle(this.ctx, cx, cy, r + spread);
      NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
      NanoVG.nvgFill(this.ctx);
   }

   public void line(float x1, float y1, float x2, float y2, float width, int argb) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, x1, y1);
      NanoVG.nvgLineTo(this.ctx, x2, y2);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, width);
      NanoVG.nvgLineCap(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgStroke(this.ctx);
   }

   public void chevron(float cx, float cy, float size, float stroke, int argb, boolean pointDown) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      if (pointDown) {
         NanoVG.nvgMoveTo(this.ctx, cx - size, cy - size * 0.5F);
         NanoVG.nvgLineTo(this.ctx, cx, cy + size * 0.5F);
         NanoVG.nvgLineTo(this.ctx, cx + size, cy - size * 0.5F);
      } else {
         NanoVG.nvgMoveTo(this.ctx, cx - size * 0.5F, cy - size);
         NanoVG.nvgLineTo(this.ctx, cx + size * 0.5F, cy);
         NanoVG.nvgLineTo(this.ctx, cx - size * 0.5F, cy + size);
      }
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, stroke);
      NanoVG.nvgLineCap(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgLineJoin(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgStroke(this.ctx);
   }

   public void triangle(float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, x0, y0);
      NanoVG.nvgLineTo(this.ctx, x1, y1);
      NanoVG.nvgLineTo(this.ctx, x2, y2);
      NanoVG.nvgClosePath(this.ctx);
      NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgFill(this.ctx);
   }

   public void checkmark(float x, float y, float size, float stroke, int argb) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, x + size * 0.22F, y + size * 0.55F);
      NanoVG.nvgLineTo(this.ctx, x + size * 0.42F, y + size * 0.74F);
      NanoVG.nvgLineTo(this.ctx, x + size * 0.78F, y + size * 0.30F);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, stroke);
      NanoVG.nvgLineCap(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgLineJoin(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgStroke(this.ctx);
   }

   public void cross(float x, float y, float size, float stroke, int argb) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      float pad = size * 0.3F;
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, x + pad, y + pad);
      NanoVG.nvgLineTo(this.ctx, x + size - pad, y + size - pad);
      NanoVG.nvgMoveTo(this.ctx, x + size - pad, y + pad);
      NanoVG.nvgLineTo(this.ctx, x + pad, y + size - pad);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
      NanoVG.nvgStrokeWidth(this.ctx, stroke);
      NanoVG.nvgLineCap(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgStroke(this.ctx);
   }

   /**
    * Single-path 5-point star (1 draw call instead of 10 separate line strokes).
    */
   public void star(float cx, float cy, float outerR, float innerR, int argb, boolean filled) {
      if (Colors.alpha(argb) == 0) {
         return;
      }
      NanoVG.nvgBeginPath(this.ctx);
      for (int i = 0; i < 10; i++) {
         float r = (i & 1) == 0 ? outerR : innerR;
         float px = cx + STAR_COS[i] * r;
         float py = cy + STAR_SIN[i] * r;
         if (i == 0) {
            NanoVG.nvgMoveTo(this.ctx, px, py);
         } else {
            NanoVG.nvgLineTo(this.ctx, px, py);
         }
      }
      NanoVG.nvgClosePath(this.ctx);
      if (filled) {
         NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
         NanoVG.nvgFill(this.ctx);
      } else {
         NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, argb));
         NanoVG.nvgStrokeWidth(this.ctx, 1.2F);
         NanoVG.nvgLineJoin(this.ctx, NanoVG.NVG_ROUND);
         NanoVG.nvgStroke(this.ctx);
      }
   }

   /**
    * Draws the signature geometric Aether prism/crest mark centered at (cx, cy).
    */
   public void aetherMark(float cx, float cy, float radius, int accent, int bright) {
      // Outer diamond halo
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, cx, cy - radius);
      NanoVG.nvgLineTo(this.ctx, cx + radius, cy);
      NanoVG.nvgLineTo(this.ctx, cx, cy + radius);
      NanoVG.nvgLineTo(this.ctx, cx - radius, cy);
      NanoVG.nvgClosePath(this.ctx);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, Colors.withAlpha(accent, 0.55F)));
      NanoVG.nvgStrokeWidth(this.ctx, 1.2F);
      NanoVG.nvgStroke(this.ctx);

      // Crisp 'A' chevron apex + crossbar
      float h = radius * 0.62F;
      float w = radius * 0.52F;
      NanoVG.nvgBeginPath(this.ctx);
      NanoVG.nvgMoveTo(this.ctx, cx - w, cy + h * 0.85F);
      NanoVG.nvgLineTo(this.ctx, cx, cy - h);
      NanoVG.nvgLineTo(this.ctx, cx + w, cy + h * 0.85F);
      NanoVG.nvgMoveTo(this.ctx, cx - w * 0.56F, cy + h * 0.25F);
      NanoVG.nvgLineTo(this.ctx, cx, cy + h * 0.02F);
      NanoVG.nvgLineTo(this.ctx, cx + w * 0.56F, cy + h * 0.25F);
      NanoVG.nvgStrokeColor(this.ctx, fillCol(this.colorA, bright));
      NanoVG.nvgStrokeWidth(this.ctx, Math.max(1.5F, radius * 0.19F));
      NanoVG.nvgLineCap(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgLineJoin(this.ctx, NanoVG.NVG_ROUND);
      NanoVG.nvgStroke(this.ctx);
   }

   public float text(String str, float x, float y, float size, int argb) {
      return this.textWithFontId(str, x, y, size, argb, this.activeFontId);
   }

   public float text(String str, float x, float y, float size, int argb, String font) {
      return this.textWithFontId(str, x, y, size, argb, this.resolveFontId(font));
   }

   private float textWithFontId(String str, float x, float y, float size, int argb, int fontId) {
      if (fontId == -1 || str == null || str.isEmpty()) {
         return 0.0F;
      }
      NanoVG.nvgFontFaceId(this.ctx, fontId);
      NanoVG.nvgFontSize(this.ctx, size);
      NanoVG.nvgTextAlign(this.ctx, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
      NanoVG.nvgFillColor(this.ctx, fillCol(this.colorA, argb));
      return NanoVG.nvgText(this.ctx, x, y, str) - x;
   }

   public float textGradient(String str, float x, float y, float size, int top, int bottom) {
      if (this.activeFontId == -1 || str == null || str.isEmpty()) {
         return 0.0F;
      }
      if (top == bottom) {
         return this.text(str, x, y, size, top);
      }
      NanoVG.nvgLinearGradient(this.ctx, x, y - size * 0.5F, x, y + size * 0.5F, fillCol(this.colorA, top), fillCol(this.colorB, bottom), this.paintBuf);
      NanoVG.nvgFontFaceId(this.ctx, this.activeFontId);
      NanoVG.nvgFontSize(this.ctx, size);
      NanoVG.nvgTextAlign(this.ctx, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
      NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
      return NanoVG.nvgText(this.ctx, x, y, str) - x;
   }

   /**
    * Zero-blur text glow: avoids nvgFontBlur CPU glyph-atlas re-rasterization stalls!
    */
   public void textGlow(String str, float x, float y, float size, int argb) {
      if (this.activeFontId != -1 && str != null && !str.isEmpty() && Colors.alpha(argb) > 0) {
         float w = this.textWidth(str, size);
         this.glow(x, y - size * 0.35F, Math.max(4.0F, w), size * 0.7F, size * 0.35F, 5.0F, Colors.withAlpha(argb, Colors.alpha(argb) / 255.0F * 0.45F));
      }
   }

   public float textTruncated(String str, float x, float y, float size, int argb, float maxWidth) {
      if (str == null || str.isEmpty() || maxWidth <= 0.0F) {
         return 0.0F;
      }
      if (this.textWidth(str, size) <= maxWidth) {
         return this.text(str, x, y, size, argb);
      }
      float ellipsisW = this.textWidth("…", size);
      if (ellipsisW >= maxWidth || str.length() <= 1) {
         return this.text("…", x, y, size, argb);
      }
      int lo = 1;
      int hi = str.length() - 1;
      int best = 1;
      while (lo <= hi) {
         int mid = (lo + hi) >>> 1;
         String candidate = str.substring(0, mid);
         if (this.textWidth(candidate, size) + ellipsisW <= maxWidth) {
            best = mid;
            lo = mid + 1;
         } else {
            hi = mid - 1;
         }
      }
      return this.text(str.substring(0, best) + "…", x, y, size, argb);
   }

   public float textWidth(String str, float size) {
      if (this.activeFontId == -1 || str == null || str.isEmpty()) {
         return 0.0F;
      }
      if (str.length() <= 64) {
         WidthKey key = new WidthKey(this.activeFontId, Float.floatToRawIntBits(size), str);
         Float cached = this.widthCache.get(key);
         if (cached != null) {
            return cached;
         }
         float computed = this.measureTextRaw(str, size);
         if (this.widthCache.size() >= MAX_WIDTH_CACHE) {
            this.widthCache.clear();
         }
         this.widthCache.put(key, computed);
         return computed;
      }
      return this.measureTextRaw(str, size);
   }

   private float measureTextRaw(String str, float size) {
      NanoVG.nvgFontFaceId(this.ctx, this.activeFontId);
      NanoVG.nvgFontSize(this.ctx, size);
      NanoVG.nvgTextAlign(this.ctx, NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_MIDDLE);
      return NanoVG.nvgTextBounds(this.ctx, 0.0F, 0.0F, str, (FloatBuffer)null);
   }

   public void imagePattern(int image, float patternX, float patternY, float patternW, float patternH, float x, float y, float w, float h, float alphaMul) {
      if (image > 0 && w > 0.0F && h > 0.0F) {
         NanoVG.nvgImagePattern(this.ctx, patternX, patternY, patternW, patternH, 0.0F, image, alphaMul, this.paintBuf);
         NanoVG.nvgBeginPath(this.ctx);
         NanoVG.nvgRect(this.ctx, x, y, w, h);
         NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
         NanoVG.nvgFill(this.ctx);
      }
   }

   public void image(int image, float x, float y, float w, float h, int tint) {
      if (image > 0 && w > 0.0F && h > 0.0F) {
         NanoVG.nvgImagePattern(this.ctx, x, y, w, h, 0.0F, image, 1.0F, this.paintBuf);
         this.paintBuf.innerColor(fillCol(this.colorA, tint));
         NanoVG.nvgBeginPath(this.ctx);
         NanoVG.nvgRect(this.ctx, x, y, w, h);
         NanoVG.nvgFillPaint(this.ctx, this.paintBuf);
         NanoVG.nvgFill(this.ctx);
      }
   }

   private static NVGColor fillCol(NVGColor out, int argb) {
      return NanoVG.nvgRGBA(
         (byte)Colors.red(argb),
         (byte)Colors.green(argb),
         (byte)Colors.blue(argb),
         (byte)Colors.alpha(argb),
         out
      );
   }
}
