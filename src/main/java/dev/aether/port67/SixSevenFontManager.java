package dev.aether.port67;

import dev.aether.render.nanovg.NVGRenderer;
import java.util.ArrayDeque;

/**
 * A {@link NVGRenderer}-backed adapter for the 67client {@code FontManager} drawing primitives,
 * ported from the 67port source ({@code sixseven.client.render.FontManager}).
 *
 * <p>The 67client's {@code FontManager} wraps its bundled NanoVG ({@code org.lwjgl.nanovg}); this
 * adapter re-implements the same drawing surface on top of Aether's {@link NVGRenderer} so the real
 * 67client GUI source runs unchanged in structure. Exotic primitives without a direct NVGRenderer
 * equivalent (polylines, arcs, the logo glyph) are approximated with the available primitives.
 *
 * <p>Frame setup ({@code beginFrame}/{@code endFrame}) is handled by Aether's overlay renderer, so
 * those methods are no-ops here.
 */
public final class SixSevenFontManager {
   private final NVGRenderer vg;
   private float amount2 = 1.0F;
   private final ArrayDeque<Float> alphaStack = new ArrayDeque<>();
   private final ArrayDeque<Boolean> enabledStack = new ArrayDeque<>();

   public SixSevenFontManager(NVGRenderer vg) {
      this.vg = vg;
   }

   public void render(float windowWidth, float windowHeight, float devicePixelRatio) {
      // Frame is driven by Aether's overlay renderer.
   }

   public void update() {
   }

   public void update2() {
      this.alphaStack.push(this.amount2);
      this.enabledStack.push(Boolean.FALSE);
      this.vg.save();
   }

   public void update3() {
      if (!this.enabledStack.isEmpty()) {
         this.enabledStack.pop();
      }
      if (!this.alphaStack.isEmpty()) {
         this.amount2 = this.alphaStack.pop();
         this.vg.alpha(this.amount2);
      }
      this.vg.restore();
   }

   public void save() {
      this.alphaStack.push(this.amount2);
      this.vg.save();
   }

   public void restore() {
      if (!this.alphaStack.isEmpty()) {
         this.amount2 = this.alphaStack.pop();
         this.vg.alpha(this.amount2);
      }
      this.vg.restore();
   }

   public void translate(float x, float y) {
      this.vg.translate(x, y);
   }

   public void apply(float x, float y) {
      this.vg.translate(x, y);
   }

   public void apply(float x) {
      this.vg.scale(x);
   }

   public void apply2(float x, float y) {
      this.vg.scale(x, y);
   }

   public void apply2(float angle) {
      this.vg.rotate(angle);
   }

   public void render2(float x, float y, float scale) {
      this.vg.translate(x + 0.0F, y + 0.0F);
      float cx = x;
      float cy = y;
      this.vg.translate(cx, cy);
      this.vg.scale(scale);
      this.vg.translate(-cx, -cy);
   }

   public void apply3(float floatValue) {
      this.amount2 = this.amount2 * Math.clamp(floatValue, 0.0F, 1.0F);
      this.vg.alpha(this.amount2);
   }

   public void setEnabled3(boolean enabled32) {
   }

   public float getAmount2() {
      return this.amount2;
   }

   // ---- shapes ----

   public void render(float x, float y, float w, float h) {
      this.vg.scissor(x, y, w, h);
   }

   public void render(float x, float y, float w, float h, float r, int color) {
      this.vg.rect(x, y, w, h, r, color);
   }

   public void render(float sx, float sy, float w, float h, float r, int c1, int c2, boolean vertical) {
      this.vg.rectGradient(sx, sy, w, h, r, c1, c2, vertical);
   }

   public void render(float sx, float sy, float w, float h, float r, int c1, int c2, float angle) {
      // Angle-based gradient; approximated with a vertical gradient.
      this.vg.rectGradient(sx, sy, w, h, r, c1, c2, true);
   }

   public void render(
      float x, float y, float w, float h,
      float rTL, float rTR, float rBR, float rBL, int c1, int c2) {
      this.vg.rectVaryingGradient(x, y, w, h, rTL, rTR, rBR, rBL, c1, c2);
   }

   public void render(
      float x, float y, float w, float h,
      float rTL, float rTR, float rBR, float rBL, int c1, int c2, boolean vertical) {
      this.vg.rectVaryingGradient(x, y, w, h, rTL, rTR, rBR, rBL, c1, c2);
   }

   public void render(float x, float y, float w, float h, float r, float size, int color) {
      this.vg.rectOutline(x, y, w, h, r, size, color);
   }

   public void render(float sx, float sy, float w, float h, float r, float size, int c1, int c2, boolean vertical) {
      this.vg.rectOutline(sx, sy, w, h, r, size, ArgbColors.lerp(c1, c2, 0.5F));
   }

   public void render2(float x, float y, float w, float h, float r, float spread, int color) {
      this.vg.glow(x, y, w, h, r, spread, color);
   }

   public void render3(float x, float y, float x2, float y2, float x3, float y3, int color) {
      this.vg.triangle(x, y, x2, y2, x3, y3, color);
   }

   public void render(float x, float y, float size, int color) {
      this.vg.star(x, y, size, 0.0F, color, true);
   }

