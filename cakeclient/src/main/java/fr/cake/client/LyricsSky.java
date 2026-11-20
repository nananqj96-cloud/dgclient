package fr.cake.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/** Separate groups live in different positions, including straight ahead and below eye level. */
public final class LyricsSky {
 private static final int SLOTS=12;
 private static final SmoothTexture[] textures=new SmoothTexture[SLOTS];private static final Card[] cards=new Card[SLOTS];
 private static long demoStarted;
 private static final LrcTimeline DEMO=LrcTimeline.parse("[00:00]let it happen\n[00:03]Des mots devant vous");
 public static void preview(){demoStarted=System.nanoTime();previous="";}
 public static void stopPreview(){demoStarted=0;previous="";}
 private static String previous="";private static int slot;private static Object world;
 private static long lastPosition=-1,lastFrame;
 private static final class Card {
  final SmoothTexture texture;Vec3 center,right,up;double visibility;final long born;final int line;final float left,rightEdge;final double from,to,width,shift,vertical,distance,hideAt;long retiring;
  Card(SmoothTexture texture,double shift,double vertical,double distance,int line,float left,float edge,double from,double to,double width,double hideAt){this.hideAt=hideAt;this.texture=texture;this.shift=shift;this.vertical=vertical;this.distance=distance;this.line=line;this.left=left;rightEdge=edge;this.from=from;this.to=to;this.width=width;born=System.nanoTime();}
 }
 public static void render(Camera camera,Matrix4f matrix){
  var mc=Minecraft.getInstance();var s=CakeClient.settings;
  if(world!=mc.level){clear();world=mc.level;}
  boolean demo=demoStarted!=0&&System.nanoTime()-demoStarted<6_000_000_000L;
  var media=demo?new LyricsService.Media("local-placement-demo","Placement","Cake",0,6000,true,demoStarted):LyricsService.media();var timeline=demo?DEMO:LyricsService.timeline();
  long now=System.nanoTime();
  if((!s.lyrics&&!demo)||mc.screen!=null||media==null||timeline==null||!media.playing()||(!demo&&now-media.sampled()>5_000_000_000L)){clear();return;}
  long position=media.currentPosition()+(long)((s.lyricOffset-.5)*30000)+(demo?0:LyricsService.calibration());int index=timeline.index(position);
  if(index<0||(media.duration()>0&&media.currentPosition()>=media.duration())){clear();return;}
  if(lastPosition>=0&&Math.abs(position-lastPosition-(now-lastFrame)/1_000_000)>1500)clear();
  lastPosition=position;lastFrame=now;
  String key=media.key()+"\n"+index;double progress=timeline.progress(index,position,media.duration());
  if(!key.equals(previous)){
   previous=key;for(Card c:cards)if(c!=null&&c.retiring==0)c.retiring=now;
   List<LyricChunks.Chunk> groups=LyricChunks.split(timeline.lines.get(index).text());
   Vec3 forward=SpatialPanel.look(camera),r=forward.cross(new Vec3(0,1,0));if(r.lengthSqr()<.001)r=new Vec3(1,0,0);else r=r.normalize();Vec3 u=r.cross(forward).normalize();
   double distance=5.5+s.lyricDistance*8;
   double aspect=mc.getWindow().getWidth()/(double)Math.max(1,mc.getWindow().getHeight());
   int layout=media.key().hashCode()^Integer.rotateLeft(index*0x9e3779b9,13);
   List<LyricPlacement.Place> places=LyricPlacement.arrange(groups.size(),layout,distance,s.lyricSize,s.lyricSpacing,s.lyricHeight,mc.options.fov().get(),aspect);
   int total=groups.stream().mapToInt(LyricChunks.Chunk::characters).sum(),consumed=0;
   for(int i=0;i<groups.size();i++){
    var group=groups.get(i);int n=slot++%SLOTS;if(textures[n]==null)textures[n]=new SmoothTexture("lyrics_"+n,1920,384);Raster raster=raster(group.text());textures[n].update(raster.image());
    var place=places.get(i);
    Vec3 center=camera.position().add(forward.scale(distance)).add(r.scale(place.x())).add(u.scale(place.y()));
    double from=consumed/(double)Math.max(1,total);consumed+=group.characters();double to=consumed/(double)Math.max(1,total);
    double width=place.width();
    cards[n]=new Card(textures[n],place.x(),place.y(),distance,index,raster.left(),raster.right(),from,to,width,groups.subList(0,Math.min(groups.size(),i+2+(int)Math.round(s.lyricOverlap*2))).stream().mapToInt(LyricChunks.Chunk::characters).sum()/(double)Math.max(1,total));
   }
  }
  for(int i=0;i<SLOTS;i++){
   Card c=cards[i];if(c==null)continue;
   double fadeOut=c.retiring==0?1:1-Math.min(1,(now-c.retiring)/850_000_000.);
   if(fadeOut<=0){cards[i]=null;continue;}
   if(c.retiring==0)c.visibility=LyricSequence.layered(progress,c.from,c.hideAt,.10);
   if(c.visibility<=.001)continue;
   if(c.center==null){
    Vec3 forward=SpatialPanel.look(camera),r=forward.cross(new Vec3(0,1,0));if(r.lengthSqr()<.001)r=new Vec3(1,0,0);else r=r.normalize();
    c.right=r;c.up=r.cross(forward).normalize();c.center=camera.position().add(forward.scale(c.distance)).add(r.scale(c.shift)).add(c.up.scale(c.vertical));
   }
   double groupProgress=c.line==index?Math.clamp((progress-c.from)/Math.max(.01,c.to-c.from),0,1):1;
   var qr=new org.joml.Vector3f(1,0,0).rotate(camera.rotation());var qu=new org.joml.Vector3f(0,1,0).rotate(camera.rotation());Vec3 billboardRight=new Vec3(qr.x,qr.y,qr.z),billboardUp=new Vec3(qu.x,qu.y,qu.z);
   WorldQuad.draw(c.texture,c.center.add(billboardUp.scale((1-c.visibility)*-.10)),billboardRight,billboardUp,c.width,c.width*384/1920,camera.position(),matrix,(float)(s.lyricOpacity*c.visibility*fadeOut),c.left+(c.rightEdge-c.left)*(float)groupProgress,true);
  }
 }
 private record Raster(BufferedImage image,float left,float right){}
 private static Raster raster(String text){
  BufferedImage image=new BufferedImage(1920,384,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  float size=254;g.setFont(CakeExtras.lyricFont(size,true));double measured=g.getFontMetrics().stringWidth(text);if(measured>1780)size=(float)(size*1780/measured);g.setFont(CakeExtras.lyricFont(Math.max(28,size),true));FontMetrics fm=g.getFontMetrics();int x=(1920-fm.stringWidth(text))/2,y=(384-fm.getHeight())/2+fm.getAscent();java.awt.Shape outline=g.getFont().createGlyphVector(g.getFontRenderContext(),text).getOutline(x,y);
  BufferedImage glow=new BufferedImage(480,96,BufferedImage.TYPE_INT_ARGB);Graphics2D gg=glow.createGraphics();gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);gg.scale(.25,.25);gg.setColor(CakeExtras.lyricGlow());gg.setStroke(new BasicStroke(22,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));gg.draw(outline);gg.fill(outline);gg.dispose();
  float[] kernel=new float[17];float total=0;for(int i=0;i<17;i++){kernel[i]=(float)Math.exp(-Math.pow((i-8)/3.4,2)/2);total+=kernel[i];}for(int i=0;i<17;i++)kernel[i]/=total;
  glow=new java.awt.image.ConvolveOp(new java.awt.image.Kernel(17,1,kernel),java.awt.image.ConvolveOp.EDGE_NO_OP,null).filter(glow,null);glow=new java.awt.image.ConvolveOp(new java.awt.image.Kernel(1,17,kernel),java.awt.image.ConvolveOp.EDGE_NO_OP,null).filter(glow,null);
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(glow,0,0,1920,384,null);g.setColor(CakeExtras.lyricOutline());g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(outline);g.setColor(CakeExtras.lyricFill());g.fill(outline);g.dispose();return new Raster(image,x/1920f,(x+fm.stringWidth(text))/1920f);
 }

 private static void clear(){for(int i=0;i<SLOTS;i++)cards[i]=null;previous="";lastPosition=-1;lastFrame=0;}
 public static void close(){for(int i=0;i<SLOTS;i++){if(textures[i]!=null){textures[i].close();textures[i]=null;}cards[i]=null;}previous="";}
}
