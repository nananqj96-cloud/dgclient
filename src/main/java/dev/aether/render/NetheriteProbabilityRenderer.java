package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.NetheriteFinderModule;
import dev.aether.render.nanovg.NVGRenderer;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class NetheriteProbabilityRenderer {
   private static final int MAX_LABELS = 100;

   private NetheriteProbabilityRenderer() {
   }

   public static void renderBoxes(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, NetheriteFinderModule netheriteFinderModule) {
      if (netheriteFinderModule.debrisHotspot.get()) {
         Minecraft var4 = Minecraft.getInstance();
         long var5 = System.currentTimeMillis() % 2000L;
         double var7 = var5 < 1000L ? (double)var5 / 1000.0 : (double)(2000L - var5) / 1000.0;
         int var9 = netheriteFinderModule.hotspotColor.get();

         for (NetheriteFinderModule.Hit var11 : netheriteFinderModule.hits()) {
            if (!var11.exact() && netheriteFinderModule.debrisHotspot.get() && !var11.probabilityRegions().isEmpty()) {
               NetheriteFinderModule.ProbabilityRegion var12 = var11.probabilityRegions().getFirst();
               if (!isInsideOrNear(var4, vec3, var12)) {
                  EspBoxRenderer.fill(
                     bufferSource,
                     poseStack,
                     vec3,
                     var12.x1(),
                     var12.y1(),
                     var12.z1(),
                     var12.x2(),
                     var12.y2(),
                     var12.z2(),
                     Colors.withAlpha(var9, 34 - (int)(10.0 * var7))
                  );
               }

               EspBoxRenderer.outline(
                  bufferSource, poseStack, vec3, var12.x1(), var12.y1(), var12.z1(), var12.x2(), var12.y2(), var12.z2(), Colors.withAlpha(var9, 220), 1.4F
               );
            }
         }

         EspBoxRenderer.flush(bufferSource);
      }
   }

   public static void renderLabels(NVGRenderer nVGRenderer) {
      if (WorldProjection.isValid()) {
         ModuleManager var1 = AetherClient.modules();
         NetheriteFinderModule var2 = var1 == null ? null : var1.netheriteFinder;
         if (var2 != null && var2.isEnabled() && var2.debrisHotspot.get()) {
            int var3 = var2.hotspotColor.get();
            int var4 = 0;

            for (NetheriteFinderModule.Hit var6 : var2.hits()) {
               if (!var6.exact() && !var6.probabilityRegions().isEmpty()) {
                  NetheriteFinderModule.ProbabilityRegion var7 = var6.probabilityRegions().getFirst();
                  float[] var8 = WorldProjection.project((var7.x1() + var7.x2()) * 0.5, var7.y2() + 0.25, (var7.z1() + var7.z2()) * 0.5);
                  if (var8 != null
                     && var8[0] >= -40.0F
                     && var8[0] <= OverlayRenderer.uiWidth() + 40.0F
                     && var8[1] >= -20.0F
                     && var8[1] <= OverlayRenderer.uiHeight() + 20.0F) {
                     drawLabel(nVGRenderer, var8[0], var8[1], var7.chance(), var3);
                     if (++var4 >= 100) {
                        return;
                     }
                  }
               }
            }
         }
      }
   }

   private static void drawLabel(NVGRenderer nVGRenderer, float f, float g, double d, int i) {
      String var6 = Math.round(d * 100.0) + "%";
      float var7 = 13.0F;
      float var8 = 6.0F;
      float var9 = nVGRenderer.textWidth(var6, var7) + var8 * 2.0F;
      float var10 = 19.0F;
      float var11 = f - var9 * 0.5F;
      float var12 = g - var10 * 0.5F;
      nVGRenderer.glow(var11, var12, var9, var10, 5.0F, 3.0F, Colors.withAlpha(i, 0.25F));
      nVGRenderer.rect(var11, var12, var9, var10, 5.0F, Colors.withAlpha(-16777216, 0.68F));
      nVGRenderer.rectOutline(var11, var12, var9, var10, 5.0F, 1.0F, Colors.withAlpha(i, 0.85F));
      nVGRenderer.text(var6, f - nVGRenderer.textWidth(var6, var7) * 0.5F, g, var7, Colors.lighten(i, 0.75F));
   }

   private static boolean isInsideOrNear(Minecraft minecraft, Vec3 vec3, NetheriteFinderModule.ProbabilityRegion probabilityRegion) {
      double var3 = 0.05;
      double var5 = probabilityRegion.x1() - var3;
      double var7 = probabilityRegion.y1() - var3;
      double var9 = probabilityRegion.z1() - var3;
      double var11 = probabilityRegion.x2() + var3;
      double var13 = probabilityRegion.y2() + var3;
      double var15 = probabilityRegion.z2() + var3;
      if (minecraft.player != null) {
         AABB var17 = minecraft.player.getBoundingBox();
         if (var17.maxX >= var5 && var17.minX <= var11 && var17.maxY >= var7 && var17.minY <= var13 && var17.maxZ >= var9 && var17.minZ <= var15) {
            return true;
         }
      }

      return vec3.x >= var5 && vec3.x <= var11 && vec3.y >= var7 && vec3.y <= var13 && vec3.z >= var9 && vec3.z <= var15;
   }
}
