package fr.cake.client;
public final class WingFit {
 public static final float FEATHER_OPENING=.50f;
 public static float shoulderY(double lift){return shoulderY(lift,0,false);}
 public static float shoulderY(double lift,int skin,boolean avatar){
  float base=avatar?switch(skin){case 28->.45f;case 26->.5f;case 27->.6f;case 22,30->.35f;case 31,32->.38f;case 23,20->.25f;default->.18f;}: .14f;
  return base-.28f*((float)Math.clamp(Double.isFinite(lift)?lift:.5,0,1)-.5f);
 }
}
