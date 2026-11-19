package dev.aether.render;

import dev.aether.module.impl.SkyLyricsModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.util.Colors;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws the Sky Lyrics module's snapshot as world-anchored text floating in the sky ahead of the
 * player. Positions are projected from world space each frame (same projection the world nametags
 * use), so the lyrics stay anchored to the world and remain visible through fog and clouds.
 */
public final class SkyLyricsRenderer {
   private static final Random RANDOM = new Random();
   private static final float BASE_SIZE = 18.0F;
   private static final float LINE_GAP = 1.75F;
   private static final float SPACE_W = 4.0F;
   private static final long BUMP_MS = 280L;
   private static final float LIT_EASE = 6.0F;

   private static final Map<Long, WordState> WORDS = new HashMap<>();

   private SkyLyricsRenderer() {
   }

   private static long key(int line, int word) {
      return (long)line * 1000000L + word;
   }

   private static final class WordState {
      float rotationDeg;
      float litAmount;
      long bumpStart;
      float sizeVar;
      float offsetY;
   }

   private static WordState state(long key, float maxRot) {
      WordState s = WORDS.get(key);
      if (s == null) {
         s = new WordState();
         s.rotationDeg = randRot(maxRot);
         s.sizeVar = 0.90F + RANDOM.nextFloat() * 0.20F;
         s.offsetY = (float)(RANDOM.nextDouble() * 2.4 - 1.2);
         WORDS.put(key, s);
      }
      return s;
   }

   private static float randRot(float maxRot) {
      return (float)(RANDOM.nextDouble() * 2.0 - 1.0) * maxRot;
   }

