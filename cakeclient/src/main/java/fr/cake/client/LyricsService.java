package fr.cake.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Optional read-only Windows media bridge. No audio capture, credentials or Spotify tokens. */
public final class LyricsService {
 public record Media(String key,String title,String artist,String album,long position,long duration,boolean playing,long sampled){
  public Media(String key,String title,String artist,long position,long duration,boolean playing,long sampled){this(key,title,artist,"",position,duration,playing,sampled);}
  public long currentPosition(){long value=position+(playing?Math.max(0,(System.nanoTime()-sampled)/1_000_000):0);return duration>0?Math.clamp(value,0,duration):Math.max(0,value);}
 }
 private static final PlaybackClock CLOCK=new PlaybackClock();
 private static final Gson GSON=new Gson();
 private static volatile Media media;
 private static volatile java.awt.image.BufferedImage artwork;
 public static java.awt.image.BufferedImage artwork(){return artwork;}
 private static volatile LrcTimeline timeline;
 private static volatile String status="Module désactivé.";
 private static volatile Process process;
 private static volatile boolean running;
 private static volatile long generation;
 private static Future<?> pendingLookup;
 private static long lookupGeneration;
 private static long nextStart;
 private static final ExecutorService FETCH=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Cake-Lyrics-Lookup");t.setDaemon(true);return t;});
 private static final Map<String,LrcTimeline> CACHE=Collections.synchronizedMap(new LinkedHashMap<>());
 public static String status(){return status;}
 public static Media media(){return media;}
 public static LrcTimeline timeline(){return timeline;}
 public static void settingsChanged(){if(!CakeClient.settings.lyrics&&!CakeClient.settings.visualizer){stop();status="Module désactivé.";}else{nextStart=0;status="Recherche de Spotify…";}}
 public static void retry(){stop();nextStart=0;CACHE.clear();settingsChanged();}
 public static void tick(){
  if(!CakeClient.settings.lyrics&&!CakeClient.settings.visualizer){if(running)stop();return;}
  if(running||System.nanoTime()<nextStart)return;
  if(!System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win")){status="La détection Spotify automatique nécessite Windows 10 ou 11.";nextStart=Long.MAX_VALUE;return;}
  nextStart=System.nanoTime()+15_000_000_000L;running=true;long token=++generation;
  Thread thread=new Thread(()->bridge(token),"Cake-Spotify-Media");thread.setDaemon(true);thread.start();
 }
 private static void bridge(long token){
  try(var stream=LyricsService.class.getResourceAsStream("/assets/cakeclient/bridge/spotify.ps1")){
   if(stream==null)throw new IOException("Missing media bridge");String source=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
   String encoded=Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_16LE));
   Process child=new ProcessBuilder("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-WindowStyle","Hidden","-EncodedCommand",encoded).redirectError(ProcessBuilder.Redirect.DISCARD).start();
   if(token!=generation){child.destroy();return;}process=child;
   try(var reader=new BufferedReader(new InputStreamReader(child.getInputStream(),StandardCharsets.UTF_8))){
    String line;while(token==generation&&(line=reader.readLine())!=null){if(line.length()<1_500_000)accept(line,token);}
   }finally{child.destroy();}
  }catch(Exception e){if(token==generation){status="Session média Windows indisponible. Vérifiez Spotify et les contrôles multimédias Windows.";CakeClient.LOG.warn("Spotify media bridge unavailable: {}",e.getClass().getSimpleName());}}
  finally{bridgeEnded(token);}
 }
 private static synchronized void bridgeEnded(long token){
  if(token!=generation)return;
  media=null;artwork=null;timeline=null;CLOCK.reset();running=false;process=null;cancelLookup();
  status="Session Spotify interrompue. Nouvelle tentative automatique…";nextStart=System.nanoTime()+15_000_000_000L;
 }
 private static synchronized void accept(String raw,long token){
  if(token!=generation)return;
  try{
   JsonObject j=JsonParser.parseString(raw).getAsJsonObject();
   if(!j.has("ready")||!j.get("ready").getAsBoolean()){
    media=null;artwork=null;timeline=null;CLOCK.reset();cancelLookup();status=j.has("error")?"Windows ne fournit pas la session Spotify. Relancez la détection.":"En attente d'une chanson dans Spotify…";return;
   }
   String title=str(j,"title"),artist=str(j,"artist");if(title.isBlank()||artist.isBlank()){media=null;timeline=null;status="Métadonnées du morceau indisponibles.";return;}
   String album=str(j,"album"),key=(artist+"\n"+title+"\n"+album).toLowerCase(Locale.ROOT);Media old=media;boolean changed=old==null||!old.key.equals(key);long now=System.nanoTime(),duration=Math.max(0,j.get("duration").getAsLong());boolean playing=j.get("playing").getAsBoolean();
   long rawPosition=Math.max(0,j.get("position").getAsLong()),stamp=j.has("updatedAt")?j.get("updatedAt").getAsLong():0,age=j.has("anchorAge")?j.get("anchorAge").getAsLong():0;double rate=j.has("rate")?j.get("rate").getAsDouble():1;
   long position=CLOCK.update(rawPosition,stamp,age,now,playing,rate,duration,changed);
   if(changed)artwork=null;
   if(j.has("artwork")&&!j.get("artwork").getAsString().isBlank()){
    try{byte[] bytes=Base64.getDecoder().decode(j.get("artwork").getAsString());if(bytes.length<1_000_000){var img=javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));if(img!=null&&img.getWidth()<=2048&&img.getHeight()<=2048)artwork=img;}}catch(Exception ignored){}
   }
   Media next=new Media(key,title,artist,album,position,duration,playing,now);media=next;
   if(!changed&&old.duration<=0&&duration>0)CACHE.remove(key);
   if(changed||(old.duration<=0&&duration>0)){
    cancelLookup();long request=lookupGeneration;
    timeline=null;status="Recherche de paroles synchronisées…";
    if(CakeClient.settings.lyrics)pendingLookup=FETCH.submit(()->lookup(next,token,request));
   }else if(timeline!=null)status=next.playing?"Spotify détecté · paroles synchronisées.":"Spotify en pause.";
  }catch(RuntimeException e){status="Données de lecture Spotify indisponibles.";}
 }
 private static String str(JsonObject j,String key){String s=j.has(key)&&!j.get(key).isJsonNull()?j.get(key).getAsString():"";return s.length()>250?s.substring(0,250):s;}
 private static void lookup(Media track,long token,long request){
  try{
   LrcTimeline result=CACHE.get(track.key);
   if(result==null){
    // A user-provided LRC can supply a missing or corrected timeline without a network request.
    String safe=(track.artist+" - "+track.title).replaceAll("[\\\\/:*?\"<>|]","_");Path local=Settings.DIR.resolve("lyrics").resolve(safe+".lrc");
    if(Files.isRegularFile(local)&&Files.size(local)<1_000_000)result=LrcTimeline.parse(Files.readString(local));
    else result=fetch(track);
    if(result!=null){if(CACHE.size()>24)CACHE.clear();CACHE.put(track.key,result);}
   }
   synchronized(LyricsService.class){
    if(token!=generation||request!=lookupGeneration||media==null||!media.key.equals(track.key))return;
    timeline=result;
    status=result!=null&&!result.lines.isEmpty()?"Spotify détecté · paroles synchronisées.":"Pas de paroles synchronisées disponibles pour ce morceau.";
   }
  }catch(Exception e){synchronized(LyricsService.class){if(token==generation&&request==lookupGeneration&&media!=null&&media.key.equals(track.key)){status="Recherche de paroles indisponible. Vérifiez Internet puis relancez la détection.";CakeClient.LOG.debug("Lyrics lookup failed: {}",e.getClass().getSimpleName());}}}
 }
 private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
 private static LrcTimeline fetch(Media track)throws Exception{
  HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
  String query="track_name="+enc(track.title)+"&artist_name="+enc(track.artist)+(track.album.isBlank()?"":"&album_name="+enc(track.album))+(track.duration>0?"&duration="+Math.round(track.duration/1000.):"");
  JsonElement json=get(client,"https://lrclib.net/api/get?"+query);
  if(json!=null&&json.isJsonObject()&&matches(json.getAsJsonObject(),track))return LrcTimeline.parse(strLyrics(json.getAsJsonObject()));
  json=get(client,"https://lrclib.net/api/search?track_name="+enc(track.title)+"&artist_name="+enc(track.artist));
  JsonObject best=null;double score=-Double.MAX_VALUE;
  if(json!=null&&json.isJsonArray())for(JsonElement e:json.getAsJsonArray()){
   JsonObject j=e.getAsJsonObject();if(!matches(j,track))continue;
   double rank=(!track.album.isBlank()&&str(j,"albumName").equalsIgnoreCase(track.album)?100:0)-(track.duration>0&&j.has("duration")?Math.abs(j.get("duration").getAsDouble()-track.duration/1000.):0);
   if(rank>score){best=j;score=rank;}
  }return best==null?null:LrcTimeline.parse(strLyrics(best));
 }
 private static String strLyrics(JsonObject j){return j.has("syncedLyrics")&&!j.get("syncedLyrics").isJsonNull()?j.get("syncedLyrics").getAsString():"";}
 private static boolean matches(JsonObject j,Media track){return str(j,"trackName").equalsIgnoreCase(track.title)&&str(j,"artistName").equalsIgnoreCase(track.artist)&&!strLyrics(j).isBlank()&&(track.duration<=0||(j.has("duration")&&Math.abs(j.get("duration").getAsDouble()-track.duration/1000.)<=2));}
 public static long calibration(){Media m=media;return m==null?0:CakeClient.settings.lyricCalibrations.getOrDefault(m.key,0L);}
 public static void calibrateLine(int step){Media m=media;LrcTimeline t=timeline;if(m==null||t==null||t.lines.isEmpty())return;long pos=m.currentPosition(),offset=(long)((CakeClient.settings.lyricOffset-.5)*30000);int line=Math.clamp(t.index(pos+offset+calibration())+step,0,t.lines.size()-1);CakeClient.settings.lyricCalibrations.put(m.key,Math.clamp(t.lines.get(line).start()-pos-offset,-120000L,120000L));CakeClient.settings.save();}
 public static void resetCalibration(){Media m=media;if(m!=null){CakeClient.settings.lyricCalibrations.remove(m.key);CakeClient.settings.save();}}
 public static String currentLine(){Media m=media;LrcTimeline t=timeline;if(m==null||t==null)return "En attente des paroles";int i=t.index(m.currentPosition()+(long)((CakeClient.settings.lyricOffset-.5)*30000)+calibration());return i>=0?t.lines.get(i).text():"Avant la première ligne";}
 public static String playbackLabel(){Media m=media;if(m==null)return "Aucun morceau";long ms=m.currentPosition()/1000;return String.format("%d:%02d · correction %+.1f s",ms/60,ms%60,calibration()/1000.);}
 private static JsonElement get(HttpClient client,String url)throws Exception{
  HttpRequest request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(9)).header("User-Agent","CakeClient/5.0 (Minecraft visual lyrics display)").GET().build();
  HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  if(response.statusCode()==404)return null;if(response.statusCode()!=200)throw new IOException("HTTP "+response.statusCode());
  if(response.body().length()>2_000_000)throw new IOException("Lyrics response too large");return JsonParser.parseString(response.body());
 }
 private static void cancelLookup(){lookupGeneration++;if(pendingLookup!=null){pendingLookup.cancel(true);pendingLookup=null;}}
 private static synchronized void stop(){generation++;running=false;Process p=process;process=null;if(p!=null)p.destroy();cancelLookup();media=null;artwork=null;timeline=null;CLOCK.reset();}
 public static void close(){stop();FETCH.shutdownNow();}
}
