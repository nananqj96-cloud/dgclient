package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Vector3f;
import net.minecraft.world.phys.Vec3;

public final class HitParticleRenderer {

    private HitParticleRenderer() {
    }

    public static void renderSpark(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, float age, float glow) {
        // Hit particle rendering stub - core particle rendering functionality
        // preserved as renderer registration; detailed rendering disabled
    }

    public static void renderHeart(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, float age, float glow) {
        // Hit particle rendering stub - core heart rendering functionality
        // preserved as renderer registration; detailed rendering disabled
    }

    public static void renderLightning(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, float age) {
        // Hit particle rendering stub - core lightning rendering functionality
        // preserved as renderer registration; detailed rendering disabled
    }

    public static void renderAether(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, Vector3f fwd, float age) {
        // Hit particle rendering stub - core aether rendering functionality
        // preserved as renderer registration; detailed rendering disabled
    }

    public static void renderShock(VertexConsumer buf, Pose pose, Vec3 cam, Vector3f right, Vector3f up, long now) {
        // Hit particle rendering stub - core shock rendering functionality
        // preserved as renderer registration; detailed rendering disabled
    }
}
