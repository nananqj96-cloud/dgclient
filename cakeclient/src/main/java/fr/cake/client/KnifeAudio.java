package fr.cake.client;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import java.io.*;
import java.util.*;
/** Only identified CS recordings, timed by original animation events. */
public final class KnifeAudio {
 private static JsonObject cues;private static long serial=-2;private static double last=-.001;private static int model=-1;
 private static final List<SimpleSoundInstance> active=new ArrayList<>();
 private static String source(int m){return switch(KnifeCatalog.IDS[m]){case "classic"->"css";case "huntsman"->"tactical";case "shadow"->"push";case "nomad"->"canis";case "paracord"->"cord";case "survival"->"outdoor";default->KnifeCatalog.IDS[m];};}
 public static void stop(){var mc=Minecraft.getInstance();for(var sound:active)mc.getSoundManager().stop(sound);active.clear();serial=-2;last=-.001;}
 public static void frame(int m,String clip,long sequence,double seconds){
  var s=CakeClient.settings;if(!s.sounds||!s.knifeOriginalSounds||Minecraft.getInstance().screen!=null||!Minecraft.getInstance().isWindowActive()){if(!active.isEmpty())stop();return;}
  if(cues==null)try(var in=KnifeAudio.class.getResourceAsStream("/assets/cakeclient/knives/audio-cues.json")){cues=JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){cues=new JsonObject();CakeClient.LOG.warn("Original knife cues unavailable",e);}
  if(model!=m||serial!=sequence){stop();model=m;serial=sequence;}
  var bank=cues.getAsJsonObject(source(m));if(bank==null)return;var list=bank.getAsJsonArray(clip);if(list==null)return;
  var mc=Minecraft.getInstance();active.removeIf(sound->!mc.getSoundManager().isActive(sound));
  for(var item:list){var c=item.getAsJsonObject();double at=c.get("at").getAsDouble();if(at>last&&at<=seconds&&seconds-at<.18){var sound=SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("cakeclient",c.get("sound").getAsString())),1,(float)(s.knifeVolume*.7));mc.getSoundManager().play(sound);active.add(sound);}}
  last=seconds;
 }
}
