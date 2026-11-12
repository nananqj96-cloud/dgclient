package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.aether.module.impl.MobEspModule;
import dev.aether.module.impl.PlayerEspModule;
import dev.aether.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

public final class EntityEspRenderer {
   private static final float TRACER_WIDTH = 1.2F;

   private EntityEspRenderer() {
   }

   public static void renderPlayers(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, PlayerEspModule module) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null && mc.player != null) {
         int color = module.color.get();
         boolean glow = module.style.is("Glow");
         boolean tracers = module.tracers.get();
         Vector3fc forward = tracers ? mc.gameRenderer.getMainCamera().forwardVector() : null;

         for (AbstractClientPlayer player : level.players()) {
            if (player != mc.player && player.isAlive() && !player.isSpectator()) {
               box(bufferSource, poseStack, cam, player.getBoundingBox(), color, glow);
               if (tracers) {
                  tracer(bufferSource, poseStack, cam, forward, player.getBoundingBox(), color);
               }
            }
         }

         EspBoxRenderer.flush(bufferSource);
      }
   }

   public static void renderMobs(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, MobEspModule module) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level != null) {
         int hostileColor = module.hostile.get();
         int passiveColor = module.passive.get();
         boolean passiveToo = module.passiveToo.get();
         boolean tracers = module.tracers.get();
         Vector3fc forward = tracers ? mc.gameRenderer.getMainCamera().forwardVector() : null;

         for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Mob && entity.isAlive()) {
               boolean hostile = entity instanceof Enemy;
               if (hostile || passiveToo) {
                  int color = hostile ? hostileColor : passiveColor;
                  box(bufferSource, poseStack, cam, entity.getBoundingBox(), color, false);
                  if (tracers) {
                     tracer(bufferSource, poseStack, cam, forward, entity.getBoundingBox(), color);
                  }
               }
            }
         }

         EspBoxRenderer.flush(bufferSource);
      }
   }

   private static void box(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, AABB b, int color, boolean glow) {
      EspBoxRenderer.outline(bufferSource, poseStack, cam, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, color, 2.0F);
      if (glow) {
         EspBoxRenderer.fill(bufferSource, poseStack, cam, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, Colors.withAlpha(color, 0.18F));
      }
   }

   private static void tracer(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, Vector3fc forward, AABB b, int color) {
      EspBoxRenderer.tracer(
         bufferSource, poseStack, cam, forward, (b.minX + b.maxX) / 2.0, (b.minY + b.maxY) / 2.0, (b.minZ + b.maxZ) / 2.0, Colors.withAlpha(color, 0.72F), 1.2F
      );
   }
}
