package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.BlockHighlightModule;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.OutlineRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void dgclient$onDrawBlockOutline(
            MatrixStack matrices,
            VertexConsumer vertexConsumer,
            double cameraX,
            double cameraY,
            double cameraZ,
            OutlineRenderState state,
            int color,
            float lineWidth,
            CallbackInfo ci
    ) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            BlockHighlightModule bh = inst.getModuleManager().get(BlockHighlightModule.class);
            if (bh != null && bh.isEnabled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void dgclient$onRenderWeather(FrameGraphBuilder frameGraphBuilder, GpuBufferSlice fogBuffer, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            WeatherParticleReducerModule mod = inst.getModuleManager().get(WeatherParticleReducerModule.class);
            if (mod != null && mod.isEnabled() && mod.disableWeather.getValue()) {
                ci.cancel();
            }
        }
    }
}
