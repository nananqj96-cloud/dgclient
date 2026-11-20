package fr.cake.client;
import java.util.*;import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;import net.minecraft.client.renderer.rendertype.RenderTypes;import net.minecraft.client.renderer.texture.OverlayTexture;import net.minecraft.resources.Identifier;import org.joml.Vector3f;
/** Wing-only extraction of Quaternius source meshes, retaining original skinning and Flying_Idle clip. */
public final class RiggedWings {
 private static final Map<Integer,AvatarMesh> CACHE=new HashMap<>();
 public static final Map<Integer,String> MODELS=Map.of(1,"dragon",2,"dragon_evolved",5,"alpaking");
 private static AvatarMesh load(int style){try(var in=RiggedWings.class.getResourceAsStream("/assets/cakeclient/back/hd/"+MODELS.get(style)+".avm")){return AvatarMesh.read(in);}catch(Exception e){CakeClient.LOG.error("Imported rigged wing",e);return null;}}
 public static void submit(PoseStack stack,SubmitNodeCollector collector,int light,int style,double time,boolean animate){
  var mesh=CACHE.computeIfAbsent(style,RiggedWings::load);if(mesh==null)return;var bones=mesh.pose(animate?time:0,0,0);
  stack.pushPose();stack.translate(0,.15,.17);stack.scale(1,-1,1);stack.mulPose(new org.joml.Quaternionf().rotationX(-.6f));
  var texture=Identifier.fromNamespaceAndPath("cakeclient","avatars/atlas.png");
  collector.submitCustomGeometry(stack,RenderTypes.entityCutoutNoCull(texture),(pose,v)->{
   float[] p=mesh.scratch;Vector3f point=new Vector3f(),normal=new Vector3f(),temp=new Vector3f();
   for(int n=0;n<mesh.vertices;n++){int i=n*12;float[] d=mesh.data;point.zero();normal.zero();for(int k=0;k<4;k++){float w=d[i+8+k];if(w<.00001f)continue;var bone=bones[mesh.joints[n*4+k]];bone.transformPosition(d[i],d[i+1],d[i+2],temp);point.fma(w,temp);bone.transformDirection(d[i+3],d[i+4],d[i+5],temp);normal.fma(w,temp);}pose.pose().transformPosition(point);pose.transformNormal(normal.x,normal.y,normal.z,normal);normal.normalize();int o=n*6;p[o]=point.x;p[o+1]=point.y;p[o+2]=point.z;p[o+3]=normal.x;p[o+4]=normal.y;p[o+5]=normal.z;}
   for(int q=0;q<mesh.indices.length/3*4;q++){int n=mesh.indices[(q/4)*3+Math.min(q%4,2)],o=n*6,i=n*12;v.addVertex(p[o],p[o+1],p[o+2]).setColor(255,255,255,255).setUv(mesh.data[i+6],mesh.data[i+7]).setLight(light).setOverlay(OverlayTexture.NO_OVERLAY).setNormal(p[o+3],p[o+4],p[o+5]);}
  });stack.popPose();
 }
}
