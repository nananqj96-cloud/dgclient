package fr.cake.client;
import com.google.gson.*;
import net.minecraft.client.*;
import net.minecraft.server.level.ParticleStatus;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Uses the real 1.21.11 options and callbacks. Never rewrites options behind the game. */
public final class CakeOptimize {
 private CakeOptimize(){}
 private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
 private static final String PACK="file/CakeClient-Potato-1.21.11.zip";
 private static final String[] EFFECTS={"motionBlur","atmosphere","sky","fireflies","sakura","motes","embers","snow","pollen","comets","trailEnabled","jumpEnabled"};
 private static State before;private static boolean initialized,dirty,loading;private static int lastDistance=-1;
 private static String status="Réglages appliqués en direct";
 public static String status(){return status;}
 private static Path backup(){return Settings.DIR.resolveSibling("cake-client-fps-before.json");}
 static final class State {
  int view,simulation,particles,weather,biome,cap;double entities;
  String clouds,preset;boolean leaves,transparency,ao,shadows,vsync;
  Map<String,Boolean> effects=new LinkedHashMap<>();
 }
 private static State capture(){
  var o=Minecraft.getInstance().options;var a=new State();
  a.view=requestedDistance();a.simulation=o.simulationDistance().get();a.entities=o.entityDistanceScaling().get();a.particles=o.particles().get().ordinal();a.weather=o.weatherRadius().get();a.biome=o.biomeBlendRadius().get();a.cap=o.framerateLimit().get();a.clouds=o.cloudStatus().get().name();a.preset=o.graphicsPreset().get().name();a.leaves=o.cutoutLeaves().get();a.transparency=o.improvedTransparency().get();a.ao=o.ambientOcclusion().get();a.shadows=o.entityShadows().get();a.vsync=o.enableVsync().get();
  for(String key:EFFECTS)try{a.effects.put(key,Settings.class.getField(key).getBoolean(CakeClient.settings));}catch(ReflectiveOperationException ignored){}
  return a;
 }
 private static void remember(){
  if(before!=null)return;
  try{if(Files.exists(backup()))before=JSON.fromJson(Files.readString(backup()),State.class);}catch(Exception e){CakeClient.LOG.warn("Could not read FPS restore point",e);}
  if(before==null){before=capture();try{Files.createDirectories(backup().getParent());Path t=backup().resolveSibling(backup().getFileName()+".tmp");Files.writeString(t,JSON.toJson(before));Files.move(t,backup(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){CakeClient.LOG.warn("Could not save FPS restore point",e);}}
 }
 public static int requestedDistance(){var s=CakeClient.settings;return s.fpsRenderDistance>=2?s.fpsRenderDistance:Minecraft.getInstance().options.renderDistance().get();}
 public static void tick(){
  var mc=Minecraft.getInstance();if(mc.options==null)return;var s=CakeClient.settings;
  if(!initialized){initialized=true;if(s.fpsRenderDistance<2)s.fpsRenderDistance=mc.options.renderDistance().get();applyFogLimit();}
  int actual=mc.options.renderDistance().get();
  if(lastDistance>=0&&actual!=lastDistance){s.fpsRenderDistance=actual;lastDistance=actual;}
  if(s.fogEnabled&&s.fogLimitTerrain)applyFogLimit();
 }
 public static void applyFogLimit(){
  var mc=Minecraft.getInstance();if(mc.options==null)return;var s=CakeClient.settings;
  int value=requestedDistance();if(s.fogEnabled&&s.fogLimitTerrain)value=Math.min(value,FogMath.chunksFor(FogMath.distance(s.fogDistance)));
  if(mc.options.renderDistance().get()!=value){mc.options.renderDistance().set(value);dirty=true;}
  lastDistance=value;
 }
 public static void preset(int level){
  remember();var mc=Minecraft.getInstance();var o=mc.options;var s=CakeClient.settings;
  var p=FpsPlan.preset(level,before.view,before.simulation,before.entities);
  o.graphicsPreset().set(GraphicsPreset.FAST);s.fpsRenderDistance=p.view();o.simulationDistance().set(p.simulation());o.entityDistanceScaling().set(p.entities());o.particles().set(ParticleStatus.values()[p.particles()]);o.weatherRadius().set(p.weather());o.cloudStatus().set(CloudStatus.OFF);o.cutoutLeaves().set(p.fastLeaves());o.improvedTransparency().set(p.improvedTransparency());o.ambientOcclusion().set(p.ao());o.entityShadows().set(false);o.biomeBlendRadius().set(0);o.enableVsync().set(false);o.framerateLimit().set(260);
  if(s.fpsReduceEffects)reduceEffects();s.fpsPreset=level;dirty=true;applyFogLimit();flush();s.save();status="Profil actif immédiatement · "+o.renderDistance().get()+" chunks";
 }
 public static void reduceEffects(){
  remember();for(String key:EFFECTS)try{Settings.class.getField(key).setBoolean(CakeClient.settings,false);}catch(ReflectiveOperationException ignored){}
  MotionBlur.reset();CakeClient.settings.changed();
 }
 public static void edit(Consumer<Options> action){remember();action.accept(Minecraft.getInstance().options);CakeClient.settings.fpsPreset=4;dirty=true;}
 public static void view(int chunks){remember();CakeClient.settings.fpsPreset=4;CakeClient.settings.fpsRenderDistance=Math.clamp(chunks,2,32);applyFogLimit();}
 public static void flush(){if(dirty){Minecraft.getInstance().options.save();dirty=false;}}
 public static void restore(){
  remember();if(before==null)return;var a=before;var o=Minecraft.getInstance().options;var s=CakeClient.settings;
  o.graphicsPreset().set(GraphicsPreset.valueOf(a.preset));s.fpsRenderDistance=a.view;o.simulationDistance().set(a.simulation);o.entityDistanceScaling().set(a.entities);o.particles().set(ParticleStatus.values()[Math.clamp(a.particles,0,2)]);o.weatherRadius().set(a.weather);o.cloudStatus().set(CloudStatus.valueOf(a.clouds));o.cutoutLeaves().set(a.leaves);o.improvedTransparency().set(a.transparency);o.ambientOcclusion().set(a.ao);o.entityShadows().set(a.shadows);o.biomeBlendRadius().set(a.biome);o.enableVsync().set(a.vsync);o.framerateLimit().set(a.cap);
  if(a.effects!=null)for(var e:a.effects.entrySet())try{Settings.class.getField(e.getKey()).setBoolean(s,e.getValue());}catch(ReflectiveOperationException ignored){}
  s.fpsPreset=0;dirty=true;applyFogLimit();flush();s.save();before=null;try{Files.deleteIfExists(backup());}catch(Exception ignored){}status="Vos réglages précédents ont été rétablis";
 }
 public static void potato(boolean enabled){
  if(loading)return;loading=true;var mc=Minecraft.getInstance();
  try{
   Path directory=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("resourcepacks");Files.createDirectories(directory);
   if(enabled)try(var in=CakeOptimize.class.getResourceAsStream("/assets/cakeclient/potato/CakeClient-Potato-1.21.11.zip")){if(in==null)throw new java.io.IOException("Pack absent");Path temp=directory.resolve("cake-potato.tmp");Files.copy(in,temp,StandardCopyOption.REPLACE_EXISTING);Files.move(temp,directory.resolve("CakeClient-Potato-1.21.11.zip"),StandardCopyOption.REPLACE_EXISTING);}
   var repo=mc.getResourcePackRepository();repo.reload();var selected=new ArrayList<>(repo.getSelectedIds());selected.remove(PACK);if(enabled)selected.add(PACK);repo.setSelected(selected);mc.options.updateResourcePacks(repo);mc.options.save();CakeClient.settings.potatoEnabled=enabled;CakeClient.settings.save();status="Chargement des textures…";
   mc.reloadResourcePacks().whenComplete((ok,error)->mc.execute(()->{loading=false;status=error==null?"Textures "+(enabled?"Potato":"personnelles")+" actives":"Échec du chargement du pack";CakeClient.settings.changed();}));
  }catch(Exception e){loading=false;status="Chargement impossible : "+e.getClass().getSimpleName();CakeClient.LOG.warn("Potato pack failed",e);}
 }
 /** Compatibility entry point for older settings screens. */
 public static String apply(boolean potato,int level){if(level==0)restore();else preset(level);if(CakeClient.settings.potatoEnabled!=potato)potato(potato);return status;}
}
