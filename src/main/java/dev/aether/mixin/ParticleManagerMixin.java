package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.NoExplosionsModule;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.core.particles.ParticleEffect;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets No Explosions suppress explosion particle spawning and No Flame suppress fire particles.
 */
@Mixin({ParticleManager.class})
public class ParticleManagerMixin {
   @Inject(
      method = {"addParticle(Lnet/minecraft/core/particles/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aetherclient$suppressParticles(
      ParticleEffect effect,
      double x,
      double y,
      double z,
      double vx,
      double vy,
      double vz,
      CallbackInfoReturnable<net.minecraft.client.particle.Particle> cir
   ) {
      ModuleManager modules = AetherClient.modules();
      if (modules == null) {
         return;
      }
      // No Explosions: cancel explosion particles.
      if (modules.noExplosions != null && modules.noExplosions.isEnabled()) {
         if (effect != null
            && (effect.getType() == ParticleTypes.EXPLOSION || effect.getType() == ParticleTypes.EXPLOSION_EMITTER)) {
            cir.setReturnValue(null);
         }
      }
      // No Flame: cancel fire smoke/flame particles.
      if (modules.noFlame != null && modules.noFlame.isEnabled()) {
         if (effect != null && isFireParticle(effect)) {
            cir.setReturnValue(null);
         }
      }
   }

   private static boolean isFireParticle(ParticleEffect effect) {
      ParticleType<?> type = effect.getType();
      if (type == null) {
         return false;
      }
      String id = type.id().getPath();
      return id.equals("fire") || id.equals("flame") || id.equals("smoke")
         || id.equals("large_smoke") || id.equals("campfire_cosy_smoke")
         || id.equals("campfire_signal_smoke");
   }
}
