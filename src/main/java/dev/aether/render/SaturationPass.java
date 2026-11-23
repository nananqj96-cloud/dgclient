package dev.aether.render;

import dev.aether.module.impl.GreatColorsModule;
import org.lwjgl.opengl.GL33C;

public final class SaturationPass {
   private static int fbo = -1;
   private static int tex = -1;
   private static int cachedW = -1;
   private static int cachedH = -1;

   private SaturationPass() {}

   private static void ensureFbo(int width, int height) {
      if (fbo != -1 && cachedW == width && cachedH == height) return;
      if (fbo != -1) {
         GL33C.glDeleteFramebuffers(fbo);
         GL33C.glDeleteTextures(tex);
         fbo = -1;
         tex = -1;
      }
      fbo = GL33C.glGenFramebuffers();
      tex = GL33C.glGenTextures();
      GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex);
      GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, GL33C.GL_RGBA, width, height, 0, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
      GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
      GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fbo);
      GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0, GL33C.GL_TEXTURE_2D, tex, 0);
      boolean ok = GL33C.glCheckFramebufferStatus(GL33C.GL_FRAMEBUFFER) == GL33C.GL_FRAMEBUFFER_COMPLETE;
      if (!ok) {
         System.err.println("SaturationPass FBO incomplete");
         fbo = -1;
         tex = -1;
      } else {
         cachedW = width;
         cachedH = height;
      }
   }

   /**
    * Applies saturation to the main render target's color texture.
    * Reads from srcTexId, renders saturated output to a temp FBO,
    * then blits back to the main render target (prevFbo).
    */
   public static void apply(int srcTexId, int width, int height, int prevFbo, GreatColorsModule module) {
      if (!module.isActive()) return;
      if (module.getSaturation() == 1.0F) return;

      ensureFbo(width, height);
      if (fbo == -1) return;

      int[] viewport = GL33C.glGetIntegers(GL33C.GL_VIEWPORT, 0, 4);
      int[] scissor = GL33C.glGetIntegers(GL33C.GL_SCISSOR_BOX, 0, 4);
      boolean scissorEnabled = GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);

      try {
         // 1. Render saturated frame to temp FBO
         GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, fbo);
         GL33C.glViewport(0, 0, width, height);
         GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
         SaturationRenderer.render(srcTexId, width, height, module);

         // 2. Blit temp FBO back to main render target
         GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER, fbo);
         GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, prevFbo);
         GL33C.glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            GL33C.GL_COLOR_BUFFER_BIT,
            GL33C.GL_NEAREST
         );
      } finally {
         GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER, prevFbo);
         GL33C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
         if (scissorEnabled) {
            GL33C.glEnable(GL33C.GL_SCISSOR_TEST);
            GL33C.glScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
         } else {
            GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
         }
      }
   }
}
