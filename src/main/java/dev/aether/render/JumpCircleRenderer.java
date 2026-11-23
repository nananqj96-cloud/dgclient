package dev.aether.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.world.phys.Vec3;

public final class JumpCircleRenderer {

    private JumpCircleRenderer() {
    }

    public static void renderDecal(
        PoseStack poseStack,
        BufferSource bufferSource,
        Vec3 cam,
        long now,
        float lifetime,
        float size,
        int baseRgb
    ) {
        // Jump circle rendering stub - core ring drawing functionality
        // preserved as renderer registration; detailed rendering disabled
    }
}
