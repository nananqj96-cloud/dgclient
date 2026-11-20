package fr.cake.client;
import java.io.*;import java.nio.*;import java.util.*;import org.joml.Matrix4f;import org.joml.Vector3f;
/** Source meshes, original CS action bone transforms and retargeted native arm poses. */
public final class CsKnifeAsset {
 public record Part(int joint,int count,ByteBuffer vertices){}
 public static final class Clip {
  public final float duration,holdStart,holdEnd;public final int frames;private final float[] data;private final int stride,parts;private final boolean fullHand;private final int handFloats;public final String name;
  Clip(String name,float d,int f,int p,float[] a,int handFloats,float holdStart,float holdEnd){this.holdStart=holdStart;this.holdEnd=holdEnd;this.name=name;duration=d;frames=f;parts=p;data=a;this.handFloats=handFloats;fullHand=handFloats>=16;stride=p*16+handFloats;}
  public Matrix4f matrix(int part,double seconds){
   double frame=Math.clamp(seconds/duration,0,1)*(frames-1);int a=(int)frame,b=Math.min(frames-1,a+1);float t=(float)(frame-a);
   Matrix4f first=new Matrix4f().set(Arrays.copyOfRange(data,a*stride+part*16,a*stride+part*16+16));
   if(a==b||t==0)return first;
   Matrix4f second=new Matrix4f().set(Arrays.copyOfRange(data,b*stride+part*16,b*stride+part*16+16));
   Vector3f position=first.getTranslation(new Vector3f()).lerp(second.getTranslation(new Vector3f()),t);
   var rotation=first.getNormalizedRotation(new org.joml.Quaternionf()).slerp(second.getNormalizedRotation(new org.joml.Quaternionf()),t);
   return new Matrix4f().translationRotate(position.x,position.y,position.z,rotation.x,rotation.y,rotation.z,rotation.w);
  }
  public Matrix4f handMatrix(double seconds){return fullHand?matrix(parts,seconds):new Matrix4f().translation(hand(seconds));}
  public boolean hasLeftArm(){return handFloats==32;}
  public Matrix4f leftArmMatrix(double seconds){return hasLeftArm()?matrix(parts+1,seconds):new Matrix4f();}
  public Vector3f hand(double seconds){if(fullHand)return matrix(parts,seconds).getTranslation(new Vector3f());double frame=Math.clamp(seconds/duration,0,1)*(frames-1);int a=(int)frame,b=Math.min(frames-1,a+1),x=a*stride+parts*16,y=b*stride+parts*16;return new Vector3f(data[x],data[x+1],data[x+2]).lerp(new Vector3f(data[y],data[y+1],data[y+2]),(float)(frame-a));}
 }
 public final List<Part> parts;public final Clip[] clips;
 public int find(String name){for(int i=0;i<clips.length;i++)if(clips[i].name.equals(name))return i;return -1;}
 private CsKnifeAsset(List<Part> p,Clip[] c){parts=List.copyOf(p);clips=c;}
 public static CsKnifeAsset load(String name)throws IOException{
  try(var in=CsKnifeAsset.class.getResourceAsStream("/assets/cakeclient/knives/"+name+".ckm")){
   if(in==null)throw new FileNotFoundException(name);ByteBuffer b=ByteBuffer.wrap(in.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
   int magic=b.getInt();boolean version7=magic==0x374d4b43,version6=magic==0x364d4b43||version7,full=magic==0x354d4b43||version6;int hands=version6?32:full?16:3;if(magic!=0x344d4b43&&!full)throw new IOException("Invalid knife mesh");int n=b.getInt();if(n<1||n>32)throw new IOException("Invalid mesh parts");List<Part> parts=new ArrayList<>();
   for(int i=0;i<n;i++){int joint=b.getInt(),count=b.getInt();if(count<3||count>1_000_000)throw new IOException("Invalid vertices");ByteBuffer vertices=ByteBuffer.allocateDirect(count*28).order(ByteOrder.nativeOrder());ByteBuffer src=b.slice();src.limit(count*28);vertices.put(src).flip();b.position(b.position()+count*28);parts.add(new Part(joint,count,vertices));}
   Clip[] clips=new Clip[b.getInt()];for(int i=0;i<clips.length;i++){String label;if(version6){int length=Short.toUnsignedInt(b.getShort());byte[] raw=new byte[length];b.get(raw);label=new String(raw,java.nio.charset.StandardCharsets.UTF_8);}else label=new String[]{"idle","inspect","deploy","slash1","slash2","stab"}[Math.min(i,5)];float d=b.getFloat(),holdStart=version7?b.getFloat():-1,holdEnd=version7?b.getFloat():-1;int frames=b.getInt();if(d<=0||frames<1||frames>2000)throw new IOException("Invalid clip");float[] data=new float[frames*(n*16+hands)];for(int k=0;k<data.length;k++)data[k]=b.getFloat();clips[i]=new Clip(label,d,frames,n,data,hands,holdStart,holdEnd);}
   if(b.hasRemaining())throw new IOException("Unexpected knife data");return new CsKnifeAsset(parts,clips);
  }catch(BufferUnderflowException e){throw new IOException("Truncated knife asset",e);}
 }
}
