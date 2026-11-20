package fr.cake.client;

import org.joml.Vector3f;
import java.util.*;
import java.nio.*;

/** Original geometry, not extracted from Valve. Positions are in metres. */
public final class KnifeMesh {
 public record Vertex(float x,float y,float z,float nx,float ny,float nz,int rgb){}
 public final List<Vertex> vertices=new ArrayList<>();
 public void triangle(Vector3f a,Vector3f b,Vector3f c,int color){Vector3f n=new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();if(!Float.isFinite(n.x))return;vertex(a,n,color);vertex(b,n,color);vertex(c,n,color);}
 private void vertex(Vector3f p,Vector3f n,int color){vertices.add(new Vertex(p.x,p.y,p.z,n.x,n.y,n.z,color));}
 public ByteBuffer bytes(){ByteBuffer out=ByteBuffer.allocateDirect(vertices.size()*20).order(ByteOrder.nativeOrder());for(Vertex v:vertices)out.putFloat(v.x).putFloat(v.y).putFloat(v.z).put((byte)(v.rgb>>16)).put((byte)(v.rgb>>8)).put((byte)v.rgb).put((byte)255).put((byte)(v.nx*127)).put((byte)(v.ny*127)).put((byte)(v.nz*127)).put((byte)0);out.flip();return out;}
 public void ellipsoid(float cx,float cy,float cz,float rx,float ry,float rz,int color){
  int lat=10,lon=14;
  for(int i=0;i<lat;i++)for(int j=0;j<lon;j++){
   Vector3f a=point(cx,cy,cz,rx,ry,rz,Math.PI*i/lat,2*Math.PI*j/lon),b=point(cx,cy,cz,rx,ry,rz,Math.PI*(i+1)/lat,2*Math.PI*j/lon),c=point(cx,cy,cz,rx,ry,rz,Math.PI*(i+1)/lat,2*Math.PI*(j+1)/lon),d=point(cx,cy,cz,rx,ry,rz,Math.PI*i/lat,2*Math.PI*(j+1)/lon);
   smooth(a,normal(a,cx,cy,cz,rx,ry,rz),b,normal(b,cx,cy,cz,rx,ry,rz),c,normal(c,cx,cy,cz,rx,ry,rz),color);smooth(a,normal(a,cx,cy,cz,rx,ry,rz),c,normal(c,cx,cy,cz,rx,ry,rz),d,normal(d,cx,cy,cz,rx,ry,rz),color);
  }
 }
 private static Vector3f normal(Vector3f p,float x,float y,float z,float rx,float ry,float rz){return new Vector3f((p.x-x)/(rx*rx),(p.y-y)/(ry*ry),(p.z-z)/(rz*rz)).normalize();}
 private void smooth(Vector3f a,Vector3f na,Vector3f b,Vector3f nb,Vector3f c,Vector3f nc,int color){vertex(a,na,color);vertex(b,nb,color);vertex(c,nc,color);}
 private static Vector3f point(float cx,float cy,float cz,float rx,float ry,float rz,double a,double b){return new Vector3f(cx+rx*(float)(Math.sin(a)*Math.cos(b)),cy+ry*(float)Math.cos(a),cz+rz*(float)(Math.sin(a)*Math.sin(b)));}
 public void tube(Vector3f a,Vector3f b,float radiusA,float radiusB,int color){
  Vector3f axis=new Vector3f(b).sub(a).normalize(),side=axis.cross(new Vector3f(0,0,1),new Vector3f());if(side.lengthSquared()<.01)side=axis.cross(new Vector3f(0,1,0),new Vector3f());side.normalize();Vector3f up=new Vector3f(axis).cross(side).normalize();int n=14;
  for(int i=0;i<n;i++){Vector3f r1=new Vector3f(side).mul((float)Math.cos(i*2*Math.PI/n)).fma((float)Math.sin(i*2*Math.PI/n),up),r2=new Vector3f(side).mul((float)Math.cos((i+1)*2*Math.PI/n)).fma((float)Math.sin((i+1)*2*Math.PI/n),up);
   Vector3f p1=new Vector3f(a).fma(radiusA,r1),p2=new Vector3f(b).fma(radiusB,r1),p3=new Vector3f(b).fma(radiusB,r2),p4=new Vector3f(a).fma(radiusA,r2);smooth(p1,r1,p2,r1,p3,r2,color);smooth(p1,r1,p3,r2,p4,r2,color);triangle(a,p4,p1,color);triangle(b,p2,p3,color);
  }
 }
 public void ring(float x,float y,float z,float radius,float thickness,int color){int n=44,k=12;for(int i=0;i<n;i++)for(int j=0;j<k;j++){
  Vector3f a=torus(x,y,z,radius,thickness,i,j,n,k),b=torus(x,y,z,radius,thickness,i+1,j,n,k),c=torus(x,y,z,radius,thickness,i+1,j+1,n,k),d=torus(x,y,z,radius,thickness,i,j+1,n,k);smooth(a,torusNormal(i,j,n,k),b,torusNormal(i+1,j,n,k),c,torusNormal(i+1,j+1,n,k),color);smooth(a,torusNormal(i,j,n,k),c,torusNormal(i+1,j+1,n,k),d,torusNormal(i,j+1,n,k),color);
 }}
 private static Vector3f torusNormal(int i,int j,int n,int k){double a=i*2*Math.PI/n,b=j*2*Math.PI/k;return new Vector3f((float)(Math.cos(a)*Math.cos(b)),(float)(Math.sin(a)*Math.cos(b)),(float)Math.sin(b));}
 private static Vector3f torus(float x,float y,float z,float r,float t,int i,int j,int n,int k){double a=i*2*Math.PI/n,b=j*2*Math.PI/k;return new Vector3f(x+(float)((r+t*Math.cos(b))*Math.cos(a)),y+(float)((r+t*Math.cos(b))*Math.sin(a)),z+(float)(t*Math.sin(b)));}
 public void polygon(float[][] p,float thickness,int face,int edge){
  int n=p.length;List<int[]> tris=triangulate(p);float z=thickness*.5f;
  for(int[] t:tris){triangle(v(p[t[0]],z),v(p[t[1]],z),v(p[t[2]],z),face);triangle(v(p[t[2]],-z),v(p[t[1]],-z),v(p[t[0]],-z),face);}
  for(int i=0;i<n;i++){int j=(i+1)%n;triangle(v(p[i],z),v(p[i],-z),v(p[j],-z),edge);triangle(v(p[i],z),v(p[j],-z),v(p[j],z),edge);}
 }
 public void blade(float[][] outline,float thickness,int face,int edge){
  int n=outline.length;float cx=0,cy=0;for(float[] p:outline){cx+=p[0];cy+=p[1];}cx/=n;cy/=n;
  float[][] inner=new float[n][2];for(int i=0;i<n;i++){inner[i][0]=cx+(outline[i][0]-cx)*.90f;inner[i][1]=cy+(outline[i][1]-cy)*.94f;}
  float z=thickness/2;for(int[] t:triangulate(inner)){triangle(v(inner[t[0]],z),v(inner[t[1]],z),v(inner[t[2]],z),face);triangle(v(inner[t[2]],-z),v(inner[t[1]],-z),v(inner[t[0]],-z),face);}
  for(int i=0;i<n;i++){int j=(i+1)%n;triangle(v(outline[i],0),v(inner[i],z),v(inner[j],z),edge);triangle(v(outline[i],0),v(inner[j],z),v(outline[j],0),edge);triangle(v(outline[j],0),v(inner[j],-z),v(inner[i],-z),edge);triangle(v(outline[j],0),v(inner[i],-z),v(outline[i],0),edge);}
 }
 private static Vector3f v(float[] p,float z){return new Vector3f(p[0],p[1],z);}
 public static List<int[]> triangulate(float[][] p){
  List<Integer> ids=new ArrayList<>();double area=0;for(int i=0;i<p.length;i++){ids.add(i);int j=(i+1)%p.length;area+=p[i][0]*p[j][1]-p[j][0]*p[i][1];}if(area<0)Collections.reverse(ids);
  List<int[]> triangles=new ArrayList<>();int guard=0;
  while(ids.size()>3&&guard++<p.length*p.length){boolean cut=false;for(int i=0;i<ids.size();i++){
   int a=ids.get((i+ids.size()-1)%ids.size()),b=ids.get(i),c=ids.get((i+1)%ids.size());if(cross(p[a],p[b],p[c])<=1e-9)continue;boolean inside=false;
   for(int d:ids)if(d!=a&&d!=b&&d!=c&&cross(p[a],p[b],p[d])>=-1e-9&&cross(p[b],p[c],p[d])>=-1e-9&&cross(p[c],p[a],p[d])>=-1e-9){inside=true;break;}
   if(!inside){triangles.add(new int[]{a,b,c});ids.remove(i);cut=true;break;}
  }if(!cut)throw new IllegalArgumentException("Knife polygon cannot be triangulated");}
  if(ids.size()==3)triangles.add(new int[]{ids.get(0),ids.get(1),ids.get(2)});return triangles;
 }
 private static double cross(float[] a,float[] b,float[] c){return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0]);}
}
