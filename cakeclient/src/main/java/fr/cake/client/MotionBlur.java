package fr.cake.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.phys.Vec3;
import java.util.OptionalInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Persistent, full-resolution GPU frame accumulation, executed before the HUD. */
public final class MotionBlur {
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
        .withLocation(net.minecraft.resources.Identifier.fromNamespaceAndPath("cakeclient","pipeline/motion"))
        .withVertexShader(net.minecraft.resources.Identifier.withDefaultNamespace("core/screenquad"))
        .withFragmentShader(net.minecraft.resources.Identifier.fromNamespaceAndPath("cakeclient","core/motion"))
        .withSampler("CurrentSampler").withSampler("HistorySampler")
        .withUniform("CakeConfig",UniformType.UNIFORM_BUFFER).build();
    private static TextureTarget history,output;
    private static GpuBuffer config;
    private static final ByteBuffer DATA=ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
    private static boolean primed,failed;
    private static Object world;
    private static Vec3 position;
    private static long last;
    public static void reset(){primed=false;last=0;failed=false;}
    public static void render() {
        Minecraft mc=Minecraft.getInstance();
        Settings s=CakeClient.settings;
        if(s==null||!s.motionBlur||s.blur<=.001||mc.level==null||mc.player==null||mc.screen!=null||mc.isPaused()){
            primed=false;last=0;return;
        }
        if(failed)return;
        try {
            RenderTarget main=mc.getMainRenderTarget();
            if(main.width<=0||main.height<=0)return;
            if(history==null||history.width!=main.width||history.height!=main.height){
                close();
                history=new TextureTarget("Cake / History",main.width,main.height,false);
                output=new TextureTarget("Cake / Composite",main.width,main.height,false);
                config=RenderSystem.getDevice().createBuffer(()->"Cake / Temporal config",GpuBuffer.USAGE_UNIFORM|GpuBuffer.USAGE_COPY_DST,16);
            }
            Vec3 now=mc.player.position();
            if(world!=mc.level||position==null||position.distanceToSqr(now)>256)primed=false;
            world=mc.level;position=now;
            long nanos=System.nanoTime();
            double dt=last==0?1/60.0:Math.clamp((nanos-last)/1e9,1/1000.,.25);
            if(last!=0&&nanos-last>250_000_000L)primed=false;
            last=nanos;
            // Time-corrected persistence gives the same exposure at 60, 144 or 240 Hz.
            float weight=primed?(float)Math.pow(.05+s.blur*.85,dt*60):0;
            var encoder=RenderSystem.getDevice().createCommandEncoder();
            if(!primed)encoder.copyTextureToTexture(main.getColorTexture(),history.getColorTexture(),0,0,0,0,0,main.width,main.height);
            DATA.clear(); DATA.putFloat(weight).putFloat(0).putFloat(0).putFloat(0).flip();
            encoder.writeToBuffer(config.slice(),DATA);
            try(RenderPass pass=encoder.createRenderPass(()->"Cake / Motion Blur",output.getColorTextureView(),OptionalInt.empty())){
                pass.setPipeline(PIPELINE);
                var sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
                pass.bindTexture("CurrentSampler",main.getColorTextureView(),sampler);
                pass.bindTexture("HistorySampler",history.getColorTextureView(),sampler);
                pass.setUniform("CakeConfig",config);pass.draw(0,3);
            }
            encoder.copyTextureToTexture(output.getColorTexture(),history.getColorTexture(),0,0,0,0,0,main.width,main.height);
            encoder.copyTextureToTexture(output.getColorTexture(),main.getColorTexture(),0,0,0,0,0,main.width,main.height);
            primed=true;CakeClient.effectError="";
        }catch(RuntimeException e){
            CakeClient.LOG.error("Motion Blur disabled after a GPU error",e);
            close();failed=true;CakeClient.effectError=e.getClass().getSimpleName();
        }
    }
    public static void close(){
        if(history!=null){history.destroyBuffers();history=null;}
        if(output!=null){output.destroyBuffers();output=null;}
        if(config!=null){config.close();config=null;}
        primed=false;last=0;world=null;position=null;
    }
}
