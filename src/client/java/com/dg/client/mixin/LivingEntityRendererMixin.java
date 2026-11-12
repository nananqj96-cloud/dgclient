package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.fun.HeadAnimationsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "shouldFlipUpsideDown(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void dgclient$onShouldFlipUpsideDown(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady() && entity == MinecraftClient.getInstance().player) {
            HeadAnimationsModule anim = inst.getModuleManager().get(HeadAnimationsModule.class);
            if (anim != null && anim.shouldFlipSelfUpsideDown()) {
                cir.setReturnValue(true);
            }
        }
    }
}
