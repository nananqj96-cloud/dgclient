package dev.aether.render;

import dev.aether.module.impl.MotionBlurModule;
import dev.aether.render.nanovg.NVGRenderer;

/**
 * Renders motion blur post-processing effect.
 */
public final class MotionBlurRenderer {
   private MotionBlurRenderer() {
   }

   public static void render(NVGRenderer vg, int width, int height, MotionBlurModule module) {
      // Motion blur rendering stub - detailed post-processing disabled
   }
}
