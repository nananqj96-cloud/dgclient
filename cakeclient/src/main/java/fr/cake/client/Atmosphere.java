package fr.cake.client;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import java.nio.*;
import java.util.OptionalInt;
/** Native-resolution bloom, local depth occlusion and photographic color grading. */
public final class Atmosphere {
 private static Identifier id(String p){return Identifier.fromNamespaceAndPath("cakeclient",p);}
 private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
  .withLocation(id("pipeline/atmosphere")).withVertexShader(Identifier.withDefaultNamespace("core/screenquad")).withFragmentShader(id("core/atmosphere"))
  .withSampler("SceneSampler").withSampler("DepthSampler").withUniform("AtmosphereData",UniformType.UNIFORM_BUFFER).build();
 private static TextureTarget swap;private static GpuBuffer buffer;private static boolean failed;
 private static final ByteBuffer DATA=ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
 public static void render(){if(RealisticMode.active())return;
  var mc=Minecraft.getInstance();var s=CakeClient.settings;if(!s.atmosphere||mc.level==null||failed)return;
  try{
   var target=mc.getMainRenderTarget();
   if(swap==null||swap.width!=target.width||swap.height!=target.height){close();swap=new TextureTarget("Cake / Atmosphere",target.width,target.height,false);buffer=RenderSystem.getDevice().createBuffer(()->"Cake / Atmosphere uniforms",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,32);}
   DATA.clear();DATA.putFloat(target.width).putFloat(target.height).putFloat((float)s.bloom).putFloat((float)s.occlusion);
   DATA.putFloat((float)s.exposure).putFloat(s.atmosphereStyle+(float)s.ambientHue*.1f).putFloat((float)s.vignette).putFloat(0);DATA.flip();
   var encoder=RenderSystem.getDevice().createCommandEncoder();encoder.writeToBuffer(buffer.slice(),DATA);
   try(var pass=encoder.createRenderPass(()->"Cake / Full resolution atmosphere",swap.getColorTextureView(),OptionalInt.empty())){
    pass.setPipeline(PIPELINE);pass.setUniform("AtmosphereData",buffer);
    pass.bindTexture("SceneSampler",target.getColorTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
    pass.bindTexture("DepthSampler",target.getDepthTextureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));pass.draw(0,3);
   }
   encoder.copyTextureToTexture(swap.getColorTexture(),target.getColorTexture(),0,0,0,0,0,target.width,target.height);
  }catch(RuntimeException e){close();failed=true;CakeClient.LOG.error("Atmosphere rendering failed",e);}
 }
 public static void close(){if(swap!=null){swap.destroyBuffers();swap=null;}if(buffer!=null){buffer.close();buffer=null;}failed=false;}
}
