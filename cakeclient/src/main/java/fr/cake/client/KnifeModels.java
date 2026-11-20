package fr.cake.client;
import org.joml.Vector3f;
/** Four independently built knife designs, with separate balisong handles and blade. */
public final class KnifeModels {
 public record Model(KnifeMesh grip,KnifeMesh blade,KnifeMesh left,KnifeMesh right){}
 public static KnifeMesh hand(){
  KnifeMesh m=new KnifeMesh();int glove=0x242730,stitch=0x444955,skin=0xb98865;
  m.tube(new Vector3f(.015f,-.10f,.065f),new Vector3f(.13f,-.51f,.40f),.047f,.078f,skin);
  m.tube(new Vector3f(.045f,-.22f,.16f),new Vector3f(.14f,-.55f,.43f),.063f,.085f,0x1d232d);
  m.tube(new Vector3f(.041f,-.21f,.155f),new Vector3f(.051f,-.245f,.18f),.069f,.069f,stitch);
  m.ellipsoid(0,-.012f,.063f,.065f,.105f,.043f,glove);
  float[] ys={.089f,.035f,-.021f,-.074f};
  for(int i=0;i<4;i++){float y=ys[i],r=.018f-i*.001f;
   m.tube(new Vector3f(.052f,y,.066f),new Vector3f(.004f,y,.097f),r,r,glove);m.tube(new Vector3f(.004f,y,.097f),new Vector3f(-.050f,y,.061f),r,r*.9f,glove);m.tube(new Vector3f(-.050f,y,.061f),new Vector3f(-.033f,y,.010f),r*.9f,r*.75f,glove);
   m.ellipsoid(.010f,y,.096f,r*1.12f,r*.8f,.009f,stitch);
  }
  m.tube(new Vector3f(.055f,-.055f,.062f),new Vector3f(.079f,.005f,.049f),.023f,.020f,glove);m.tube(new Vector3f(.079f,.005f,.049f),new Vector3f(.028f,.051f,.032f),.020f,.016f,glove);
  m.ellipsoid(.003f,-.025f,.100f,.045f,.055f,.008f,0x303642);return m;
 }
 public static Model build(int type){
  KnifeMesh grip=new KnifeMesh(),blade=new KnifeMesh(),left=new KnifeMesh(),right=new KnifeMesh();int dark=0x262c35,edge=0xe1e7ed,steel=0xa9b5c4;
  if(type==0){
   grip.polygon(new float[][]{{-.031f,-.115f},{.025f,-.115f},{.033f,-.04f},{.025f,.076f},{.008f,.106f},{-.022f,.08f},{-.036f,-.04f}},.031f,dark,0x505866);
   grip.ring(0,.121f,0,.034f,.009f,steel);
   blade.blade(new float[][]{{-.027f,-.103f},{-.072f,-.13f},{-.13f,-.124f},{-.189f,-.078f},{-.225f,-.008f},{-.229f,.066f},{-.208f,.120f},{-.194f,.025f},{-.158f,-.031f},{-.107f,-.059f},{-.039f,-.059f}},.012f,steel,edge);
   for(float y=-.075f;y<.076f;y+=.036f)grip.tube(new Vector3f(-.024f,y,.019f),new Vector3f(.021f,y+.004f,.019f),.003f,.003f,0x626a77);
  }else if(type==1){
   blade.blade(new float[][]{{-.018f,.10f},{-.024f,.25f},{-.008f,.392f},{.026f,.292f},{.025f,.10f}},.009f,steel,edge);
   handle(left,-.022f);handle(right,.022f);grip.tube(new Vector3f(-.042f,.103f,0),new Vector3f(.042f,.103f,0),.012f,.012f,steel);
  }else{
   grip.polygon(new float[][]{{-.026f,-.12f},{.026f,-.12f},{.032f,-.082f},{.031f,.09f},{-.031f,.09f},{-.033f,-.082f}},.037f,dark,0x434d58);
   for(float y=-.095f;y<.08f;y+=.025f)grip.tube(new Vector3f(-.025f,y,.021f),new Vector3f(.025f,y,.021f),.003f,.003f,0x69717a);
   grip.polygon(new float[][]{{-.081f,.09f},{.077f,.09f},{.073f,.112f},{-.079f,.112f}},.029f,steel,edge);
   if(type==2){
    blade.blade(new float[][]{{-.026f,.107f},{-.027f,.31f},{0,.458f},{.031f,.325f},{.029f,.108f}},.011f,steel,edge);
    grip.ring(-.061f,.108f,0,.022f,.005f,steel);
   }else blade.blade(new float[][]{{-.029f,.105f},{-.048f,.323f},{-.044f,.438f},{.015f,.392f},{.060f,.315f},{.059f,.131f},{.027f,.105f}},.014f,steel,edge);
  }
  for(float y:new float[]{-.075f,.046f})grip.ellipsoid(0,y,.022f,.006f,.006f,.003f,0xb8c0ca);
  return new Model(grip,blade,left,right);
 }
 private static void handle(KnifeMesh m,float x){
  m.polygon(new float[][]{{x-.016f,-.124f},{x+.016f,-.124f},{x+.020f,-.087f},{x+.017f,.105f},{x-.017f,.105f},{x-.020f,-.087f}},.026f,0x353c49,0x85909f);
  for(float y=-.09f;y<.085f;y+=.037f){m.ellipsoid(x,y,.015f,.008f,.013f,.0018f,0x121720);}
  m.ellipsoid(x,.101f,.020f,.009f,.009f,.004f,0xd1d7df);
 }
}
