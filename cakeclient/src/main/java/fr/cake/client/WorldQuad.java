package fr.cake.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.nio.*;
import java.util.*;

/** One immutable unit quad. Only the small transform/hover uniform changes per frame. */
public final class WorldQuad {
 private static final RenderPipeline PANEL=pipeline(false),LYRIC=pipeline(true);
 private static RenderPipeline pipeline(boolean overlay){return RenderPipeline.builder()
  .withLocation(id(overlay?"pipeline/lyric_panel":"pipeline/world_panel")).withVertexShader(id("core/world_panel")).withFragmentShader(id("core/world_panel"))
  .withSampler("CardSampler").withUniform("CardData",UniformType.UNIFORM_BUFFER)
  .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.TRIANGLES)
  .withDepthTestFunction(overlay?DepthTestFunction.NO_DEPTH_TEST:DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();}
 private static GpuBuffer vertices;
 private static final Map<SmoothTexture,MappableRingBuffer> UNIFORMS=new IdentityHashMap<>();
 private static final ByteBuffer DATA=ByteBuffer.allocateDirect(112).order(ByteOrder.nativeOrder());
 private static Identifier id(String path){return Identifier.fromNamespaceAndPath("cakeclient",path);}
 public static void draw(SmoothTexture texture,Vec3 center,Vec3 right,Vec3 up,double width,double height,Vec3 camera,Matrix4f matrix,float opacity,float progress,boolean lyric){
  if(vertices==null){
   ByteBuffer v=ByteBuffer.allocateDirect(144).order(ByteOrder.nativeOrder());
   vertex(v,-.5f,.5f,0,0);vertex(v,-.5f,-.5f,0,1);vertex(v,.5f,-.5f,1,1);
   vertex(v,-.5f,.5f,0,0);vertex(v,.5f,-.5f,1,1);vertex(v,.5f,.5f,1,0);v.flip();
   vertices=RenderSystem.getDevice().createBuffer(()->"Cake / Immutable panel quad",GpuBuffer.USAGE_VERTEX,v);
  }
  var ring=UNIFORMS.computeIfAbsent(texture,t->new MappableRingBuffer(()->"Cake / Panel transform",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST|GpuBuffer.USAGE_MAP_WRITE,112));
  Vec3 c=center.subtract(camera),r=right.scale(width),u=up.scale(height);
  Matrix4f local=new Matrix4f((float)r.x,(float)r.y,(float)r.z,0,(float)u.x,(float)u.y,(float)u.z,0,0,0,1,0,(float)c.x,(float)c.y,(float)c.z,1);
  DATA.clear();new Matrix4f(matrix).mul(local).get(0,DATA);DATA.position(64);
  DATA.putFloat(opacity).putFloat(progress).putFloat(lyric?1:0).putFloat(0);
  DATA.putFloat(.73f).putFloat(.58f).putFloat(1).putFloat(1);
  UiCanvas.Hover hover=lyric?null:ClientScreen.canvas.hoverBounds();
  if(hover==null)DATA.putFloat(0).putFloat(0).putFloat(0).putFloat(0);
  else DATA.putFloat((float)hover.x()).putFloat((float)hover.y()).putFloat((float)hover.w()).putFloat((float)hover.h());
  DATA.flip();var uniform=ring.currentBuffer();var encoder=RenderSystem.getDevice().createCommandEncoder();encoder.writeToBuffer(uniform.slice(),DATA);
  var target=Minecraft.getInstance().getMainRenderTarget();
  try(var pass=encoder.createRenderPass(()->"Cake / Cached spatial panel",target.getColorTextureView(),OptionalInt.empty(),target.getDepthTextureView(),OptionalDouble.empty())){
   pass.setPipeline(lyric?LYRIC:PANEL);pass.setVertexBuffer(0,vertices);pass.setUniform("CardData",uniform);
   pass.bindTexture("CardSampler",texture.getTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));pass.draw(0,6);
  }
  ring.rotate();
 }
 private static void vertex(ByteBuffer v,float x,float y,float u,float t){v.putFloat(x).putFloat(y).putFloat(0).putFloat(u).putFloat(t).putInt(0xffffffff);}
 public static void release(SmoothTexture texture){var ring=UNIFORMS.remove(texture);if(ring!=null)ring.close();}
 public static void close(){if(vertices!=null){vertices.close();vertices=null;}for(var ring:UNIFORMS.values())ring.close();UNIFORMS.clear();}
}
