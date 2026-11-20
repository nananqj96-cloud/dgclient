package fr.cake.client;
import net.minecraft.client.Minecraft;import net.minecraft.client.Camera;import net.minecraft.world.phys.Vec3;import org.joml.Matrix4f;
/** Bounded ribbons in the world, all submitted in one GPU batch. */
public final class Comets {
 private static Object world;private static Vec3 anchor;private static long born;
 public static void reset(){world=null;anchor=null;}
 private static Vec3 at(int i,double t){double phase=i*2.399963+t*(.10+(i%4)*.018),r=8+(i%5)*2.2;return anchor.add(Math.cos(phase)*r,1+(i%4)*2.1+Math.sin(phase*.8+i)*2,Math.sin(phase)*r);}
 public static void render(Camera camera,Matrix4f matrix){
  var mc=Minecraft.getInstance();if(!CakeClient.settings.comets||mc.level==null){reset();return;}
  if(world!=mc.level||anchor==null||anchor.distanceToSqr(camera.position())>6400){world=mc.level;anchor=camera.position();born=System.nanoTime();}
  var atlas=mc.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.PARTICLES);var sprite=atlas.getSprite(net.minecraft.resources.Identifier.fromNamespaceAndPath("cakeclient","mote"));double time=(System.nanoTime()-born)/1e9;
  InsectBatch.begin(camera,matrix);
  for(int i=0;i<14;i++){
   for(int j=0;j<14;j++){double p=j/14.,q=(j+1)/14.;InsectBatch.ribbon(at(i,time-(1-p)*1.7),at(i,time-(1-q)*1.7),.014+.026*q,0xb467ff,(float)(p*p*.82),(float)(q*q*.82),sprite);}
   InsectBatch.add(at(i,time),.075f,0,0xe9ceff,.95f,sprite);
  }
  InsectBatch.end(atlas.getTextureView());
 }
}
