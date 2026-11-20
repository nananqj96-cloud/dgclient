package fr.cake.client.mixin;
import fr.cake.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(FogRenderer.class)
public abstract class FogMixin {
 @Unique private Camera cake$camera;
 @Inject(method="setupFog",at=@At("HEAD"))
 private void cake$camera(Camera camera,int distance,net.minecraft.client.DeltaTracker delta,float sky,net.minecraft.client.multiplayer.ClientLevel level,CallbackInfoReturnable<Vector4f> ci){cake$camera=camera;}
 @ModifyArgs(method="setupFog",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
 private void cake$fog(Args args){
  var s=CakeClient.settings;if(s==null||!s.fogEnabled||cake$camera==null)return;
  FogType fluid=cake$camera.getFluidInCamera();if(fluid!=FogType.NONE&&fluid!=FogType.ATMOSPHERIC)return;
  if(cake$camera.entity() instanceof net.minecraft.world.entity.LivingEntity living&&(living.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)||living.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)))return;
  float vanillaEnd=args.get(6);var range=FogMath.range(FogMath.distance(s.fogDistance),s.fogStart,s.fogStrength,vanillaEnd);
  args.set(3,Math.min((float)args.get(3),range.start()));args.set(4,Math.min((float)args.get(4),range.end()));
  args.set(5,range.start());args.set(6,range.end());
  if(s.fogSky){args.set(7,Math.min((float)args.get(7),range.end()));args.set(8,Math.min((float)args.get(8),range.end()));}
  Vector4f original=args.get(2),color=new Vector4f(original);
  int rgb=java.awt.Color.HSBtoRGB((float)s.fogHue,(float)s.fogSaturation,(float)s.fogBrightness);
  float mix=(float)s.fogTint;color.x+=( ((rgb>>16)&255)/255f-color.x)*mix;color.y+=(((rgb>>8)&255)/255f-color.y)*mix;color.z+=((rgb&255)/255f-color.z)*mix;color.w*=(float)s.fogOpacity;if(s.fogSky)original.set(color);args.set(2,color);
 }
}
