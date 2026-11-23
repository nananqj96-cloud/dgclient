package dev.aether.render;

import dev.aether.module.impl.GreatColorsModule;
import org.lwjgl.opengl.GL33C;

public final class SaturationRenderer {
   private static int program = -1;
   private static int vao = -1;
   private static int vbo = -1;
   private static boolean compiled = false;

   private SaturationRenderer() {}

   private static void compile() {
      if (compiled) return;
      compiled = true;

      String vertSrc = """
         #version 33 core
         layout(location = 0) in vec2 aPos;
         out vec2 vUv;
         void main() {
            vUv = aPos * 0.5 + 0.5;
            gl_Position = vec4(aPos, 0.0, 1.0);
         }
         """;

      String fragSrc = """
         #version 33 core
         in vec2 vUv;
         out vec4 fragColor;
         uniform sampler2D uTex;
         uniform float uSaturation;

         vec3 rgb2hsl(vec3 c) {
            float maxC = max(max(c.r, c.g), c.b);
            float minC = min(min(c.r, c.g), c.b);
            float l = (maxC + minC) * 0.5;
            if (maxC == minC) return vec3(0.0, 0.0, l);
            float d = maxC - minC;
            float s = l > 0.5 ? d / (2.0 - maxC - minC) : d / (maxC + minC);
            float h;
            if (maxC == c.r) h = (c.g - c.b) / d + (c.g < c.b ? 6.0 : 0.0);
            else if (maxC == c.g) h = (c.b - c.r) / d + 2.0;
            else h = (c.r - c.g) / d + 4.0;
            return vec3(h / 6.0, s, l);
         }

         vec3 hsl2rgb(vec3 hsl) {
            float h = hsl.x * 6.0;
            float s = hsl.y;
            float l = hsl.z;
            float c = (1.0 - abs(2.0 * l - 1.0)) * s;
            float x = c * (1.0 - abs(mod(h, 2.0) - 1.0));
            float m = l - c * 0.5;
            vec3 rgb;
            if (h < 1.0) rgb = vec3(c, x, 0.0);
            else if (h < 2.0) rgb = vec3(x, c, 0.0);
            else if (h < 3.0) rgb = vec3(0.0, c, x);
            else if (h < 4.0) rgb = vec3(0.0, x, c);
            else if (h < 5.0) rgb = vec3(x, 0.0, c);
            else rgb = vec3(c, 0.0, x);
            return rgb + m;
         }

         void main() {
            vec4 tex = texture(uTex, vUv);
            vec3 hsl = rgb2hsl(tex.rgb);
            hsl.y *= uSaturation;
            hsl.y = clamp(hsl.y, 0.0, 1.0);
            vec3 result = hsl2rgb(hsl);
            fragColor = vec4(result, tex.a);
         }
         """;

      int vs = GL33C.glCreateShader(GL33C.GL_VERTEX_SHADER);
      GL33C.glShaderSource(vs, vertSrc);
      GL33C.glCompileShader(vs);

      int fs = GL33C.glCreateShader(GL33C.GL_FRAGMENT_SHADER);
      GL33C.glShaderSource(fs, fragSrc);
      GL33C.glCompileShader(fs);

      program = GL33C.glCreateProgram();
      GL33C.glAttachShader(program, vs);
      GL33C.glAttachShader(program, fs);
      GL33C.glLinkProgram(program);

      GL33C.glDeleteShader(vs);
      GL33C.glDeleteShader(fs);

      vao = GL33C.glGenVertexArrays();
      vbo = GL33C.glGenBuffers();
      GL33C.glBindVertexArray(vao);
      GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo);
      float[] verts = {
         -1.0f, -1.0f,
          1.0f, -1.0f,
         -1.0f,  1.0f,
         -1.0f,  1.0f,
          1.0f, -1.0f,
          1.0f,  1.0f,
      };
      GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, verts, GL33C.GL_STATIC_DRAW);
      GL33C.glEnableVertexAttribArray(0);
      GL33C.glVertexAttribPointer(0, 2, GL33C.GL_FLOAT, false, 0, 0);
      GL33C.glBindVertexArray(0);
   }

   public static void render(int srcTexId, int width, int height, GreatColorsModule module) {
      if (program == -1) {
         compile();
         if (program == -1) return;
      }

      float sat = module.getSaturation();
      if (sat == 1.0F) return;

      GL33C.glUseProgram(program);
      GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
      GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, srcTexId);
      GL33C.glUniform1i(GL33C.glGetUniformLocation(program, "uTex"), 0);
      GL33C.glUniform1f(GL33C.glGetUniformLocation(program, "uSaturation"), sat);

      GL33C.glDisable(GL33C.GL_BLEND);
      GL33C.glDisable(GL33C.GL_DEPTH_TEST);
      GL33C.glDisable(GL33C.GL_SCISSOR_TEST);

      GL33C.glBindVertexArray(vao);
      GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 6);
      GL33C.glBindVertexArray(0);

      GL33C.glUseProgram(0);
   }

   public static void cleanup() {
      if (vao != -1) { GL33C.glDeleteVertexArrays(vao); vao = -1; }
      if (vbo != -1) { GL33C.glDeleteBuffers(vbo); vbo = -1; }
      if (program != -1) { GL33C.glDeleteProgram(program); program = -1; }
      compiled = false;
   }
}
