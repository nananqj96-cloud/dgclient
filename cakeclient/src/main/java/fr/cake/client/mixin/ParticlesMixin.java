package fr.cake.client.mixin;
import net.minecraft.client.particle.ParticleEngine;
import fr.cake.client.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ParticleEngine.class)
public abstract class ParticlesMixin {
 @Inject(method="clearParticles",at=@At("TAIL")) private void cake$reset(CallbackInfo ci){Fireflies.reset();Ambience.reset();}
}
