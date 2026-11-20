package fr.cake.client;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.nio.*;
import java.util.*;
final class InsectBatch {
 private static Identifier id(String p){return Identifier.fromNamespaceAndPath("cakeclient",p);}
 private static final RenderPipeline PIPELINE=RenderPipeline.builder().withLocation(id("pipeline/insects")).withVertexShader(id("core/insects")).withFragmentShader(id("core/insects"))
 .withSampler("InsectSampler").withUniform("InsectData",UniformType.UNIFORM_BUFFER).withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.TRIANGLES)
 .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();
 private static GpuBuffer vertex,uniform;
 private static final ByteBuffer V=ByteBuffer.allocateDirect(256*144).order(ByteOrder.nativeOrder()),U=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder());
 private static Vec3 eye,right,up;private static int count;
 static void begin(Camera camera,Matrix4f matrix){
  eye=camera.position();Vector3f r=new Vector3f(1,0,0).rotate(camera.rotation()),u=new Vector3f(0,1,0).rotate(camera.rotation());right=new Vec3(r.x,r.y,r.z);up=new Vec3(u.x,u.y,u.z);count=0;V.clear();U.clear();matrix.get(0,U);U.limit(64);U.position(0);
 }
 static void add(Vec3 position,double size,float angle,int rgb,float alpha,TextureAtlasSprite sprite){
  if(count>=256)return;count++;Vec3 p=position.subtract(eye),r=right.scale(Math.cos(angle)*size).add(up.scale(Math.sin(angle)*size)),u=up.scale(Math.cos(angle)*size).subtract(right.scale(Math.sin(angle)*size));
  int color=(Math.clamp((int)(alpha*255),0,255)<<24)|rgb;
  put(p.subtract(r).add(u),sprite.getU0(),sprite.getV0(),color);put(p.subtract(r).subtract(u),sprite.getU0(),sprite.getV1(),color);put(p.add(r).subtract(u),sprite.getU1(),sprite.getV1(),color);
  put(p.subtract(r).add(u),sprite.getU0(),sprite.getV0(),color);put(p.add(r).subtract(u),sprite.getU1(),sprite.getV1(),color);put(p.add(r).add(u),sprite.getU1(),sprite.getV0(),color);
 }
 static void ribbon(Vec3 start,Vec3 end,double width,int rgb,float a,float b,TextureAtlasSprite sprite){
  if(count>=256)return;Vec3 direction=end.subtract(start),toward=eye.subtract(start),side=direction.cross(toward);if(side.lengthSqr()<.000001)return;side=side.normalize().scale(width);count++;
  Vec3 p=start.subtract(eye),q=end.subtract(eye);int ca=(Math.clamp((int)(a*255),0,255)<<24)|rgb,cb=(Math.clamp((int)(b*255),0,255)<<24)|rgb;float u0=sprite.getU0(),u1=sprite.getU1(),v=(sprite.getV0()+sprite.getV1())/2;
  put(p.subtract(side),u0,v,ca);put(p.add(side),u1,v,ca);put(q.add(side),u1,v,cb);put(p.subtract(side),u0,v,ca);put(q.add(side),u1,v,cb);put(q.subtract(side),u0,v,cb);
 }
 private static void put(Vec3 p,float u,float v,int c){V.putFloat((float)p.x).putFloat((float)p.y).putFloat((float)p.z).putFloat(u).putFloat(v).put((byte)(c>>16)).put((byte)(c>>8)).put((byte)c).put((byte)(c>>>24));}
 static void end(GpuTextureView atlas){
  if(count==0)return;V.flip();if(vertex==null){vertex=RenderSystem.getDevice().createBuffer(()->"Cake / Insect batch",GpuBuffer.USAGE_VERTEX|GpuBuffer.USAGE_COPY_DST,256*144);uniform=RenderSystem.getDevice().createBuffer(()->"Cake / Insect matrix",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,64);}
  var encoder=RenderSystem.getDevice().createCommandEncoder();encoder.writeToBuffer(vertex.slice(0,V.remaining()),V);encoder.writeToBuffer(uniform.slice(),U);var target=Minecraft.getInstance().getMainRenderTarget();
  try(var pass=encoder.createRenderPass(()->"Cake / Sharp fireflies",target.getColorTextureView(),OptionalInt.empty(),target.getDepthTextureView(),OptionalDouble.empty())){pass.setPipeline(PIPELINE);pass.setVertexBuffer(0,vertex);pass.setUniform("InsectData",uniform);pass.bindTexture("InsectSampler",atlas,RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(0,count*6);}
 }
 static void close(){if(vertex!=null){vertex.close();vertex=null;}if(uniform!=null){uniform.close();uniform=null;}}
}
