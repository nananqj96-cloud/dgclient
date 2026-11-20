package fr.cake.client.mixin;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fr.cake.client.*;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
 @Inject(method="renderLevel",at=@At("TAIL"))
 private void cake$render(GraphicsResourceAllocator allocator,DeltaTracker delta,boolean outlines,Camera camera,Matrix4f view,Matrix4f projection,Matrix4f culling,GpuBufferSlice fog,Vector4f color,boolean sky,CallbackInfo ci){
  Matrix4f matrix=new Matrix4f(projection).mul(view);
  CosmeticFx.render(camera,matrix);SkyEffect.render(matrix);Atmosphere.render();MotionBlur.render();Fireflies.render(camera,matrix,delta.getGameTimeDeltaPartialTick(true));Comets.render(camera,matrix);SpatialPanel.render(camera,matrix);LyricsSky.render(camera,matrix);SpotifyVisualizer.render();
 }
}
