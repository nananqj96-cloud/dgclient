package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.SkyFogModule;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"))
    private void dgclient$onGetFogColor(
            Camera camera,
            float tickProgress,
            ClientWorld world,
            int viewDistance,
            float skyDarkness,
            CallbackInfoReturnable<Vector4f> cir
    ) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        SkyFogModule mod = inst.getModuleManager().get(SkyFogModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        Vector4f fog = cir.getReturnValue();
        if (fog == null) {
            return;
        }
        int col = mod.getActiveFogColor();
        float d = mod.density.getValue().floatValue();
        fog.x = MathUtil.lerp(fog.x, ColorUtil.getRed(col) / 255.0f, d);
        fog.y = MathUtil.lerp(fog.y, ColorUtil.getGreen(col) / 255.0f, d);
        fog.z = MathUtil.lerp(fog.z, ColorUtil.getBlue(col) / 255.0f, d);
    }

    @ModifyArgs(
            method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void dgclient$modifyFogRanges(Args args) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        SkyFogModule mod = inst.getModuleManager().get(SkyFogModule.class);
        if (mod == null || !mod.isEnabled()) {
            return;
        }
        if (mod.preset.getValue() == SkyFogModule.Preset.CLEAR_FOG) {
            args.set(3, 1_000_000.0f);
            args.set(4, 2_000_000.0f);
            args.set(5, 1_000_000.0f);
            args.set(6, 2_000_000.0f);
            return;
        }
        float envEnd = args.get(4);
        float rdEnd = args.get(6);
        float baseDist = Math.max(32.0f, Math.max(envEnd, rdEnd));
        float start = baseDist * mod.fogStartRatio.getValue().floatValue();
        float end = Math.max(start + 8.0f, baseDist * mod.fogEndRatio.getValue().floatValue());
        args.set(3, start);
        args.set(4, end);
        args.set(5, start);
        args.set(6, end);
    }
}
