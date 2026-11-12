package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.FullbrightModule;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
    @Shadow
    private boolean dirty;

    @Unique
    private boolean dgclient$lastGammaState = false;

    @Inject(method = "tick", at = @At("TAIL"))
    private void dgclient$onTick(CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        FullbrightModule fb = inst.getModuleManager().get(FullbrightModule.class);
        boolean active = fb != null && fb.isEnabled() && fb.mode.getValue() == FullbrightModule.Mode.GAMMA;
        if (active != dgclient$lastGammaState) {
            dgclient$lastGammaState = active;
            this.dirty = true;
        }
    }

    @Inject(method = "getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F", at = @At("RETURN"), cancellable = true)
    private static void dgclient$onGetBrightness(DimensionType type, int lightLevel, CallbackInfoReturnable<Float> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        FullbrightModule fb = inst.getModuleManager().get(FullbrightModule.class);
        if (fb != null && fb.isEnabled() && fb.mode.getValue() == FullbrightModule.Mode.GAMMA) {
            cir.setReturnValue(Math.max(cir.getReturnValueF(), Math.min(1.0f, fb.strength.getValue().floatValue())));
        }
    }

    @Inject(method = "getBrightness(FI)F", at = @At("RETURN"), cancellable = true)
    private static void dgclient$onGetBrightnessAmbient(float ambientLight, int lightLevel, CallbackInfoReturnable<Float> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        FullbrightModule fb = inst.getModuleManager().get(FullbrightModule.class);
        if (fb != null && fb.isEnabled() && fb.mode.getValue() == FullbrightModule.Mode.GAMMA) {
            cir.setReturnValue(Math.max(cir.getReturnValueF(), Math.min(1.0f, fb.strength.getValue().floatValue())));
        }
    }
}
