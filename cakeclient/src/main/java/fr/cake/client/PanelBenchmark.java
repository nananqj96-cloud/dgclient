package fr.cake.client;

import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.util.*;

/** Optional deterministic profiling harness. Inactive unless an explicit JVM property is supplied. */
public final class PanelBenchmark {
 private static final String PATH=System.getProperty("cakeclient.panelBenchmark","");
 private static final String[] NAMES={"warmup","hidden","panel_still","panel_scan","hidden_scan","hidden_repeat"};
 private static final List<List<Double>> FRAMES=new ArrayList<>();
 private static long start,last,paintBase;private static int phase=-1;private static float yaw,pitch;
 private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
 public static void frame(){
  if(PATH.isEmpty())return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  long now=System.nanoTime();if(start==0){start=now;yaw=mc.player.getYRot();pitch=mc.player.getXRot();for(int i=0;i<NAMES.length;i++)FRAMES.add(new ArrayList<>());}
  int next=(int)((now-start)/8_000_000_000L);
  if(next!=phase){
   if(phase>=0){Map<String,Object> row=new LinkedHashMap<>();var values=FRAMES.get(phase);Collections.sort(values);row.put("phase",NAMES[phase]);row.put("frames",values.size());row.put("median_ms",values.isEmpty()?0:values.get(values.size()/2));row.put("p95_ms",values.isEmpty()?0:values.get((int)((values.size()-1)*.95)));row.put("canvas_paints",ClientScreen.canvas.paintCount()-paintBase);RESULTS.add(row);}
   if(next>=NAMES.length){try{Files.writeString(Path.of(PATH),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(RESULTS));}catch(Exception e){CakeClient.LOG.error("Benchmark output",e);}mc.stop();return;}
   phase=next;paintBase=ClientScreen.canvas.paintCount();if(phase==2)SpatialPanel.pin();if(phase==4)SpatialPanel.hide();
  }
  double elapsed=(now-start)/1e9-phase*8;
  if(last!=0&&elapsed>2)FRAMES.get(phase).add((now-last)/1e6);last=now;
  if(phase==3||phase==4){double t=elapsed;mc.player.setYRot(yaw+(float)Math.sin(t*2.2)*24);mc.player.setXRot(pitch+(float)Math.cos(t*1.7)*16);}else{mc.player.setYRot(yaw);mc.player.setXRot(pitch);}
 }
}
