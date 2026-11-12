package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.HandItemGlowModule;
import com.dg.client.render.TintingVertexConsumerProvider;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow
    public abstract void renderItem(
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model
    );

    @Unique
    private boolean dgclient$renderingHandGlow = false;

    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("TAIL")
    )
    private void dgclient$afterRenderItem(
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci
    ) {
        if (dgclient$renderingHandGlow || stack.isEmpty()) {
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
        boolean isFirst = renderMode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND
                || renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND;
        boolean isThird = renderMode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND
                || renderMode == ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
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
            renderItem(stack, renderMode, leftHanded, matrices,
                    new TintingVertexConsumerProvider(vertexConsumers, col), 0xF000F0, overlay, model);
            matrices.pop();

            // Outer soft halo shell
            matrices.push();
            float s2 = 1.0f + 0.078f * Math.min(2.0f, mult);
            matrices.scale(s2, s2, s2);
            int outerCol = ColorUtil.scaleAlpha(col, 0.48f);
            renderItem(stack, renderMode, leftHanded, matrices,
                    new TintingVertexConsumerProvider(vertexConsumers, outerCol), 0xF000F0, overlay, model);
            matrices.pop();
        } finally {
            dgclient$renderingHandGlow = false;
        }
    }
}
