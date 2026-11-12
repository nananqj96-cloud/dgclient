package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.HandItemGlowModule;
import com.dg.client.render.TintingVertexConsumerProvider;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow
    public static void renderItem(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer layer,
            ItemRenderState.Glint glint
    ) {
    }

    @Unique
    private static boolean dgclient$renderingHandGlow = false;

    @Inject(method = "renderItem", at = @At("TAIL"))
    private static void dgclient$afterRenderItem(
            ItemDisplayContext displayContext,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            int[] tints,
            List<BakedQuad> quads,
            RenderLayer layer,
            ItemRenderState.Glint glint,
            CallbackInfo ci
    ) {
        if (dgclient$renderingHandGlow || quads == null || quads.isEmpty()) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        HandItemGlowModule glow = inst.getModuleManager().get(HandItemGlowModule.class);
        if (glow == null || !glow.isEnabled()) {
            return;
        }
        boolean isFirst = displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean isThird = displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        if ((isFirst && !glow.firstPerson.getValue()) || (isThird && !glow.thirdPerson.getValue()) || (!isFirst && !isThird)) {
            return;
        }

        dgclient$renderingHandGlow = true;
        try {
            int col = glow.computeCurrentGlowColor();
            float mult = glow.intensity.getValue().floatValue();

            // Inner neon aura shell
            matrices.push();
            float s1 = 1.0f + 0.038f * Math.min(2.0f, mult);
            matrices.scale(s1, s1, s1);
            renderItem(displayContext, matrices, new TintingVertexConsumerProvider(vertexConsumers, col),
                    0xF000F0, overlay, tints, quads, layer, ItemRenderState.Glint.NONE);
            matrices.pop();

            // Outer soft halo shell
            matrices.push();
            float s2 = 1.0f + 0.078f * Math.min(2.0f, mult);
            matrices.scale(s2, s2, s2);
            int outerCol = ColorUtil.scaleAlpha(col, 0.48f);
            renderItem(displayContext, matrices, new TintingVertexConsumerProvider(vertexConsumers, outerCol),
                    0xF000F0, overlay, tints, quads, layer, ItemRenderState.Glint.NONE);
            matrices.pop();
        } finally {
            dgclient$renderingHandGlow = false;
        }
    }
}
