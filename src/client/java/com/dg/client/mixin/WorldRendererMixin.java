package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.BlockHighlightModule;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
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
            Entity entity,
            double cameraX,
            double cameraY,
            double cameraZ,
            BlockPos pos,
            BlockState state,
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
    private void dgclient$onRenderWeather(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            WeatherParticleReducerModule mod = inst.getModuleManager().get(WeatherParticleReducerModule.class);
            if (mod != null && mod.isEnabled() && mod.disableWeather.getValue()) {
                ci.cancel();
            }
        }
    }
}
