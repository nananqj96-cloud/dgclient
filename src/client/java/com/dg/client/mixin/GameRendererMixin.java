package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void dgclient$modifyFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        ZoomModule zoom = inst.getModuleManager().get(ZoomModule.class);
        if (zoom != null) {
            cir.setReturnValue((float) zoom.modifyFov(cir.getReturnValueF()));
        }
    }
}
