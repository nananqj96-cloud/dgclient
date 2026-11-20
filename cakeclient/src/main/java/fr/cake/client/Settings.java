package fr.cake.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public final class Settings {
 public int shaderPack=0;
 public boolean backHalo=true;public double backLift=.5,realismExposure=.5,realismBloom=.4,realismRain=.5;public int realismLook=0;
 public boolean realismSaved=false,previousShadersEnabled=false;public String previousShader="";
 public String language="en";public boolean backEnabled=false,realistic=false,crosshairEnabled=false,knifeTrails=false,hitColor=false,deathEffects=false;public int backStyle=0,realismQuality=0,crosshairStyle=0,deathStyle=0;public double backScale=.5,backSpeed=.4,crosshairSize=.4,crosshairGap=.3,crosshairThickness=.3,crosshairHue=.77,knifeTrailHue=.77,knifeTrailLife=.4,hitColorHue=.77,hitColorStrength=.65;
 public boolean visualizer=false,hitParticles=false,extraParticles=false;public int visualizerStyle=1,hitParticleStyle=0,extraParticleStyle=0;
 public double visualizerHue=.77,visualizerSize=.5,hitParticleCount=.5,hitParticleSize=.4,hitParticleHue=.77,extraParticleSize=.5,extraParticleHue=.77;
 public double musicX=.025,musicY=.08,musicScale=.5,musicOpacity=.88;public boolean musicArtwork=true,musicBars=true;
 public int[] pvpTextures={0,0,0,0,0};
 public int menuKey=346,avatarSkin=21;public boolean shareCosmetics=true,skinEnabled=false;public double skinScale=.5;
 public boolean followEnabled=false,followSword=true,hitboxEnabled=false,hitboxPlayers=true,hotbarEnabled=false,knifeOriginalSounds=true;
 public double hotbarScale=.4,hotbarLift=0;public int hotbarStyle=0;public double followRange=.35,hitboxRange=.35,hitboxPadding=0,hitboxHue=.77,hotbarHue=.77,hotbarOpacity=.75,knifeVolume=.7;
 public double trailSize=.4,trailLife=.4,trailDensity=.4,jumpSize=.4,jumpLife=.4;
 public boolean potatoEnabled=false,trailEnabled=false,jumpEnabled=false;public int fpsPreset=0,trailStyle=0,jumpStyle=0;public double trailHue=.77,jumpHue=.77;
 public boolean fogEnabled=false,fogLimitTerrain=false,fogSky=true,fpsReduceEffects=true;
 public int fpsRenderDistance=-1;
 public double fogDistance=.35,fogStart=.30,fogOpacity=1,fogHue=.72,fogSaturation=.30,fogBrightness=.72,fogTint=.45;
 public boolean knives=false;
 public int knifeModel=0,knifeFinish=0;
 public double knifeScale=.5,knifeSpeed=.5;
 public boolean motionBlur=false,fireflies=false,sounds=true,rainbow=false;
 public double blur=.4,density=.35,speed=.35,radius=.5,glow=.4,hue=.76;
 public boolean skyClouds=false,spatial=false,sky=false,sakura=false,motes=false,embers=false,lyrics=false;
 public double menuScale=.62,panelSize=.5,panelDistance=.5;
 public int skyMode=2;
 public double skyHue=.73,skyIntensity=.65,skySpeed=.25,stars=.4;
 public double ambientDensity=.3,wind=.3,lyricSize=.5,lyricDistance=.45,lyricOpacity=.85,lyricOffset=.5;
 public double lyricHeight=.5,lyricSpacing=.4,lyricOverlap=.7;
 public double lyricHue=.76,ambientHue=.72,fogStrength=0;public int lyricFont=0;
 public java.util.Map<String,Long> lyricCalibrations=new java.util.LinkedHashMap<>();
 public boolean comets=false;
 public boolean atmosphere=false,snow=false,pollen=false;
 public int atmosphereStyle=0;
 public double bloom=.3,occlusion=.5,exposure=.5,vignette=.25;
 public static final Path DIR=FabricLoader.getInstance().getConfigDir().resolve("cake-client");
 private static final Path LEGACY=DIR.resolveSibling("cake-client.json");
 private static final Path PATH=Edition.PLAN==Edition.Plan.PREMIUM?LEGACY:DIR.resolveSibling("cake-client-"+Edition.name().toLowerCase(java.util.Locale.ROOT)+".json");
 private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
 public static volatile long revision;
 public static Settings load(){
  try{Path input=Files.exists(PATH)?PATH:LEGACY;if(Files.exists(input)){String raw=Files.readString(input);Settings s=GSON.fromJson(raw,Settings.class);if(s!=null&&!com.google.gson.JsonParser.parseString(raw).getAsJsonObject().has("fogEnabled")&&s.fogStrength>0){s.fogEnabled=true;s.fogHue=s.ambientHue;}if(s!=null){s.sanitize();return s;}}}
  catch(Exception e){CakeClient.LOG.warn("Invalid configuration; using defaults",e);}Settings s=new Settings();s.sanitize();return s;
 }
 private static double c(double n,double fallback){return Double.isFinite(n)?Math.clamp(n,0,1):fallback;}
 public void sanitize(){shaderPack=Math.clamp(shaderPack,0,1);backLift=c(backLift,.5);realismExposure=c(realismExposure,.5);realismBloom=c(realismBloom,.4);realismRain=c(realismRain,.5);realismLook=Math.clamp(realismLook,0,2);
  if(language==null||!java.util.Set.of("en","fr","ru").contains(language))language="en";backStyle=Math.clamp(backStyle,0,12);realismQuality=Math.clamp(realismQuality,0,2);crosshairStyle=Math.clamp(crosshairStyle,0,7);deathStyle=Math.clamp(deathStyle,0,4);backScale=c(backScale,.5);backSpeed=c(backSpeed,.4);crosshairSize=c(crosshairSize,.4);crosshairGap=c(crosshairGap,.3);crosshairThickness=c(crosshairThickness,.3);crosshairHue=c(crosshairHue,.77);knifeTrailHue=c(knifeTrailHue,.77);knifeTrailLife=c(knifeTrailLife,.4);hitColorHue=c(hitColorHue,.77);hitColorStrength=c(hitColorStrength,.65);
  musicX=c(musicX,.025);musicY=c(musicY,.08);musicScale=c(musicScale,.5);musicOpacity=c(musicOpacity,.88);
  fpsPreset=Math.clamp(fpsPreset,0,4);fpsRenderDistance=fpsRenderDistance<2?-1:Math.clamp(fpsRenderDistance,2,32);
  if(pvpTextures==null||pvpTextures.length!=5)pvpTextures=new int[5];for(int i=0;i<5;i++)pvpTextures[i]=PvpCatalog.valid(i,pvpTextures[i]);
  if(menuKey<32||menuKey>348||menuKey==256)menuKey=346;avatarSkin=Math.clamp(avatarSkin,1,32);skinScale=c(skinScale,.5);
  trailStyle=Math.clamp(trailStyle,0,7);jumpStyle=Math.clamp(jumpStyle,0,11);hotbarStyle=Math.clamp(hotbarStyle,0,3);
  trailSize=c(trailSize,.4);trailLife=c(trailLife,.4);trailDensity=c(trailDensity,.4);jumpSize=c(jumpSize,.4);jumpLife=c(jumpLife,.4);followRange=c(followRange,.35);hitboxRange=c(hitboxRange,.35);hitboxPadding=c(hitboxPadding,0);hitboxHue=c(hitboxHue,.77);hotbarScale=c(hotbarScale,.4);hotbarLift=c(hotbarLift,0);hotbarHue=c(hotbarHue,.77);hotbarOpacity=c(hotbarOpacity,.75);knifeVolume=c(knifeVolume,.7);
  fogDistance=c(fogDistance,.35);fogStart=c(fogStart,.3);fogOpacity=c(fogOpacity,1);fogHue=c(fogHue,.72);fogSaturation=c(fogSaturation,.3);fogBrightness=c(fogBrightness,.72);fogTint=c(fogTint,.45);
  knifeModel=Math.clamp(knifeModel,0,KnifeCatalog.COUNT-1);knifeFinish=Math.clamp(knifeFinish,0,13);knifeScale=c(knifeScale,.5);knifeSpeed=c(knifeSpeed,.5);
  blur=c(blur,.4);density=c(density,.35);speed=c(speed,.35);radius=c(radius,.5);glow=c(glow,.4);hue=c(hue,.76);
  menuScale=Math.clamp(c(menuScale,.62),.42,.78);panelSize=c(panelSize,.5);panelDistance=c(panelDistance,.5);
  skyMode=Math.clamp(skyMode,0,9);skyHue=c(skyHue,.73);skyIntensity=c(skyIntensity,.65);skySpeed=c(skySpeed,.25);stars=c(stars,.4);
  ambientDensity=c(ambientDensity,.3);wind=c(wind,.3);lyricSize=c(lyricSize,.5);lyricDistance=c(lyricDistance,.45);lyricOpacity=c(lyricOpacity,.85);lyricOffset=c(lyricOffset,.5);
  lyricOverlap=c(lyricOverlap,.7);if(lyricCalibrations==null)lyricCalibrations=new java.util.LinkedHashMap<>();lyricCalibrations.entrySet().removeIf(e->e.getKey()==null||e.getKey().length()>800||e.getValue()==null||Math.abs((double)e.getValue())>120000);while(lyricCalibrations.size()>24)lyricCalibrations.remove(lyricCalibrations.keySet().iterator().next());
  lyricHeight=c(lyricHeight,.5);lyricSpacing=c(lyricSpacing,.4);lyricHue=c(lyricHue,.76);ambientHue=c(ambientHue,.72);fogStrength=c(fogStrength,0);lyricFont=Math.clamp(lyricFont,0,4);
  atmosphereStyle=Math.clamp(atmosphereStyle,0,3);bloom=c(bloom,.3);occlusion=c(occlusion,.5);exposure=c(exposure,.5);vignette=c(vignette,.25);
  Edition.enforce(this);
 }
 public void changed(){sanitize();revision++;}
 public void save(){
  sanitize();revision++;
  try{Files.createDirectories(PATH.getParent());Path tmp=PATH.resolveSibling(PATH.getFileName()+".tmp");Files.writeString(tmp,GSON.toJson(this));
   try{Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,PATH,StandardCopyOption.REPLACE_EXISTING);}
  }catch(Exception e){CakeClient.LOG.error("Could not save settings",e);}
 }
 public int color(double t){return java.awt.Color.HSBtoRGB((float)(rainbow?(t*.035)%1:hue),.5f,1f)&0xffffff;}
}
