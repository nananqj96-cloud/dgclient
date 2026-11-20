package fr.cake.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public final class SpatialPanel {
 private static Vec3 anchor,right,normal,up;
 private static Object world;
 private static boolean visible,dragging;
 private static long appeared;
 public record Hit(double x,double y,double distance){}
 public static boolean visible(){return visible;}
 private static double width(){return Math.min(2.7+CakeClient.settings.panelSize*1.2,(1.9+CakeClient.settings.panelDistance*1.2)*1.5);}
 public static void hide(){visible=false;dragging=false;ClientScreen.canvas.release();}
 public static void toggle(){if(visible){hide();return;}var mc=Minecraft.getInstance();pin();}
 public static void placeFromMenu(){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  CakeClient.settings.spatial=true;CakeClient.settings.save();pin();if(mc.screen!=null)mc.setScreen(null);
 }
 public static void pin(){
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  Camera camera=mc.gameRenderer.getMainCamera();Vec3 forward=look(camera);
  Vec3 side=forward.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);
  anchor=camera.position().add(forward.scale(1.9+CakeClient.settings.panelDistance*1.2));
  normal=forward.scale(-1);right=side.normalize();up=normal.cross(right).normalize();world=mc.level;visible=true;appeared=System.nanoTime();ClientScreen.canvas.setSpatial(true);
 }
 public static void tick(Minecraft mc){
  if(mc.level==null||mc.player==null||world!=mc.level){if(visible)hide();anchor=null;return;}
  if(!CakeClient.settings.spatial&&visible)hide();
  if(dragging&&visible){Hit h=hit(mc.gameRenderer.getMainCamera().position(),look(mc.gameRenderer.getMainCamera()));if(h!=null)ClientScreen.canvas.drag(h.x,h.y);}
 }
 public static boolean mouse(int button,int action){
  var mc=Minecraft.getInstance();if(!visible||mc.screen!=null||button!=GLFW.GLFW_MOUSE_BUTTON_LEFT||mc.player==null)return false;
  if(action==GLFW.GLFW_RELEASE&&dragging){dragging=false;ClientScreen.canvas.release();return true;}
  if(action!=GLFW.GLFW_PRESS)return false;
  Hit h=hit(mc.gameRenderer.getMainCamera().position(),look(mc.gameRenderer.getMainCamera()));if(h==null)return false;
  Vec3 eye=mc.gameRenderer.getMainCamera().position(),end=eye.add(look(mc.gameRenderer.getMainCamera()).scale(h.distance));
  var blocked=mc.level.clip(new net.minecraft.world.level.ClipContext(eye,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
  if(blocked.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&blocked.getLocation().distanceToSqr(eye)<h.distance*h.distance-.02)return false;
  dragging=true;ClientScreen.canvas.click(h.x,h.y);return true;
 }
 public static Vec3 look(Camera camera){org.joml.Vector3f v=new org.joml.Vector3f(0,0,-1).rotate(camera.rotation());return new Vec3(v.x,v.y,v.z).normalize();}
 public static Hit hit(Vec3 eye,Vec3 direction){
  if(anchor==null||right==null||normal==null)return null;
  return intersect(eye,direction,anchor,right,normal,width(),width()*9/16);
 }
 public static Hit intersect(Vec3 eye,Vec3 direction,Vec3 center,Vec3 axis,Vec3 normal,double width,double height){
  double denominator=direction.dot(normal);if(Math.abs(denominator)<1e-7)return null;
  double t=center.subtract(eye).dot(normal)/denominator;if(t<=0||t>32)return null;
  Vec3 p=eye.add(direction.scale(t)).subtract(center);double u=p.dot(axis)/width+.5,v=.5-p.dot(normal.cross(axis).normalize())/height;
  if(u<0||u>1||v<0||v>1)return null;
  if(eye.subtract(center).dot(normal)<0)u=1-u;
  return new Hit(u*1920,v*1080,t);
 }
 public static void render(Camera camera,Matrix4f matrix){
  var mc=Minecraft.getInstance();if(!visible||mc.screen!=null||anchor==null||mc.level!=world)return;
  if(anchor.subtract(camera.position()).dot(look(camera)) < -width())return;
  boolean back=camera.position().subtract(anchor).dot(normal)<0;
  Hit h=hit(camera.position(),look(camera));
  ClientScreen.canvas.setSpatial(true);ClientScreen.canvas.update(h==null?-1:h.x,h==null?-1:h.y);
  float opacity=(float)(1-Math.pow(1-Math.min(1,(System.nanoTime()-appeared)/300_000_000.),3));
  WorldQuad.draw(ClientScreen.canvas.texture(),anchor,back?right.scale(-1):right,up,width(),width()*9/16,camera.position(),matrix,opacity,0,false);
 }
}
