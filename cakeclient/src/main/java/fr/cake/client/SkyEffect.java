package fr.cake.client;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import java.nio.*;
import java.util.OptionalInt;
public final class SkyEffect {
 private static Identifier id(String p){return Identifier.fromNamespaceAndPath("cakeclient",p);}
 private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
  .withLocation(id("pipeline/sky")).withVertexShader(Identifier.withDefaultNamespace("core/screenquad")).withFragmentShader(id("core/sky"))
  .withSampler("SceneSampler").withSampler("DepthSampler").withUniform("SkyData",UniformType.UNIFORM_BUFFER).build();
 private static TextureTarget swap;
 private static GpuBuffer buffer;
 private static final ByteBuffer DATA=ByteBuffer.allocateDirect(96).order(ByteOrder.nativeOrder());
 private static long retry;
 private static final long START=System.nanoTime();
 public static void render(Matrix4f matrix){if(RealisticMode.active())return;
  var mc=Minecraft.getInstance();var s=CakeClient.settings;
  if(!s.sky||s.skyIntensity<=0||mc.level==null)return;
  if(System.nanoTime()<retry)return;
  try{
   var target=mc.getMainRenderTarget();
   if(swap==null||swap.width!=target.width||swap.height!=target.height){close();swap=new TextureTarget("Cake / Sky composite",target.width,target.height,false);buffer=RenderSystem.getDevice().createBuffer(()->"Cake / Sky uniforms",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,96);}
   DATA.clear();new Matrix4f(matrix).invert().get(0,DATA);DATA.position(64);
   int col=java.awt.Color.HSBtoRGB((float)s.skyHue,.6f,.85f);
   DATA.putFloat(((col>>16)&255)/255f).putFloat(((col>>8)&255)/255f).putFloat((col&255)/255f).putFloat((float)s.skyIntensity);
   DATA.putFloat(s.skyMode).putFloat((System.nanoTime()-START)/1e9f).putFloat((float)s.skySpeed).putFloat((float)s.stars);DATA.flip();
   var encoder=RenderSystem.getDevice().createCommandEncoder();encoder.writeToBuffer(buffer.slice(),DATA);
   try(var pass=encoder.createRenderPass(()->"Cake / Procedural sky",swap.getColorTextureView(),OptionalInt.empty())){
    pass.setPipeline(PIPELINE);pass.setUniform("SkyData",buffer);
    pass.bindTexture("SceneSampler",target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
    pass.bindTexture("DepthSampler",target.getDepthTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));pass.draw(0,3);
   }
   encoder.copyTextureToTexture(swap.getColorTexture(),target.getColorTexture(),0,0,0,0,0,target.width,target.height);
  }catch(RuntimeException e){close();retry=System.nanoTime()+3_000_000_000L;CakeClient.LOG.error("Could not render the custom sky",e);}
 }
 public static void close(){if(swap!=null){swap.destroyBuffers();swap=null;}if(buffer!=null){buffer.close();buffer=null;}retry=0;}
}
