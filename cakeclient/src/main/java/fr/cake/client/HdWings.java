package fr.cake.client;
import java.io.*;
import java.nio.*;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
/** Original attributed feather geometry, cached once. No per-vertex allocation. */
public final class HdWings {
 public record Part(int side,int material,float[] vertices,int[] indices){}
 private static Part[] cached;
 public static Part[] read(InputStream in)throws IOException {
  if(in==null)throw new IOException("Missing wing mesh");
  try{ByteBuffer b=ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);if(b.getInt()!=0x31445743||b.getInt()!=4)throw new IOException("Bad wing header");Part[] result=new Part[4];
   for(int p=0;p<4;p++){int side=b.getInt(),material=b.getInt(),n=b.getInt(),ni=b.getInt();if(side<0||side>1||material<0||material>1||n<3||n>20000||ni<3||ni>60000||ni%3!=0)throw new IOException("Bad wing counts");float[] v=new float[n*8];for(int k=0;k<v.length;k++){v[k]=b.getFloat();if(!Float.isFinite(v[k]))throw new IOException("Bad wing vertex");}int[] indices=new int[ni];for(int k=0;k<ni;k++){indices[k]=b.getInt();if(indices[k]<0||indices[k]>=n)throw new IOException("Bad wing index");}result[p]=new Part(side,material,v,indices);}if(b.hasRemaining())throw new IOException("Trailing wing bytes");return result;
  }catch(BufferUnderflowException e){throw new IOException("Truncated wing",e);}
 }
 private static Part[] mesh(){if(cached==null){try(var in=HdWings.class.getResourceAsStream("/assets/cakeclient/back/hd/angel.cwd")){cached=read(in);}catch(IOException e){CakeClient.LOG.error("HD wings",e);cached=new Part[0];}}return cached;}
 public static void submit(PoseStack stack,SubmitNodeCollector collector,int light,int style,double time,boolean animated){
  for(Part part:mesh()){
   final float side=part.side==0?1:-1;
   stack.pushPose();stack.translate(side*.17f,.05f,.16f);
   float flap=animated?(float)Math.sin(time*1.15)*.13f:0;
   stack.mulPose(new org.joml.Quaternionf().rotationY(side*(.10f+flap)).rotateZ(side*WingFit.FEATHER_OPENING));
   // Source X extends outwards, Z upwards, Y is feather depth.
   stack.scale(side*.95f,-.95f,.95f);
   var texture=Identifier.fromNamespaceAndPath("cakeclient","back/hd/"+(part.material==0?"feather":"wing")+".png");
   collector.submitCustomGeometry(stack,RenderTypes.entityCutoutNoCull(texture),(pose,v)->{
    Matrix4f m=pose.pose();float[] a=part.vertices;int[] indices=part.indices;
    for(int i=0;i<indices.length;i+=3)for(int q=0;q<4;q++){
     int k=indices[i+Math.min(q,2)]*8;float x=a[k],y=a[k+2],z=a[k+1];float nx=a[k+3],ny=a[k+5],nz=a[k+4];
     float px=m.m00()*x+m.m10()*y+m.m20()*z+m.m30(),py=m.m01()*x+m.m11()*y+m.m21()*z+m.m31(),pz=m.m02()*x+m.m12()*y+m.m22()*z+m.m32();
     float dx=m.m00()*nx+m.m10()*ny+m.m20()*nz,dy=m.m01()*nx+m.m11()*ny+m.m21()*nz,dz=m.m02()*nx+m.m12()*ny+m.m22()*nz;float inv=1f/(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
     v.addVertex(px,py,pz).setColor(style==3?65:255,style==3?61:style==4?205:255,style==3?92:style==4?128:255,255).setUv(a[k+6],a[k+7]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(dx*inv,dy*inv,dz*inv);
    }
   });stack.popPose();
  }
 }
}
