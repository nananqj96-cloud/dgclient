package fr.cake.client;
import net.minecraft.client.*;
import java.nio.file.*;
import java.util.*;
/** Optional reproducible in-game comparison. Disabled unless a test JVM property is set. */
public final class FpsBenchmark {
 private static final String OUTPUT=System.getProperty("cakeclient.fpsBenchmark");
 private static long began,last;private static int phase=-1;private static final List<Double> samples=new ArrayList<>();private static final List<Map<String,Object>> result=new ArrayList<>();
 public static void frame(){
  if(OUTPUT==null)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||mc.screen!=null)return;
  long now=System.nanoTime();if(phase<0){phase=0;began=now;var o=mc.options;o.renderDistance().set(12);CakeClient.settings.fpsRenderDistance=12;o.simulationDistance().set(8);o.entityDistanceScaling().set(1.);o.graphicsPreset().set(GraphicsPreset.FANCY);o.cloudStatus().set(CloudStatus.FANCY);o.ambientOcclusion().set(true);o.entityShadows().set(true);o.enableVsync().set(false);o.framerateLimit().set(260);mc.player.connection.sendCommand("difficulty peaceful");mc.player.connection.sendCommand("time set day");mc.player.setXRot(6);mc.player.setYRot(0);last=now;return;}
  double elapsed=(now-began)/1e9;if(last!=0&&elapsed>3&&(phase==1||phase==3))samples.add((now-last)/1e6);last=now;
  if(elapsed<12)return;
  if(phase==1||phase==3){Collections.sort(samples);var row=new LinkedHashMap<String,Object>();row.put("phase",phase==1?"baseline":"maximum");row.put("frames",samples.size());row.put("median_ms",samples.get(samples.size()/2));row.put("p95_ms",samples.get(Math.min(samples.size()-1,(int)(samples.size()*.95))));row.put("view_chunks",mc.options.renderDistance().get());row.put("simulation_chunks",mc.options.simulationDistance().get());row.put("framebuffer",mc.getMainRenderTarget().width+"x"+mc.getMainRenderTarget().height);result.add(row);samples.clear();}
  phase++;began=now;if(phase==2)CakeOptimize.preset(3);
  if(phase==4){try{Files.writeString(Path.of(OUTPUT),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));}catch(Exception e){CakeClient.LOG.error("FPS benchmark write failed",e);}mc.stop();}
 }
}
