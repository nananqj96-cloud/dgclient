package fr.cake.client;
import java.io.*;
import java.nio.*;
import org.joml.Matrix4f;

/** Indexed mesh and original four-weight skinning with sampled author animation clips. */
public final class AvatarMesh {
 public final int vertices,bones;public final float[] data;public final int[] joints,indices;public final Clip[] clips;public final float[] scratch;
 public record Clip(float duration,int frames,Matrix4f[] poses){
  Matrix4f bone(int bone,int count,double seconds){double f=(seconds%duration)/duration*(frames-1);int a=Math.min(frames-2,(int)f);return new Matrix4f(poses[a*count+bone]).lerp(poses[(a+1)*count+bone],(float)(f-a));}
 }
 private AvatarMesh(int vertices,int bones,float[] data,int[] joints,int[] indices,Clip[] clips){this.scratch=new float[vertices*6];this.vertices=vertices;this.bones=bones;this.data=data;this.joints=joints;this.indices=indices;this.clips=clips;}
 public static AvatarMesh read(InputStream in)throws IOException{
  if(in==null)throw new IOException("Missing avatar");ByteBuffer b=ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
  try{if(b.getInt()!=0x324d5641)throw new IOException("Unknown avatar format");int n=b.getInt(),ni=b.getInt(),nb=b.getInt(),nc=b.getInt();if(n<1||n>60000||ni<3||ni>150000||ni%3!=0||nb<1||nb>128||nc!=2)throw new IOException("Invalid avatar counts");float[] data=new float[n*12];for(int i=0;i<data.length;i++){data[i]=b.getFloat();if(!Float.isFinite(data[i]))throw new IOException("Nonfinite mesh");}int[] joints=new int[n*4],indices=new int[ni];for(int i=0;i<joints.length;i++){joints[i]=Short.toUnsignedInt(b.getShort());if(joints[i]>=nb)throw new IOException("Bad joint");}for(int i=0;i<ni;i++){indices[i]=b.getInt();if(indices[i]<0||indices[i]>=n)throw new IOException("Bad index");}Clip[] clips=new Clip[nc];for(int c=0;c<nc;c++){float duration=b.getFloat();int frames=b.getInt();if(!(duration>0)||!Float.isFinite(duration)||frames<2||frames>3000)throw new IOException("Invalid clip");Matrix4f[] poses=new Matrix4f[frames*nb];for(int f=0;f<poses.length;f++){float[] m=new float[16];for(int k=0;k<16;k++){m[k]=b.getFloat();if(!Float.isFinite(m[k]))throw new IOException("Bad matrix");}poses[f]=new Matrix4f().set(m);}clips[c]=new Clip(duration,frames,poses);}if(b.hasRemaining())throw new IOException("Trailing mesh bytes");return new AvatarMesh(n,nb,data,joints,indices,clips);
  }catch(BufferUnderflowException e){throw new IOException("Truncated avatar",e);}
 }
 public Matrix4f[] pose(double idle,double walk,float amount){Matrix4f[] result=new Matrix4f[bones];for(int i=0;i<bones;i++)result[i]=clips[0].bone(i,bones,idle).lerp(clips[1].bone(i,bones,Math.max(0,walk)),Math.clamp(amount,0,1));return result;}
}
