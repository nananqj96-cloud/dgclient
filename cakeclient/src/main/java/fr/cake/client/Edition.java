package fr.cake.client;
import java.util.*;
/** Distribution policy; it is deliberately separate from editable user settings. */
public final class Edition {
 public enum Plan { FREE,PRO,PREMIUM }
 public static final Plan PLAN=load();
 private static Plan load(){try(var in=Edition.class.getResourceAsStream("/assets/cakeclient/edition.txt")){return in==null?Plan.PREMIUM:Plan.valueOf(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).trim().toUpperCase(Locale.ROOT));}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public static boolean knife(Plan p,int id){return id>=0&&id<22&&(p==Plan.PREMIUM||(p==Plan.FREE?id==2:id<=3));}
 public static boolean sky(Plan p,int id){return id>=0&&id<=9&&(p==Plan.PREMIUM||(p==Plan.FREE?id==2:id==2||id==4||id==5));}
 public static boolean skin(Plan p,int id){return SkinCatalog.available(id)&&p==Plan.PREMIUM;}
 public static boolean musicCustomization(Plan p){return p==Plan.PREMIUM;}
 public static boolean module(Plan p,int tab){
  if(tab<0||tab>25)return false;
  if(p==Plan.PREMIUM)return true;
  if(p==Plan.PRO)return tab!=15&&tab!=20;
  return Set.of(0,3,6,8,9,22,25).contains(tab);
 }
 public static String name(){return switch(PLAN){case FREE->"Free";case PRO->"Pro";case PREMIUM->"Premium";};}
 public static void enforce(Settings s){
  if(!knife(PLAN,s.knifeModel))s.knifeModel=PLAN==Plan.FREE?2:0;
  if(!sky(PLAN,s.skyMode))s.skyMode=2;
  if(!skin(PLAN,s.avatarSkin))s.avatarSkin=21;
  if(!module(PLAN,0))s.motionBlur=false;if(!module(PLAN,1))s.fireflies=false;
  if(!module(PLAN,2))s.spatial=false;if(!module(PLAN,4)){s.sakura=false;s.motes=false;s.embers=false;s.comets=false;s.snow=false;s.pollen=false;}
  if(!module(PLAN,5))s.lyrics=false;if(!module(PLAN,7))s.atmosphere=false;
  if(!module(PLAN,10))s.trailEnabled=false;if(!module(PLAN,11))s.jumpEnabled=false;
  if(!module(PLAN,12))s.followEnabled=false;if(!module(PLAN,13))s.hitboxEnabled=false;
  if(!module(PLAN,14))s.hotbarEnabled=false;if(!module(PLAN,15))s.skinEnabled=false;
  if(!module(PLAN,16))s.visualizer=false;if(!module(PLAN,17))s.hitParticles=false;
  if(!module(PLAN,18))s.extraParticles=false;if(!module(PLAN,19))Arrays.fill(s.pvpTextures,0);
  if(!module(PLAN,20))s.backEnabled=false;if(!module(PLAN,21))s.realistic=false;
  if(!module(PLAN,23)){s.hitColor=false;s.deathEffects=false;}
  if(!module(PLAN,24)){s.knifeFinish=0;s.knifeTrails=false;}
  if(!musicCustomization(PLAN)){s.musicX=.025;s.musicY=.08;s.musicScale=.5;s.musicOpacity=.88;s.musicArtwork=true;s.musicBars=true;s.visualizerStyle=1;s.visualizerHue=.77;s.visualizerSize=.5;s.lyricSize=.5;s.lyricDistance=.45;s.lyricOpacity=.85;s.lyricHeight=.5;s.lyricSpacing=.4;s.lyricOverlap=.7;s.lyricHue=.76;s.lyricFont=0;}
 }
}
