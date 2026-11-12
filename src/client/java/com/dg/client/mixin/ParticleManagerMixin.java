package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.WeatherParticleReducerModule;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void dgclient$onAddParticle(
            ParticleEffect parameters,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            WeatherParticleReducerModule mod = inst.getModuleManager().get(WeatherParticleReducerModule.class);
            if (mod != null && mod.shouldCancelParticle(parameters)) {
                cir.setReturnValue(null);
            }
        }
    }
}
