package dev.aether.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.aether.AetherClient;
import dev.aether.module.impl.CustomGlintModule;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ARGB;

public final class GlintTextureTinter {
   private static final Identifier[] TEXTURES = new Identifier[]{ItemRenderer.ENCHANTED_GLINT_ITEM, ItemRenderer.ENCHANTED_GLINT_ARMOR};
   private static final NativeImage[] originals = new NativeImage[TEXTURES.length];
   private static final NativeImage[] scratch = new NativeImage[TEXTURES.length];
   private static final GpuTexture[] lastUploaded = new GpuTexture[TEXTURES.length];
   private static final Map<String, NativeImage> customCache = new HashMap<>();
   private static boolean loaded;
   private static boolean written;
   private static String lastKey;

   private GlintTextureTinter() {
   }

   public static void tick() {
      CustomGlintModule module = AetherClient.modules() == null ? null : AetherClient.modules().customGlint;
      if (module != null) {
         if (module.isActive()) {
            if (module.usesTexture()) {
               applyTexture(module.textureName(), module.strengthUnit());
            } else {
               applyTint(module.glintColor());
            }
         } else if (written) {
            restore();
         }
      }
   }

   private static void ensureLoaded() {
      if (!loaded) {
         loaded = true;
         Minecraft mc = Minecraft.getInstance();

         for (int i = 0; i < TEXTURES.length; i++) {
            Optional<Resource> res = mc.getResourceManager().getResource(TEXTURES[i]);
            if (!res.isEmpty()) {
               try (InputStream in = res.get().open()) {
                  originals[i] = NativeImage.read(in);
               } catch (Exception var8) {
                  originals[i] = null;
               }
            }
         }
      }
   }

   private static void applyTint(int color) {
      ensureLoaded();
      upload("tint:" + color, i -> tintInto(i, originals[i], color));
   }

   private static void applyTexture(String name, float strength) {
      ensureLoaded();
      NativeImage custom = loadCustom(name);
      if (custom != null) {
         int strq = Math.round(strength * 255.0F);
         upload("tex:" + name + ":" + strq, i -> sampleInto(i, custom, strength));
      }
   }