   public void render(float x, float y, float size, float size2, int color) {
      this.vg.star(x, y, size, size2 * 0.5F, color, false);
   }

   public void render(float cx, float cy, float outr, float a0, float a1, int c1, int c2) {
      this.vg.circleGlow(cx, cy, outr, outr * 0.5F, c1);
   }

   public void render(float[] xs, float[] ys, int n, float size, int color) {
      if (n >= 2) {
         for (int i = 0; i < n - 1; i++) {
            this.vg.line(xs[i], ys[i], xs[i + 1], ys[i + 1], size, color);
         }
      }
   }

   public void render4(float cx, float cy, float r, float a0, float a1, float size, int color) {
      this.vg.circleOutline(cx, cy, r, size, color);
   }

   public void render2(float cx, float cy, float r, int color) {
      this.vg.circle(cx, cy, r, color);
   }

   public void render(
      int image, float ox, float oy, float ex, float ey, float x, float y, float w, float h, float alpha) {
      if (image > 0) {
         this.vg.imagePattern(image, ox, oy, ex, ey, 0.0F, x, y, w, h, alpha);
      }
   }

   public void render2(float cx, float cy, float r, float size, int color) {
      this.vg.circleOutline(cx, cy, r, size, color);
   }

   public void render3(float cx, float cy, float r, float spread, int color) {
      this.vg.circleGlow(cx, cy, r, spread, color);
   }

   public void render2(float x, float y, float x2, float y2, float size, int color) {
      this.vg.line(x, y, x2, y2, size, color);
   }

   public void render(float x, float y, float size, float size2, int color, boolean flag) {
      this.vg.chevron(x, y, size, size2, color, !flag);
   }

   public void render4(float x, float y, float w, float h, int color) {
      this.vg.chevron(x + w * 0.5F, y + h * 0.5F, Math.min(w, h) * 0.5F, 1.8F, color, true);
   }

   public void render(float x, float y, float w, float size, int color, float prog) {
      float p = Math.clamp(prog, 0.0F, 1.0F);
      if (p > 0.001F) {
         this.vg.checkmark(x, y, w * p, size, color);
      }
   }

   public void render2(float x, float y, float w, float h, int color, float prog) {
      // 67client logo glyph — approximated with a simple rounded mark.
      float p = Math.clamp(prog, 0.0F, 1.0F);
      if (p > 0.001F) {
         this.vg.rect(x, y, w * p, h, 4.0F, ArgbColors.withOpacity(color, p));
      }
   }

   public void render5(float x, float y, float w, float size, int color) {
      this.vg.cross(x + w * 0.5F, y + w * 0.5F, w * 0.3F, size, color);
   }

   // ---- text ----

   private static int alignX(float x, float width, int align) {
      if ((align & 4) != 0) {
         return x - width;
      }
      if ((align & 2) != 0) {
         return x - width * 0.5F;
      }
      return x;
   }

   public float computeFloat(String text, float x, float y, float size, int color) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.text(text, alignX(x, w, 1), y, size, color);
      return w;
   }

   public float computeFloat2(String text, float x, float y, float size, int color) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.text(text, alignX(x, w, 2), y, size, color);
      return w;
   }

   public float computeFloat3(String text, float x, float y, float size, int color) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.text(text, alignX(x, w, 4), y, size, color);
      return w;
   }

   public float computeFloat(String text, float x, float y, float size, int color, int align) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.text(text, alignX(x, w, align), y, size, color);
      return w;
   }

   public float computeFloat4(String text, float x, float y, float size, int color) {
      return this.computeFloat(text, x, y, size, color);
   }

   public float computeFloat2(String text, float x, float y, float size, int c1, int c2) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.textGradient(text, alignX(x, w, 2), y, size, c1, c2);
      return w;
   }

   public void render(String text, float x, float y, float size, int color) {
      if (text != null && !text.isEmpty()) {
         this.vg.text(text, x, y, size, color);
      }
   }

   public float computeFloat(String text, float x, float y, float size, int color, String font, int align) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      this.vg.text(text, alignX(x, w, align), y, size, color);
      return w;
   }

   public float computeFloat(String text, float x, float y, float size, int color, float maxWidth) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      if (w <= maxWidth) {
         this.vg.text(text, x, y, size, color);
         return w;
      }
      return this.vg.textTruncated(text, x, y, size, color, maxWidth);
   }

   public float computeFloat(String text, float x, float y, float size, int color, float maxWidth, int align) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      float w = this.vg.textWidth(text, size);
      float drawn;
      if (w <= maxWidth) {
         this.vg.text(text, alignX(x, w, align), y, size, color);
         drawn = w;
      } else {
         drawn = this.vg.textTruncated(text, alignX(x, w, align), y, size, color, maxWidth);
      }
      return drawn;
   }

   public float computeFloat(String text, float size) {
      if (text == null || text.isEmpty()) {
         return 0.0F;
      }
      return this.vg.textWidth(text, size);
   }

   public float computeFloat2(String text, float size) {
      return this.computeFloat(text, size);
   }

   public float computeFloat(String text, float size, String font) {
      return this.computeFloat(text, size);
   }
}
