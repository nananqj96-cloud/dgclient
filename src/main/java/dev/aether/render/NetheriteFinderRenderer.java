package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.NetheriteFinderModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class NetheriteFinderRenderer {
   private NetheriteFinderRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 vec3, NetheriteFinderModule netheriteFinderModule) {
      Minecraft var4 = Minecraft.getInstance();
      ClientLevel var5 = var4.level;
      if (var5 != null) {
         long var6 = System.currentTimeMillis() % 2000L;
         double var8 = var6 < 1000L ? (double)var6 / 1000.0 : (double)(2000L - var6) / 1000.0;
         int var10 = netheriteFinderModule.outerBoxColor.get();
         int var11 = Colors.withAlpha(var10, 90 - (int)(35.0 * var8));
         int var12 = Colors.withAlpha(var10, 45 - (int)(15.0 * var8));
         boolean var13 = netheriteFinderModule.tracers.get();
         Vector3fc var14 = var4.gameRenderer.getMainCamera().forwardVector();
         NetheriteFinderModule.Hit var15 = var4.player == null
            ? null
            : NetheriteFinderModule.closestSuspiciousHit(
               netheriteFinderModule.hits(), netheriteFinderModule.closestLayer.get(), var4.player.getX(), var4.player.getZ()
            );

         for (NetheriteFinderModule.Hit var17 : netheriteFinderModule.hits()) {
            int var18 = var17 == var15 ? netheriteFinderModule.closestColor.get() : var10;
            int var19 = Colors.withAlpha(var18, var17.exact() ? 90 - (int)(35.0 * var8) : 45 - (int)(15.0 * var8));
            if (!isInsideOrNear(var4, vec3, var17.x1(), var17.y1(), var17.z1(), var17.x2(), var17.y2(), var17.z2(), var17.exact())) {
               EspBoxRenderer.fill(bufferSource, poseStack, vec3, var17.x1(), var17.y1(), var17.z1(), var17.x2(), var17.y2(), var17.z2(), var19);
            }

            EspBoxRenderer.outline(
               bufferSource,
               poseStack,
               vec3,
               var17.x1(),
               var17.y1(),
               var17.z1(),
               var17.x2(),
               var17.y2(),
               var17.z2(),
               Colors.withAlpha(var18, 255),
               var17.exact() ? 2.0F : 1.5F
            );
            if (var17.exact() || var13) {
               EspBoxRenderer.tracer(
                  bufferSource,
                  poseStack,
                  vec3,
                  var14,
                  (var17.x1() + var17.x2()) * 0.5,
                  (var17.y1() + var17.y2()) * 0.5,
                  (var17.z1() + var17.z2()) * 0.5,
                  Colors.withAlpha(var18, 255),
                  1.2F
               );
            }
         }

         for (Entity var21 : var5.entitiesForRendering()) {
            if (var21 instanceof ItemEntity) {
               ItemEntity var22 = (ItemEntity)var21;
               if (var22.getItem().is(Items.ANCIENT_DEBRIS)) {
                  AABB var23 = var22.getBoundingBox();
                  if (!isInsideOrNear(var4, vec3, var23.minX, var23.minY, var23.minZ, var23.maxX, var23.maxY, var23.maxZ, true)) {
                     EspBoxRenderer.fill(bufferSource, poseStack, vec3, var23.minX, var23.minY, var23.minZ, var23.maxX, var23.maxY, var23.maxZ, var11);
                  }

                  EspBoxRenderer.outline(
                     bufferSource, poseStack, vec3, var23.minX, var23.minY, var23.minZ, var23.maxX, var23.maxY, var23.maxZ, Colors.withAlpha(var10, 255), 2.0F
                  );
                  EspBoxRenderer.tracer(bufferSource, poseStack, vec3, var14, var22.getX(), var22.getY(), var22.getZ(), Colors.withAlpha(var10, 255), 1.2F);
               }
            }
         }

         EspBoxRenderer.flush(bufferSource);
      }
   }

   private static boolean isInsideOrNear(Minecraft minecraft, Vec3 vec3, double d, double e, double f, double g, double h, double i, boolean bl) {
      double var15 = bl ? 1.35 : 0.05;
      double var17 = d - var15;
      double var19 = e - var15;
      double var21 = f - var15;
      double var23 = g + var15;
      double var25 = h + var15;
      double var27 = i + var15;
      if (minecraft.player != null) {
         AABB var29 = minecraft.player.getBoundingBox();
         if (var29.maxX >= var17 && var29.minX <= var23 && var29.maxY >= var19 && var29.minY <= var25 && var29.maxZ >= var21 && var29.minZ <= var27) {
            return true;
         }
      }

      return vec3.x >= var17 && vec3.x <= var23 && vec3.y >= var19 && vec3.y <= var25 && vec3.z >= var21 && vec3.z <= var27;
   }
}
