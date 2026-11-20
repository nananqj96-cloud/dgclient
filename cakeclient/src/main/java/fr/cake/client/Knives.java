package fr.cake.client;
import com.mojang.blaze3d.pipeline.*;import com.mojang.blaze3d.platform.DepthTestFunction;import com.mojang.blaze3d.shaders.UniformType;import com.mojang.blaze3d.systems.RenderSystem;import com.mojang.blaze3d.textures.FilterMode;import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;import net.minecraft.resources.Identifier;import net.minecraft.tags.ItemTags;import net.minecraft.world.entity.HumanoidArm;import org.joml.Matrix4f;import org.joml.Vector3f;import org.joml.Quaternionf;import java.nio.*;
/** CS source geometry and original action clips, with Minecraft's real arm. */
public final class Knives {
 private static final String[] NAMES=KnifeCatalog.IDS;
 private static Identifier id(String p){return Identifier.fromNamespaceAndPath("cakeclient",p);}
 private static CsKnifeAsset asset;private static NativeMesh[] meshes;
 static record NativeMesh(int count,float[] data){}
 private static final net.minecraft.client.renderer.rendertype.RenderType[][] TYPES=new net.minecraft.client.renderer.rendertype.RenderType[KnifeCatalog.COUNT][14];
 private static int loaded=-1,inspectVariant,drawVariant;private static boolean wasEligible,failed;
 private static KnifePlayback playback=new KnifePlayback();private static long lastSerial=-1;private static long transition;
 private static Matrix4f[] pose,previousPose;private static Matrix4f arm=new Matrix4f(),previousArm=new Matrix4f(),leftArm=new Matrix4f(),previousLeft=new Matrix4f();
 private static boolean supportHand;private static boolean cake$inspectHeld;
 public static boolean eligible(){var mc=Minecraft.getInstance();return CakeClient.settings!=null&&CakeClient.settings.knives&&mc.player!=null&&mc.level!=null&&!mc.player.isSpectator()&&!mc.player.isSleeping()&&!mc.options.hideGui&&mc.options.getCameraType().isFirstPerson()&&mc.player.getMainHandItem().is(ItemTags.SWORDS);}
 private static void ensure(){int model=CakeClient.settings.knifeModel;if(model==loaded)return;close();try{asset=CsKnifeAsset.load(NAMES[model]);meshes=new NativeMesh[asset.parts.size()];for(int i=0;i<meshes.length;i++){var part=asset.parts.get(i);var b=part.vertices().duplicate().order(ByteOrder.nativeOrder());float[] data=new float[part.count()*8];for(int v=0;v<part.count();v++){int offset=v*8;for(int k=0;k<5;k++)data[offset+k]=b.getFloat();b.getInt();for(int k=0;k<3;k++)data[offset+5+k]=b.get()/127f;b.get();}meshes[i]=new NativeMesh(part.count(),data);}loaded=model;}catch(Exception e){failed=true;loaded=model;CakeClient.LOG.error("CS knife asset could not load",e);}}
 private static double speed(){return .5+CakeClient.settings.knifeSpeed;}
 private static double[] durations(){double[] d=new double[asset.clips.length];for(int i=0;i<d.length;i++)d[i]=asset.clips[i].duration/speed();return d;}
 public static boolean inspect(){if(Minecraft.getInstance().screen!=null||!eligible())return false;ensure();if(asset==null)return false;int[] variants={1,asset.find("inspect2"),asset.find("inspect3")};int count=1;while(count<variants.length&&variants[count]>=0)count++;playback.play(variants[Math.floorMod(inspectVariant++,count)],System.nanoTime());sound("knife_flip",.4f);return true;}
 public static void attack(int button){if(Minecraft.getInstance().screen!=null||!eligible()||(button!=0&&button!=1))return;ensure();if(asset==null)return;playback.attack(button==1,System.nanoTime(),durations());sound("knife_swing",.4f);}
 private static Matrix4f base(){
  float side=Minecraft.getInstance().player.getMainArm()==HumanoidArm.LEFT?-1:1,scale=(float)(.85+CakeClient.settings.knifeScale*.3);
  float y=loaded==3?-.055f:-.045f,z=loaded==3?-.16f:loaded==0?-.10f:-.13f;
  return new Matrix4f().translation(.015f*side,y,z).scale(side*scale,scale,scale);
 }
 /** Key repeat never restarts an inspection. Releasing G finishes the original outro. */
 public static boolean keyEvent(int key,int action){
  if(key!=org.lwjgl.glfw.GLFW.GLFW_KEY_G)return false;
  if(action==org.lwjgl.glfw.GLFW.GLFW_RELEASE){boolean used=cake$inspectHeld;cake$inspectHeld=false;return used;}
  if(Minecraft.getInstance().screen!=null||!eligible())return false;
  if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS){if(!inspect())return false;cake$inspectHeld=true;return true;}
  return cake$inspectHeld;
 }
 private static void holdInspection(long now,double[] d){
  int mode=playback.mode();if(!cake$inspectHeld||loaded!=1||!asset.clips[mode].name.startsWith("inspect"))return;
  var c=asset.clips[mode];playback.skipInspectionHold(now,c.holdStart/speed(),c.holdEnd/speed());
  int[] variants={1,asset.find("inspect2"),asset.find("inspect3")};int index=0;while(index<variants.length-1&&variants[index]!=mode)index++;
  int next=variants[(index+1)%variants.length];if(next<0)next=1;
  if(playback.loopIfFinished(next,now,d))sound("knife_flip",.25f);
 }

 private static Matrix4f blend(Matrix4f a,Matrix4f b,float p){Vector3f pos=a.getTranslation(new Vector3f()).lerp(b.getTranslation(new Vector3f()),p),scale=a.getScale(new Vector3f()).lerp(b.getScale(new Vector3f()),p);Quaternionf q=a.getUnnormalizedRotation(new Quaternionf()).slerp(b.getUnnormalizedRotation(new Quaternionf()),p);return new Matrix4f().translationRotateScale(pos,q,scale);}
 /** Called inside the vanilla hand pass, so the blade and native arm share camera bob and rotation. */
 public static Matrix4f prepare(){
  ensure();if(asset==null)return new Matrix4f();long now=System.nanoTime();
  if(!wasEligible){wasEligible=true;int other=asset.find("deploy2");playback.play(other>=0&&(drawVariant++&1)==1?other:2,now);sound("knife_draw",.4f);}
  double[] d=durations();holdInspection(now,d);playback.advance(now,d);int mode=playback.mode();
  if(lastSerial!=playback.serial()){previousPose=pose;previousArm=new Matrix4f(arm);previousLeft=new Matrix4f(leftArm);transition=now;lastSerial=playback.serial();}
  var clip=asset.clips[mode];double t=playback.seconds(now,d,speed());KnifeAudio.frame(loaded,clip.name,playback.serial(),t);float mix=(float)Math.clamp((now-transition)/80_000_000.,0,1);mix=mix*mix*(3-2*mix);
  supportHand=clip.hasLeftArm()&&(clip.name.startsWith("inspect")||clip.name.startsWith("deploy"));
  leftArm=clip.leftArmMatrix(t);if(previousPose!=null&&mix<1)leftArm=blend(previousLeft,leftArm,mix);
  arm=clip.handMatrix(t);if(previousPose!=null&&mix<1)arm=blend(previousArm,arm,mix);
  pose=new Matrix4f[meshes.length];for(int i=0;i<pose.length;i++){pose[i]=clip.matrix(i,t);if(previousPose!=null&&mix<1)pose[i]=blend(previousPose[i],pose[i],mix);}
  boolean left=Minecraft.getInstance().player.getMainArm()==HumanoidArm.LEFT;
  return nativeArm(base().mul(arm),left);
 }
 public static Matrix4f supportArmPose(){boolean left=Minecraft.getInstance().player.getMainArm()!=HumanoidArm.LEFT;return nativeArm(base().mul(leftArm),left);}
 private static Matrix4f nativeArm(Matrix4f matrix,boolean left){var mc=Minecraft.getInstance();var model=mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player).getModel();var part=left?model.leftArm:model.rightArm;var pose=part.getInitialPose();var cubes=((fr.cake.client.mixin.ModelCubesAccess)(Object)part).cake$cubes();if(cubes.isEmpty())return matrix;var cube=cubes.getFirst();var grip=NativeHandGrip.anchor(pose.x(),pose.y(),pose.z(),cube.minX,cube.maxX,cube.maxY,cube.minZ,cube.maxZ,left);return matrix.scale(.32f,.58f,.32f).translate(-grip.x,-grip.y,-grip.z);}
 public static double drawSeconds(){ensure();return asset==null?1.2:asset.clips[2].duration/speed();}
 public static double inspectionSeconds(){ensure();return asset==null?6.2:asset.clips[1].duration/speed();}
 public static boolean supportHand(){return asset!=null&&asset.clips[playback.mode()].hasLeftArm();}
 private static net.minecraft.client.renderer.rendertype.RenderType type(){int f=CakeClient.settings.knifeFinish;return net.minecraft.client.renderer.rendertype.RenderTypes.entityCutoutNoCull(id(f==0?"knives/"+NAMES[loaded]+"-color.png":"knives/finishes/"+NAMES[loaded]+"-"+f+".png"));}
 /** Both source knife pieces and the skin arm are submitted through the vanilla hand collector. */
 public static void submit(PoseStack stack,net.minecraft.client.renderer.SubmitNodeCollector collector){
  if(asset==null||failed||pose==null)return;var type=type();KnifeTrail.submit(stack,collector,meshes,pose,base(),arm,playback.mode()!=0);
  for(int part=0;part<meshes.length;part++){
   if(loaded==10&&part==0&&!Minecraft.getInstance().player.getOffhandItem().isEmpty())continue;
   NativeMesh mesh=meshes[part];stack.pushPose();stack.mulPose(base().mul(pose[part]));
   collector.submitCustomGeometry(stack,type,(transform,vertices)->{float[] d=mesh.data;Vector3f point=new Vector3f(),normal=new Vector3f();for(int q=0;q<mesh.count/3*4;q++){int v=(q/4)*3+Math.min(q%4,2);int i=v*8;transform.pose().transformPosition(d[i],d[i+1],d[i+2],point);transform.transformNormal(d[i+5],d[i+6],d[i+7],normal);vertices.addVertex(point.x,point.y,point.z).setUv(d[i+3],d[i+4]).setColor(255,255,255,255).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(normal.x,normal.y,normal.z);}});
   stack.popPose();
  }
 }
 public static void render(){var mc=Minecraft.getInstance();if(mc.screen!=null||!mc.isWindowActive())cake$inspectHeld=false;if(!eligible()||mc.screen!=null||!mc.isWindowActive()){if(!eligible())wasEligible=false;cake$inspectHeld=false;KnifeAudio.stop();}}
 private static void sound(String n,float v){if(CakeClient.settings.sounds&&!CakeClient.settings.knifeOriginalSounds)Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id(n)),1,v));}
 public static void close(){KnifeTrail.clear();KnifeAudio.stop();meshes=null;asset=null;loaded=-1;failed=false;wasEligible=false;pose=null;previousPose=null;lastSerial=-1;arm.identity();leftArm.identity();cake$inspectHeld=false;playback=new KnifePlayback();}
}
