package fr.cake.client;
import org.joml.Vector3f;
/** Exact fist anchor after the vanilla first-person renderer's +/-0.1 Z rotation. */
public final class NativeHandGrip {
 public static Vector3f anchor(float pivotX,float pivotY,float pivotZ,float minX,float maxX,float maxY,float minZ,float maxZ,boolean left){float x=(minX+maxX)*.5f,y=maxY-1,z=(minZ+maxZ)*.5f,a=left?-.1f:.1f,c=(float)Math.cos(a),s=(float)Math.sin(a);return new Vector3f((pivotX+c*x-s*y)/16,(pivotY+s*x+c*y)/16,(pivotZ+z)/16);}
}
