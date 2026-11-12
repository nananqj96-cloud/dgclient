package dev.aether.render.nanovg;

import org.lwjgl.opengl.GL33C;

/**
 * Zero-allocation OpenGL state snapshot/restore around NanoVG passes.
 * Reuses a single render-thread instance and scratch array to eliminate per-frame GC churn.
 */
public final class GlStateSnapshot {
   private static final GlStateSnapshot INSTANCE = new GlStateSnapshot();
   private static final int[] MASK_SCRATCH = new int[4];

   private int program;
   private int vao;
   private int arrayBuffer;
   private int uniformBuffer;
   private int uniformBufferBase0;
   private int activeTexture;
   private int texture2D;
   private int sampler0;
   private int blendSrcRgb;
   private int blendDstRgb;
   private int blendSrcAlpha;
   private int blendDstAlpha;
   private int blendEqRgb;
   private int blendEqAlpha;
   private int cullFaceMode;
   private int frontFace;
   private int depthFunc;
   private int stencilFunc;
   private int stencilRef;
   private int stencilValueMask;
   private int stencilWriteMask;
   private int stencilFail;
   private int stencilPassDepthFail;
   private int stencilPassDepthPass;
   private boolean blend;
   private boolean cullFace;
   private boolean depthTest;
   private boolean scissorTest;
   private boolean stencilTest;
   private boolean depthMask;
   private final int[] viewport = new int[4];
   private final int[] scissorBox = new int[4];
   private final boolean[] colorMask = new boolean[4];
   private int unpackAlignment;
   private int unpackRowLength;
   private int unpackSkipPixels;
   private int unpackSkipRows;

   private GlStateSnapshot() {
   }

