package fr.cake.client;
/** Native fog distances in blocks; no extra framebuffer or post-processing pass. */
public final class FogMath {
 private FogMath(){}
 public record Range(float start,float end){}
 public static float distance(double slider){return 32+8*Math.round(Math.clamp(slider,0,1)*44);}
 public static Range range(double distance,double startFraction,double strength,float vanillaEnd){
  float end=(float)Math.max(1,Math.min(distance,vanillaEnd));
  float width=end*(1-(float)Math.clamp(startFraction,0,.95))*(.2f+.8f*(float)Math.clamp(strength,0,1));
  return new Range(Math.max(0,end-width),end);
 }
 public static int chunksFor(float end){return Math.max(2,(int)Math.ceil(end/16.));}
}
