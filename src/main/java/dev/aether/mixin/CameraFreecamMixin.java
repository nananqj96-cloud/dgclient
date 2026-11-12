package dev.aether.mixin;

import dev.aether.module.impl.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraFreecamMixin {
   @Shadow
   protected abstract void setPosition(Vec3 vec3);

   @Shadow
   protected abstract void setRotation(float f, float g);

   @Inject(
      method = {"setup"},
      at = {@At("TAIL")}
   )
   private void aetherclient$freecam(Level level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
      FreecamModule freecam = FreecamModule.get();
      if (freecam != null && freecam.isActive()) {
         this.setRotation(freecam.getInterpolatedYaw(partialTick), freecam.getInterpolatedPitch(partialTick));
         this.setPosition(freecam.getInterpolatedPos(partialTick));
      }
   }
}
