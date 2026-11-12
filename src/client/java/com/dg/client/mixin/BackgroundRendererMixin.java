package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.SkyFogModule;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Shadow
    private static float red;
    @Shadow
    private static float green;
    @Shadow
    private static float blue;

    @Inject(method = "render", at = @At("TAIL"))
    private static void dgclient$onRenderFogColor(Camera camera, float tickDelta, ClientWorld world, int viewDistance, float skyDarkness, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        SkyFogModule mod = inst.getModuleManager().get(SkyFogModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        int col = mod.getActiveFogColor();
        float d = mod.density.getValue().floatValue();
        red = MathUtil.lerp(red, ColorUtil.getRed(col) / 255.0f, d);
        green = MathUtil.lerp(green, ColorUtil.getGreen(col) / 255.0f, d);
        blue = MathUtil.lerp(blue, ColorUtil.getBlue(col) / 255.0f, d);
        RenderSystem.clearColor(red, green, blue, 0.0f);
    }

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void dgclient$onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        SkyFogModule mod = inst.getModuleManager().get(SkyFogModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        if (mod.preset.getValue() == SkyFogModule.Preset.CLEAR_FOG) {
            RenderSystem.setShaderFogStart(1_000_000.0f);
            RenderSystem.setShaderFogEnd(2_000_000.0f);
            return;
        }
        float start = viewDistance * mod.fogStartRatio.getValue().floatValue();
        float end = Math.max(start + 8.0f, viewDistance * mod.fogEndRatio.getValue().floatValue());
        RenderSystem.setShaderFogStart(start);
        RenderSystem.setShaderFogEnd(end);
    }
}
