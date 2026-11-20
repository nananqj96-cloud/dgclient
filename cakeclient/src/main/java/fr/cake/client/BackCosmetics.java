package fr.cake.client;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;import org.joml.Vector3f;
import java.io.InputStreamReader;import java.nio.charset.StandardCharsets;import java.util.*;
/** Geometry/UV imported from pau101/Wings (MIT), with bounded cosmetic animation. */
public final class BackCosmetics {
 public static final String[] IDS={"angel","dragon","bat","evil","fire","parrot","blue_butterfly","monarch_butterfly","fairy","slime","astral","royal","arctic"};
 public static final String[] NAMES={"Celestial","Dragon","Drake","Obsidian","Golden Angel","Alpaking","Blue Butterfly","Monarch","Fairy","Slime","Account / Astral Cape","Royal Cape","Arctic Cape"};
 private static class Shape{float[] box,uv;boolean plane;}
 private static class Bone{String name,parent;float[] pivot;Shape[] shapes;}
 private static final Map<Boolean,Bone[]> meshes=new HashMap<>();
 private static Bone[] load(boolean insect){return meshes.computeIfAbsent(insect,k->{try(var in=BackCosmetics.class.getResourceAsStream("/assets/cakeclient/back/"+(k?"insectoid":"avian")+".json")){return new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Bone[].class);}catch(Exception e){CakeClient.LOG.error("Wings import",e);return new Bone[0];}});}
 public static boolean replacesCape(Object raw){var mc=Minecraft.getInstance();if(mc.level==null||!(raw instanceof AvatarRenderState a)||!(mc.level.getEntity(a.id) instanceof net.minecraft.world.entity.player.Player p))return false;var state=CosmeticNetwork.get(p.getUUID());return state!=null&&(state.flags()&16)!=0;}
 public static void submit(Object raw,PoseStack stack,SubmitNodeCollector collector,int light){
  var s=CakeClient.settings;var mc=Minecraft.getInstance();if(s==null||mc.player==null||!(raw instanceof AvatarRenderState state)||state.isInvisible)return;
  var entity=mc.level.getEntity(state.id);if(!(entity instanceof net.minecraft.world.entity.player.Player player)||player.isSpectator()||state.distanceToCameraSq>1600)return;var cosmetic=CosmeticNetwork.get(player.getUUID());if(cosmetic==null||(cosmetic.flags()&16)==0)return;int id=Math.clamp(cosmetic.back(),0,12);boolean cape=id>=10,insect=id>=6&&!cape;double time=System.nanoTime()/1e9*(.5+cosmetic.backSpeed()*2);float scale=(float)(cape?.85+cosmetic.backScale()*.3:.65+cosmetic.backScale()*.35);
  stack.pushPose();if(!cape)stack.translate(0,WingFit.shoulderY(s.backLift,cosmetic.skin(),(cosmetic.flags()&8)!=0),.025);stack.scale(scale,scale,scale);if(player.isCrouching()){stack.translate(0,.15,.08);stack.mulPose(new org.joml.Quaternionf().rotationX(.3f));}
  if(id==0||id==3||id==4){HdWings.submit(stack,collector,light,id,time,s.backHalo);stack.popPose();return;}
  if(RiggedWings.MODELS.containsKey(id)){RiggedWings.submit(stack,collector,light,id,time,s.backHalo);stack.popPose();return;}
  boolean accountCape=id==10&&state.skin!=null&&state.skin.cape()!=null;var texture=accountCape?state.skin.cape().texturePath():Identifier.fromNamespaceAndPath("cakeclient","back/"+IDS[id]+(cape?".png":"_wings.png"));var type=RenderTypes.entityCutoutNoCull(texture);
  var source=new Matrix4f(stack.last().pose());
  SubmitNodeCollector.CustomGeometryRenderer geometry=(pose,v)->{
   if(cape){CapeCloth.draw(v,pose.pose(),player,cosmetic.backSpeed(),accountCape,light);return;}
   Map<String,Matrix4f> transforms=new HashMap<>();
   for(var b:load(insect)){
    Matrix4f m=b.parent.isEmpty()?new Matrix4f(pose.pose()):new Matrix4f(transforms.get(b.parent));m.translate(b.pivot[0]/16,b.pivot[1]/16,b.pivot[2]/16);
    float side=b.name.contains("Right")?-1:1;String n=b.name;
    if(insect&&n.startsWith("wing"))m.rotateY(side*(.35f+(float)Math.sin(time*3.8)*.32f));
    else if(n.startsWith("coracoid"))m.rotateY(side*(-.4102f+(float)Math.sin(time*1.5)*.12f)).rotateZ(side*(-.279f+(float)Math.sin(time*1.5)*.06f));
    else if(n.startsWith("humerus"))m.rotateY(side*.2269f).rotateZ(side*.5061f);
    else if(n.startsWith("ulna"))m.rotateY(side*.2094f).rotateZ(side*(-.4887f+(float)Math.sin(time*1.5-.6)*.06f));
    else if(n.startsWith("carpals"))m.rotateY(side*.0698f).rotateZ(side*.3194f);
    else if(n.startsWith("feathersCoracoid"))m.rotateZ(side*.4098f);
    transforms.put(b.name,m);for(var shape:b.shapes)box(v,m,shape,light);
   }
  };collector.submitCustomGeometry(stack,type,geometry);if(!cape&&s.backHalo)collector.submitCustomGeometry(stack,RenderTypes.eyes(Identifier.fromNamespaceAndPath("cakeclient","back/"+IDS[id]+"-halo.png")),geometry);stack.popPose();
 }
 private static void box(com.mojang.blaze3d.vertex.VertexConsumer v,Matrix4f m,Shape s,int light){float[] b=s.box;float x=b[0]/16,y=b[1]/16,z=b[2]/16,w=b[3]/16,h=b[4]/16,d=(s.plane?.14f:b[5])/16,u=s.uv[0],t=s.uv[1];
  if(s.plane||d==0){quad(v,m,new float[][]{{x,y,z},{x+w,y,z},{x+w,y+h,z},{x,y+h,z}},u/64,t/64,(u+b[3])/64,(t+b[4])/64,light);if(d>0)quad(v,m,new float[][]{{x+w,y,z+d},{x,y,z+d},{x,y+h,z+d},{x+w,y+h,z+d}},u/64,t/64,(u+b[3])/64,(t+b[4])/64,light);return;}
  float[][] p={{x,y,z},{x+w,y,z},{x+w,y+h,z},{x,y+h,z},{x,y,z+d},{x+w,y,z+d},{x+w,y+h,z+d},{x,y+h,z+d}};int[][] faces={{1,0,3,2},{4,5,6,7},{0,4,7,3},{5,1,2,6},{0,1,5,4},{3,7,6,2}};float a=b[3],c=b[4],e=b[5];float[][] uv={{u+e,t+e,u+e+a,t+e+c},{u+2*e+a,t+e,u+2*e+2*a,t+e+c},{u,t+e,u+e,t+e+c},{u+e+a,t+e,u+2*e+a,t+e+c},{u+e,t,u+e+a,t+e},{u+e+a,t,u+e+2*a,t+e}};for(int i=0;i<6;i++){int[] f=faces[i];float[] r=uv[i];quad(v,m,new float[][]{p[f[0]],p[f[1]],p[f[2]],p[f[3]]},r[0]/64,r[1]/64,r[2]/64,r[3]/64,light);}
 }
 private static void quad(com.mojang.blaze3d.vertex.VertexConsumer v,Matrix4f m,float[][] p,float u,float t,float r,float b,int light){float[][] uv={{u,t},{r,t},{r,b},{u,b}};Vector3f normal=new Vector3f(p[1]).sub(new Vector3f(p[0])).cross(new Vector3f(p[2]).sub(new Vector3f(p[0]))).normalize();m.transformDirection(normal).normalize();for(int i=0;i<4;i++){var point=m.transformPosition(new Vector3f(p[i]));v.addVertex(point.x,point.y,point.z).setUv(uv[i][0],uv[i][1]).setColor(255,255,255,255).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(normal.x,normal.y,normal.z);}}
}