   private static void upload(String key, GlintTextureTinter.Source source) {
      TextureManager tm = Minecraft.getInstance().getTextureManager();
      boolean texChanged = false;

      for (int i = 0; i < TEXTURES.length; i++) {
         AbstractTexture t = tm.getTexture(TEXTURES[i]);
         if (t != null && t.getTexture() != lastUploaded[i]) {
            texChanged = true;
         }
      }

      if (!written || !key.equals(lastKey) || texChanged) {
         for (int ix = 0; ix < TEXTURES.length; ix++) {
            if (originals[ix] != null) {
               AbstractTexture t = tm.getTexture(TEXTURES[ix]);
               GpuTexture gpu = t == null ? null : t.getTexture();
               if (gpu != null) {
                  NativeImage img = source.build(ix);
                  if (img != null) {
                     RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, img);
                     lastUploaded[ix] = gpu;
                  }
               }
            }
         }

         written = true;
         lastKey = key;
      }
   }

   private static void restore() {
      TextureManager tm = Minecraft.getInstance().getTextureManager();

      for (int i = 0; i < TEXTURES.length; i++) {
         NativeImage src = originals[i];
         if (src != null) {
            AbstractTexture t = tm.getTexture(TEXTURES[i]);
            GpuTexture gpu = t == null ? null : t.getTexture();
            if (gpu != null) {
               RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, src);
               lastUploaded[i] = gpu;
            }
         }
      }

      written = false;
      lastKey = null;
   }

   private static NativeImage tintInto(int i, NativeImage src, int color) {
      int w = src.getWidth();
      int h = src.getHeight();
      NativeImage dst = ensureScratch(i, w, h);
      int tr = ARGB.red(color);
      int tg = ARGB.green(color);
      int tb = ARGB.blue(color);

      for (int y = 0; y < h; y++) {
         for (int x = 0; x < w; x++) {
            int p = src.getPixel(x, y);
            int v = Math.max(ARGB.red(p), Math.max(ARGB.green(p), ARGB.blue(p)));
            dst.setPixel(x, y, ARGB.color(ARGB.alpha(p), tr * v / 255, tg * v / 255, tb * v / 255));
         }
      }

      return dst;
   }

   private static NativeImage sampleInto(int i, NativeImage src, float strength) {
      int w = originals[i].getWidth();
      int h = originals[i].getHeight();
      int sw = src.getWidth();
      int sh = src.getHeight();
      NativeImage dst = ensureScratch(i, w, h);

      for (int y = 0; y < h; y++) {
         float fy = ((float)y + 0.5F) * (float)sh / (float)h - 0.5F;

         for (int x = 0; x < w; x++) {
            float fx = ((float)x + 0.5F) * (float)sw / (float)w - 0.5F;
            int p = bilinearWrapped(src, fx, fy, sw, sh);
            int r = Math.round((float)ARGB.red(p) * strength);
            int g = Math.round((float)ARGB.green(p) * strength);
            int b = Math.round((float)ARGB.blue(p) * strength);
            dst.setPixel(x, y, ARGB.color(ARGB.alpha(p), r, g, b));
         }
      }

      return dst;
   }

   private static NativeImage ensureScratch(int i, int w, int h) {
      NativeImage dst = scratch[i];
      if (dst == null || dst.getWidth() != w || dst.getHeight() != h) {
         if (dst != null) {
            dst.close();
         }

         dst = new NativeImage(w, h, false);
         scratch[i] = dst;
      }

      return dst;
   }

   private static int bilinearWrapped(NativeImage img, float fx, float fy, int sw, int sh) {
      int x0 = Math.floorMod((int)Math.floor((double)fx), sw);
      int y0 = Math.floorMod((int)Math.floor((double)fy), sh);
      int x1 = (x0 + 1) % sw;
      int y1 = (y0 + 1) % sh;
      float dx = fx - (float)Math.floor((double)fx);
      float dy = fy - (float)Math.floor((double)fy);
      int p00 = img.getPixel(x0, y0);
      int p10 = img.getPixel(x1, y0);
      int p01 = img.getPixel(x0, y1);
      int p11 = img.getPixel(x1, y1);
      int a = mix(ARGB.alpha(p00), ARGB.alpha(p10), ARGB.alpha(p01), ARGB.alpha(p11), dx, dy);
      int r = mix(ARGB.red(p00), ARGB.red(p10), ARGB.red(p01), ARGB.red(p11), dx, dy);
      int g = mix(ARGB.green(p00), ARGB.green(p10), ARGB.green(p01), ARGB.green(p11), dx, dy);
      int b = mix(ARGB.blue(p00), ARGB.blue(p10), ARGB.blue(p01), ARGB.blue(p11), dx, dy);
      return ARGB.color(a, r, g, b);
   }

   private static int mix(int c00, int c10, int c01, int c11, float dx, float dy) {
      float top = (float)c00 + (float)(c10 - c00) * dx;
      float bot = (float)c01 + (float)(c11 - c01) * dx;
      return Math.round(top + (bot - top) * dy);
   }

   private static NativeImage loadCustom(String name) {
      if (customCache.containsKey(name)) {
         return customCache.get(name);
      } else {
         NativeImage img = null;
         Identifier id = Identifier.fromNamespaceAndPath("aetherclient", "textures/misc/glints/" + name + ".png");
         Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(id);
         if (res.isPresent()) {
            try (InputStream in = res.get().open()) {
               img = NativeImage.read(in);
            } catch (Exception var9) {
               img = null;
            }
         }

         customCache.put(name, img);
         return img;
      }
   }

   private interface Source {
      NativeImage build(int i);
   }
}
