package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.TimeChangerModule;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void dgclient$onGetTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (!(((Object) this) instanceof ClientWorld)) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        TimeChangerModule tc = inst.getModuleManager().get(TimeChangerModule.class);
        if (tc != null && tc.isEnabled()) {
            cir.setReturnValue(tc.getClientTime());
        }
    }

    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
    private void dgclient$onGetRainGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!(((Object) this) instanceof ClientWorld)) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            WeatherParticleReducerModule mod = inst.getModuleManager().get(WeatherParticleReducerModule.class);
            if (mod != null && mod.isEnabled() && mod.disableWeather.getValue()) {
                cir.setReturnValue(0.0f);
            }
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
    private void dgclient$onGetThunderGradient(float delta, CallbackInfoReturnable<Float> cir) {
        if (!(((Object) this) instanceof ClientWorld)) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            WeatherParticleReducerModule mod = inst.getModuleManager().get(WeatherParticleReducerModule.class);
            if (mod != null && mod.isEnabled() && mod.disableWeather.getValue()) {
                cir.setReturnValue(0.0f);
            }
        }
    }
}