   public static void render(NVGRenderer vg, SkyLyricsModule module) {
      if (!module.isEnabled()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      Camera camera = mc.gameRenderer.getMainCamera();
      if (player == null || camera == null || !WorldProjection.isValid()) {
         return;
      }

      float scale = module.textScale.getFloat();
      float maxRot = Math.min(15.0F, Math.max(0.0F, module.maxRotation.getFloat()));
      float size = BASE_SIZE * scale;
      float lineHeight = size * LINE_GAP;

      // Anchor: in front of the player's view direction, at the configured height.
      Vector3f fwd = new Vector3f(camera.forwardVector());
      Vec3 eye = player.getEyePosition();
      double distance = module.distance.getFloat();
      double ax = eye.x + fwd.x * distance;
      double ay = eye.y + module.height.getFloat();
      double az = eye.z + fwd.z * distance;
      float[] anchor = WorldProjection.project(ax, ay, az);
      if (anchor == null) {
         return;
      }

      SkyLyricsModule.Snapshot snap = module.snapshot();
      if (snap == null || snap.lines().isEmpty()) {
         // Nothing playing / no lyrics yet: show a status line so the feature is visibly running.
         String status = module.statusText();
         if (status != null && !status.isEmpty()) {
            float statusW = vg.textWidth(status, size);
            vg.save();
            vg.translate(anchor[0], anchor[1] + size * 0.35F);
            if (module.glow.get()) {
               vg.glow(-statusW * 0.5F - 6.0F, -size * 0.5F - 6.0F, statusW + 12.0F, size + 12.0F, 5.0F, 7.0F, Colors.withAlpha(0xFF3D7AFF, 0.3F));
            }
            vg.text(status, -statusW * 0.5F, 0.0F, size, 0xFFB8C8E8);
            vg.restore();
         }
         return;
      }

      int lineCount = snap.lines().size();
      int current = snap.currentLine();
      if (current < 0) {
         current = 0;
      } else if (current >= lineCount) {
         current = lineCount - 1;
      }
      int currentWord = snap.currentWord();
      int half = (int)Math.max(0.0, module.visibleLines.getFloat() - 1.0F) / 2;
      int first = Math.max(0, current - half);
      int last = Math.min(lineCount - 1, current + half);

      long now = System.nanoTime();
      int gray = module.grayColor.get();
      int white = module.whiteColor.get();
      boolean glow = module.glow.get();

      for (int idx = first; idx <= last; idx++) {
         SkyLyricsModule.Line line = snap.lines().get(idx);
         if (line.text().isEmpty() && line.words().length == 0) {
            continue;
         }
         int rel = idx - current;
         float lineAlpha = 1.0F;
         float lineY = anchor[1] + rel * lineHeight;

         if (line.words().length == 0) {
            // Word-less line: render the whole line as one element.
            boolean lit = rel <= 0;
            WordState st = state(key(idx, 0), maxRot);
            if (lit && st.litAmount <= 0.001F) {
               st.rotationDeg = randRot(maxRot);
               st.bumpStart = now;
            }
            st.litAmount = ease(st.litAmount, lit ? 1.0F : 0.0F, LIT_EASE);
            float bump = 1.0F;
            if (st.bumpStart > 0L) {
               float p = (float)((now - st.bumpStart) / 1.0E6 / BUMP_MS);
               if (p < 1.0F) {
                  bump = 1.0F + 0.1F * (1.0F - p) * (1.0F - p);
               } else {
                  st.bumpStart = 0L;
               }
            }
            float wordSize = size * st.sizeVar * bump;
            float wordW = vg.textWidth(line.text(), wordSize);
            int color = Colors.lerp(gray, white, st.litAmount);
            vg.save();
            vg.alpha(lineAlpha);
            vg.translate(anchor[0], lineY + wordSize * 0.35F);
            vg.rotate((float)Math.toRadians(st.rotationDeg));
            if (glow) {
               float gw = wordW + 6.0F;
               float gh = wordSize + 6.0F;
               vg.glow(-gw * 0.5F, -gh * 0.5F, gw, gh, 4.0F, 6.0F, Colors.withAlpha(color, 0.22F * st.litAmount));
            }
            vg.text(line.text(), -wordW * 0.5F, 0.0F, wordSize, color);
            vg.restore();
            continue;
         }

         // Total width to center the line on the anchor.
         float totalW = 0.0F;
         for (int w = 0; w < line.words().length; w++) {
            totalW += vg.textWidth(line.words()[w], size) + (w + 1 < line.words().length ? SPACE_W : 0.0F);
         }
         float x = anchor[0] - totalW * 0.5F;

         for (int w = 0; w < line.words().length; w++) {
            String word = line.words()[w];
            if (word.isEmpty()) {
               continue;
            }
            boolean lit = rel < 0 || (rel == 0 && w <= currentWord);
            WordState st = state(key(idx, w), maxRot);
            if (lit && st.litAmount <= 0.001F) {
               // Newly sung: fresh random tilt + bump animation.
               st.rotationDeg = randRot(maxRot);
               st.bumpStart = now;
            }
            st.litAmount = ease(st.litAmount, lit ? 1.0F : 0.0F, LIT_EASE);
            float bump = 1.0F;
            if (st.bumpStart > 0L) {
               float p = (float)((now - st.bumpStart) / 1.0E6 / BUMP_MS);
               if (p < 1.0F) {
                  bump = 1.0F + 0.1F * (1.0F - p) * (1.0F - p);
               } else {
                  st.bumpStart = 0L;
               }
            }
            float wordSize = size * st.sizeVar * bump;
            float wordW = vg.textWidth(word, wordSize);
            float wy = lineY + st.offsetY;
            int color = Colors.lerp(gray, white, st.litAmount);

            vg.save();
            vg.alpha(lineAlpha);
            vg.translate(x + wordW * 0.5F, wy + wordSize * 0.35F);
            vg.rotate((float)Math.toRadians(st.rotationDeg));
            if (glow) {
               float gw = wordW + 6.0F;
               float gh = wordSize + 6.0F;
               vg.glow(-gw * 0.5F, -gh * 0.5F, gw, gh, 4.0F, 6.0F, Colors.withAlpha(color, 0.22F * st.litAmount));
            }
            vg.text(word, -wordW * 0.5F, 0.0F, wordSize, color);
            vg.restore();

            x += wordW + SPACE_W;
         }
      }
   }

   private static float ease(float current, float target, float speed) {
      if (current < target) {
         return Math.min(target, current + speed * 0.02F);
      } else {
         return Math.max(target, current - speed * 0.02F);
      }
   }
}