   public static GlStateSnapshot capture() {
      GlStateSnapshot s = INSTANCE;
      s.program = GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
      s.vao = GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING);
      s.arrayBuffer = GL33C.glGetInteger(GL33C.GL_ARRAY_BUFFER_BINDING);
      s.uniformBuffer = GL33C.glGetInteger(GL33C.GL_UNIFORM_BUFFER_BINDING);
      s.uniformBufferBase0 = GL33C.glGetIntegeri(GL33C.GL_UNIFORM_BUFFER_BINDING, 0);
      s.activeTexture = GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
      if (s.activeTexture != GL33C.GL_TEXTURE0) {
         GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
      }
      s.texture2D = GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);
      s.sampler0 = GL33C.glGetInteger(GL33C.GL_SAMPLER_BINDING);
      if (s.sampler0 != 0) {
         GL33C.glBindSampler(0, 0);
      }
      if (s.activeTexture != GL33C.GL_TEXTURE0) {
         GL33C.glActiveTexture(s.activeTexture);
      }

      s.blend = GL33C.glIsEnabled(GL33C.GL_BLEND);
      s.blendSrcRgb = GL33C.glGetInteger(GL33C.GL_BLEND_SRC_RGB);
      s.blendDstRgb = GL33C.glGetInteger(GL33C.GL_BLEND_DST_RGB);
      s.blendSrcAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_SRC_ALPHA);
      s.blendDstAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_DST_ALPHA);
      s.blendEqRgb = GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_RGB);
      s.blendEqAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_ALPHA);

      s.cullFace = GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
      s.cullFaceMode = GL33C.glGetInteger(GL33C.GL_CULL_FACE_MODE);
      s.frontFace = GL33C.glGetInteger(GL33C.GL_FRONT_FACE);

      s.depthTest = GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST);
      s.depthFunc = GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC);
      s.depthMask = GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK);

      s.scissorTest = GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
      s.stencilTest = GL33C.glIsEnabled(GL33C.GL_STENCIL_TEST);
      s.stencilFunc = GL33C.glGetInteger(GL33C.GL_STENCIL_FUNC);
      s.stencilRef = GL33C.glGetInteger(GL33C.GL_STENCIL_REF);
      s.stencilValueMask = GL33C.glGetInteger(GL33C.GL_STENCIL_VALUE_MASK);
      s.stencilWriteMask = GL33C.glGetInteger(GL33C.GL_STENCIL_WRITEMASK);
      s.stencilFail = GL33C.glGetInteger(GL33C.GL_STENCIL_FAIL);
      s.stencilPassDepthFail = GL33C.glGetInteger(GL33C.GL_STENCIL_PASS_DEPTH_FAIL);
      s.stencilPassDepthPass = GL33C.glGetInteger(GL33C.GL_STENCIL_PASS_DEPTH_PASS);

      GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, s.viewport);
      GL33C.glGetIntegerv(GL33C.GL_SCISSOR_BOX, s.scissorBox);
      GL33C.glGetIntegerv(GL33C.GL_COLOR_WRITEMASK, MASK_SCRATCH);
      s.colorMask[0] = MASK_SCRATCH[0] != 0;
      s.colorMask[1] = MASK_SCRATCH[1] != 0;
      s.colorMask[2] = MASK_SCRATCH[2] != 0;
      s.colorMask[3] = MASK_SCRATCH[3] != 0;

      s.unpackAlignment = GL33C.glGetInteger(GL33C.GL_UNPACK_ALIGNMENT);
      s.unpackRowLength = GL33C.glGetInteger(GL33C.GL_UNPACK_ROW_LENGTH);
      s.unpackSkipPixels = GL33C.glGetInteger(GL33C.GL_UNPACK_SKIP_PIXELS);
      s.unpackSkipRows = GL33C.glGetInteger(GL33C.GL_UNPACK_SKIP_ROWS);
      return s;
   }

   public void restore() {
      GL33C.glUseProgram(this.program);
      GL33C.glBindVertexArray(this.vao);
      GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, this.arrayBuffer);
      GL33C.glBindBufferBase(GL33C.GL_UNIFORM_BUFFER, 0, this.uniformBufferBase0);
      GL33C.glBindBuffer(GL33C.GL_UNIFORM_BUFFER, this.uniformBuffer);
      GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
      GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, this.texture2D);
      GL33C.glBindSampler(0, this.sampler0);
      if (this.activeTexture != GL33C.GL_TEXTURE0) {
         GL33C.glActiveTexture(this.activeTexture);
      }

      setEnabled(GL33C.GL_BLEND, this.blend);
      GL33C.glBlendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
      GL33C.glBlendEquationSeparate(this.blendEqRgb, this.blendEqAlpha);
      setEnabled(GL33C.GL_CULL_FACE, this.cullFace);
      GL33C.glCullFace(this.cullFaceMode);
      GL33C.glFrontFace(this.frontFace);
      setEnabled(GL33C.GL_DEPTH_TEST, this.depthTest);
      GL33C.glDepthFunc(this.depthFunc);
      GL33C.glDepthMask(this.depthMask);
      setEnabled(GL33C.GL_SCISSOR_TEST, this.scissorTest);
      setEnabled(GL33C.GL_STENCIL_TEST, this.stencilTest);
      GL33C.glStencilFunc(this.stencilFunc, this.stencilRef, this.stencilValueMask);
      GL33C.glStencilMask(this.stencilWriteMask);
      GL33C.glStencilOp(this.stencilFail, this.stencilPassDepthFail, this.stencilPassDepthPass);
      GL33C.glViewport(this.viewport[0], this.viewport[1], this.viewport[2], this.viewport[3]);
      GL33C.glScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
      GL33C.glColorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
      GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, this.unpackAlignment);
      GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, this.unpackRowLength);
      GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, this.unpackSkipPixels);
      GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, this.unpackSkipRows);
   }

   private static void setEnabled(int cap, boolean enabled) {
      if (enabled) {
         GL33C.glEnable(cap);
      } else {
         GL33C.glDisable(cap);
      }
   }
}
