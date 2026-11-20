package fr.cake.client;
import java.util.*;import org.joml.Matrix4f;import org.joml.Vector3f;import com.mojang.blaze3d.vertex.VertexConsumer;import net.minecraft.world.entity.player.Player;import net.minecraft.client.Minecraft;import net.minecraft.client.renderer.texture.OverlayTexture;
/** Bounded damped cloth strip, with front/back surfaces, thickness and closed edges. */
public final class CapeCloth {
 public static final int SEGMENTS=16;private static final Map<UUID,Cloth> states=new LinkedHashMap<>();private static Object world;
 private static class Cloth{final float[] angle=new float[SEGMENTS],velocity=new float[SEGMENTS];long time;}
 static float integrate(float position,float velocity,float target,float dt){return velocity+((target-position)*52-velocity*13)*dt;}
 private static float[] pose(Player player,double speed){var mc=Minecraft.getInstance();if(world!=mc.level){states.clear();world=mc.level;}if(states.size()>32)states.clear();var cloth=states.computeIfAbsent(player.getUUID(),k->new Cloth());long now=System.nanoTime();float dt=cloth.time==0?.016f:(float)Math.clamp((now-cloth.time)/1e9,0,.05);cloth.time=now;if(mc.isPaused())return cloth.angle;double motion=player.getDeltaMovement().horizontalDistance();double bend=Math.min(.9,motion*3.5)+Math.min(.35,Math.max(0,-player.getDeltaMovement().y)*.4);for(int j=0;j<SEGMENTS;j++){float weight=(j+1f)/SEGMENTS;float target=(float)(.10+bend*weight+Math.sin(now/1e9*(1.1+speed)-j*.38)*.045*weight);cloth.velocity[j]=integrate(cloth.angle[j],cloth.velocity[j],target,dt);cloth.angle[j]=Math.clamp(cloth.angle[j]+cloth.velocity[j]*dt,-.03f,1.25f);}return cloth.angle;}
 public static void draw(VertexConsumer v,Matrix4f matrix,Player player,double speed,boolean nativeUv,int light){float[] angles=pose(player,speed);float y=.08f,z=.19f,w=.3125f,t=.012f;for(int i=0;i<SEGMENTS;i++){float nextY=y+(float)Math.cos(angles[i])*1.02f/SEGMENTS,nextZ=z+(float)Math.sin(angles[i])*1.02f/SEGMENTS;float a=i/(float)SEGMENTS,b=(i+1f)/SEGMENTS;
  float u0=nativeUv?1/64f:0,u1=nativeUv?11/64f:1,v0=nativeUv?(1+a*16)/32:a,v1=nativeUv?(1+b*16)/32:b;
  face(v,matrix,new float[][]{{-w,y,z},{w,y,z},{w,nextY,nextZ},{-w,nextY,nextZ}},u0,v0,u1,v1,light);
  face(v,matrix,new float[][]{{w,y,z+t},{-w,y,z+t},{-w,nextY,nextZ+t},{w,nextY,nextZ+t}},nativeUv?12/64f:0,v0,nativeUv?22/64f:1,v1,light);
  face(v,matrix,new float[][]{{-w,y,z+t},{-w,y,z},{-w,nextY,nextZ},{-w,nextY,nextZ+t}},0,v0,nativeUv?1/64f:.02f,v1,light);
  face(v,matrix,new float[][]{{w,y,z},{w,y,z+t},{w,nextY,nextZ+t},{w,nextY,nextZ}},nativeUv?11/64f:.98f,v0,nativeUv?12/64f:1,v1,light);
  if(i==0)face(v,matrix,new float[][]{{-w,y,z+t},{w,y,z+t},{w,y,z},{-w,y,z}},u0,0,u1,nativeUv?1/32f:.02f,light);
  if(i==SEGMENTS-1)face(v,matrix,new float[][]{{-w,nextY,nextZ},{w,nextY,nextZ},{w,nextY,nextZ+t},{-w,nextY,nextZ+t}},u0,nativeUv?17/32f:.98f,u1,nativeUv?18/32f:1,light);y=nextY;z=nextZ;
 }}
 private static void face(VertexConsumer v,Matrix4f m,float[][] p,float u,float t,float r,float b,int light){float[][] uv={{u,t},{r,t},{r,b},{u,b}};Vector3f n=new Vector3f(p[1]).sub(new Vector3f(p[0])).cross(new Vector3f(p[2]).sub(new Vector3f(p[0]))).normalize();m.transformDirection(n).normalize();for(int i=0;i<4;i++){var q=m.transformPosition(new Vector3f(p[i]));v.addVertex(q.x,q.y,q.z).setUv(uv[i][0],uv[i][1]).setColor(255,255,255,255).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(n.x,n.y,n.z);}}
}
