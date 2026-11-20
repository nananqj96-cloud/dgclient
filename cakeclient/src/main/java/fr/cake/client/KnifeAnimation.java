package fr.cake.client;
/** Keyframed recreations; no official CS animation data is embedded. */
public final class KnifeAnimation {
 public record Pose(float x,float y,float z,float rx,float ry,float rz,float spin,float blade,float left,float right){
  public static final Pose IDLE=new Pose(0,0,0,0,0,0,0,0,0,0);
 }
 private static Pose p(float y,float z,float rx,float ry,float rz,float spin,float blade,float left,float right){return new Pose(0,y,z,rx,ry,rz,spin,blade,left,right);}
 public static Pose sample(int model,int mode,double progress){
  float t=(float)Math.clamp(progress,0,1);if(mode==0)return Pose.IDLE;
  if(mode==1){float e=(1-t);return new Pose(.05f*e,-.42f*e,.16f*e,25*e,0,-30*e,0,0,0,model==1?-170*e:0);}
  if(mode==3||mode==4){float swing=(float)Math.sin(Math.PI*t),ease=(float)Math.sin(Math.PI*Math.min(1,t*1.6));return new Pose(-.22f*swing,.07f*ease,.13f*swing,-20*swing,mode==4?30*swing:-35*swing,-65*swing,0,0,0,0);}
  float[] times={0,.12f,.28f,.44f,.60f,.78f,1};Pose[] keys;
  if(model==0)keys=new Pose[]{Pose.IDLE,p(.06f,.06f,-8,-18,-15,0,0,0,0),p(.10f,.08f,5,-28,7,350,0,0,0),p(.08f,.11f,14,12,25,710,0,0,0),p(.07f,.08f,6,30,-10,720,0,0,0),p(.04f,.03f,-5,8,-5,720,0,0,0),p(0,0,0,0,0,720,0,0,0)};
  else if(model==1)keys=new Pose[]{Pose.IDLE,p(.08f,.05f,-12,-15,-25,0,0,0,-150),p(.10f,.08f,12,5,-55,0,20,170,-15),p(.12f,.08f,4,25,30,0,180,185,-170),p(.10f,.06f,-15,-20,45,0,360,360,-350),p(.05f,.02f,8,12,-12,0,540,360,-180),p(0,0,0,0,0,0,720,720,-720)};
  else keys=new Pose[]{Pose.IDLE,p(.06f,.05f,-10,-20,-10,0,0,0,0),p(.12f,.11f,5,-55,12,0,0,0,0),p(.13f,.13f,10,65,22,0,0,0,0),p(.09f,.08f,-8,110,-15,0,0,0,0),p(.03f,.03f,0,35,4,0,0,0,0),Pose.IDLE};
  for(int i=0;i<times.length-1;i++)if(t<=times[i+1]){float a=(t-times[i])/(times[i+1]-times[i]);a=a*a*(3-2*a);return mix(keys[i],keys[i+1],a);}return keys[keys.length-1];
 }
 public static Pose mix(Pose a,Pose b,float t){return new Pose(lerp(a.x,b.x,t),lerp(a.y,b.y,t),lerp(a.z,b.z,t),lerp(a.rx,b.rx,t),lerp(a.ry,b.ry,t),lerp(a.rz,b.rz,t),lerp(a.spin,b.spin,t),lerp(a.blade,b.blade,t),lerp(a.left,b.left,t),lerp(a.right,b.right,t));}
 private static float lerp(float a,float b,float t){return a+(b-a)*t;}
}
