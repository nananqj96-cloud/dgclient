package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.CustomCrosshairModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void dgclient$onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        CustomCrosshairModule mod = inst.getModuleManager().get(CustomCrosshairModule.class);
        if (mod != null && mod.isEnabled()) {
            ci.cancel();
        }
    }
}
