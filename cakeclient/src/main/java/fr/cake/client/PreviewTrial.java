package fr.cake.client;
import net.minecraft.client.Minecraft;
public final class PreviewTrial {
 private static int pending=-1;private static net.minecraft.client.CameraType camera;private static boolean back;
 private static boolean active,spatial,knife,inspectPending;
 private static boolean blur,flies,sky,sakura,motes,embers,atmosphere;
 private static long until,inspectAt;
 public static boolean active(){return active;}
 public static void start(int module){pending=module;}
 private static void begin(int module){
  Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
  if(module==6&&!mc.player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS))return;
  cancel();var s=CakeClient.settings;
  camera=module==20?mc.options.getCameraType():null;back=s.backEnabled;atmosphere=s.atmosphere;spatial=SpatialPanel.visible();knife=s.knives;inspectPending=module==6;blur=s.motionBlur;flies=s.fireflies;sky=s.sky;sakura=s.sakura;motes=s.motes;embers=s.embers;
  if(mc.screen!=null)mc.setScreen(null);SpatialPanel.hide();
  if(module==20){s.backEnabled=true;mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);}if(module==7)s.atmosphere=true;if(module==5)LyricsSky.preview();if(module==6)s.knives=true;if(module==0)s.motionBlur=true;if(module==1)s.fireflies=true;if(module==3)s.sky=true;if(module==4){s.sakura=true;s.motes=true;}
  long now=System.nanoTime();double delay=module==6?Knives.drawSeconds()+.15:1;inspectAt=now+(long)(delay*1e9);active=true;until=now+(long)((module==6?delay+Knives.inspectionSeconds()+1:6)*1e9);s.changed();
 }
 public static void tick(Minecraft mc){if(pending>=0){int module=pending;pending=-1;begin(module);}if(active&&inspectPending&&System.nanoTime()>inspectAt){inspectPending=false;Knives.inspect();}if(active&&(mc.level==null||mc.screen!=null||System.nanoTime()>=until)){boolean returnMenu=mc.level!=null&&mc.screen==null;cancel();if(returnMenu){if(spatial&&CakeClient.settings.spatial)SpatialPanel.toggle();else mc.setScreen(new ClientScreen(null));}}}
 public static void cancel(){pending=-1;if(!active)return;active=false;LyricsSky.stopPreview();var s=CakeClient.settings;s.backEnabled=back;if(camera!=null){Minecraft.getInstance().options.setCameraType(camera);camera=null;}s.atmosphere=atmosphere;s.knives=knife;s.motionBlur=blur;s.fireflies=flies;s.sky=sky;s.sakura=sakura;s.motes=motes;s.embers=embers;s.changed();MotionBlur.reset();}
}
