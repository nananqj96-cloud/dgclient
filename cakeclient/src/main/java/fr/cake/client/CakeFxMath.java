package fr.cake.client;
/** Pure math for position/color of inexpensive Cake ambient particles. */
public final class CakeFxMath {
 private CakeFxMath(){}
 public static double[] locate(double x,double y,double z,int type,double phase){
  double radius=type==6?1.05:.17;
  return new double[]{x+Math.cos(phase)*radius,y+(type==6?.12:.13),z+Math.sin(phase)*radius};
 }
 public static float[] rgb(int style){
  float h=switch(style){case 1->.68f;case 2->.02f;case 3->.50f;default->.77f;};
  int color=java.awt.Color.HSBtoRGB(h,.75f,1f);
  return new float[]{((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f};
 }
}
