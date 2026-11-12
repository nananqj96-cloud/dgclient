package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.NametagsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true, require = 0)
    private void dgclient$onRenderLabel(
            S state,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            CameraRenderState cameraRenderState,
            CallbackInfo ci
    ) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && playerState.id == mc.player.getId()) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            NametagsModule nt = inst.getModuleManager().get(NametagsModule.class);
            if (nt != null && nt.isEnabled()) {
                ci.cancel();
            }
        }
    }
}
