package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Note: Block outline suppression is handled via Fabric API's
 * {@code WorldRenderEvents.BEFORE_BLOCK_OUTLINE} in {@link DGClient} rather than
 * injecting into {@code WorldRenderer#drawBlockOutline}, avoiding conflicts with Sodium.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true, require = 0)
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
