package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.aether.module.impl.WaypointsModule;
import dev.aether.util.Colors;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Renders placed waypoints as glowing markers in the world, with optional labels.
 */
public final class WaypointsRenderer {
   private WaypointsRenderer() {
   }

   public static void render(BufferSource bufferSource, PoseStack poseStack, Vec3 cam, WaypointsModule module) {
      if (module.waypoints().isEmpty()) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      Camera camera = mc.gameRenderer.getMainCamera();
      Vector3f fwd = new Vector3f(camera.forwardVector());
      Vector3f right = new Vector3f();
      fwd.cross(new Vector3f(0.0F, 1.0F, 0.0F), right);
      if (right.lengthSquared() < 1.0E-6F) {
         right.set(1.0F, 0.0F, 0.0F);
      }
      right.normalize();
      Vector3f up = new Vector3f();
      right.cross(fwd, up);
      up.normalize();

      Pose pose = poseStack.last();
      VertexConsumer fill = bufferSource.getBuffer(FlatOverlay.FILL);
      VertexConsumer lines = bufferSource.getBuffer(FlatOverlay.LINES);

      for (WaypointsModule.Waypoint wp : module.waypoints()) {
         float ax = (float) (wp.x() - cam.x());
         float ay = (float) (wp.y() - cam.y());
         float az = (float) (wp.z() - cam.z());
         int rgb = wp.color();

         // Glow halo.
         int halo = Colors.withAlpha(rgb, 0.25F);
         quad(fill, pose, ax, ay, az, right, up, 2.5F, halo);
         // Core marker.
         int core = Colors.withAlpha(rgb, 1.0F);
         quad(fill, pose, ax, ay, az, right, up, 1.0F, core);
         // Cross lines.
         line3(lines, pose, ax - 2.0F, ay, az, ax + 2.0F, ay, az, core, 1.5F);
         line3(lines, pose, ax, ay - 2.0F, az, ax, ay + 2.0F, az, core, 1.5F);
      }

      FlatOverlay.flush(bufferSource);
   }

   private static void quad(VertexConsumer buf, Pose pose, float bx, float by, float bz, Vector3f right, Vector3f up, float half, int argb) {
      float h = Math.max(0.05F, half);
      buf.addVertex(pose, bx - right.x * h - up.x * h, by - right.y * h - up.y * h, bz - right.z * h - up.z * h).setColor(argb);
      buf.addVertex(pose, bx + right.x * h - up.x * h, by + right.y * h - up.y * h, bz + right.z * h - up.z * h).setColor(argb);
      buf.addVertex(pose, bx + right.x * h + up.x * h, by + right.y * h + up.y * h, bz + right.z * h + up.z * h).setColor(argb);
      buf.addVertex(pose, bx - right.x * h + up.x * h, by - right.y * h + up.y * h, bz - right.z * h + up.z * h).setColor(argb);
   }

   private static void line3(VertexConsumer buf, Pose pose, float x1, float y1, float z1, float x2, float y2, float z2, int argb, float width) {
      Vector3f n = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
      if (n.lengthSquared() > 1.0E-9F) {
         n.normalize();
      } else {
         n.set(0.0F, 1.0F, 0.0F);
      }
      buf.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, n).setLineWidth(width);
      buf.addVertex(pose, x2, y2, z2).setColor(argb).setNormal(pose, n).setLineWidth(width);
   }
}
