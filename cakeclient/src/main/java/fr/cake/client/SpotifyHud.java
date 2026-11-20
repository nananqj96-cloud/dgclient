package fr.cake.client;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/** One cached antialiased card. Only the progress strip and meter move between paints. */
public final class SpotifyHud {
 private static final int W=840,H=212;
 private static final BufferedImage IMAGE=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
 private static SmoothTexture texture;
 private static String key="";
 private static BufferedImage cover;
 private static long revision=-1;
 private static boolean paused;
 public record Bounds(double x,double y,double width,double height) {public boolean contains(double px,double py){return px>=x&&py>=y&&px<=x+width&&py<=y+height;}}
 public static Bounds bounds(int width,int height){var s=CakeClient.settings;double scale=Math.min(width/1920.,height/1080.)*(.7+s.musicScale*.8);double w=420*scale,h=106*scale;return new Bounds(s.musicX*Math.max(0,width-w),s.musicY*Math.max(0,height-h),w,h);}
 public static void move(double x,double y,int width,int height){var b=bounds(width,height);var s=CakeClient.settings;s.musicX=Math.clamp(x/Math.max(1,width-b.width),0,1);s.musicY=Math.clamp(y/Math.max(1,height-b.height),0,1);}
 public static void render(GuiGraphics g,boolean editor){
  var s=CakeClient.settings;if(s==null||(!s.visualizer&&!editor))return;var media=LyricsService.media();if(media==null&&!editor)return;
  String next=media==null?"Aucun morceau en cours":media.key();var art=LyricsService.artwork();boolean pause=media==null||!media.playing();
  if(texture==null){texture=new SmoothTexture("spotify_card",W,H);revision=-1;}
  if(!next.equals(key)||art!=cover||revision!=Settings.revision||paused!=pause){key=next;cover=art;paused=pause;revision=Settings.revision;paint(media,art);texture.update(IMAGE);}
  var b=bounds(g.guiWidth(),g.guiHeight());float scale=(float)(b.width/W);g.pose().pushMatrix();g.pose().translate((float)b.x,(float)b.y);g.pose().scale(scale,scale);g.blit(RenderPipelines.GUI_TEXTURED,texture.id,0,0,0,0,W,H,W,H);
  int accent=0xff000000|(Color.HSBtoRGB((float)s.visualizerHue,.75f,1)&0xffffff);double progress=media==null||media.duration()<=0?0:Math.clamp(media.currentPosition()/(double)media.duration(),0,1);
  g.fill(222,168,600,176,0xff35323a);g.fill(222,168,222+(int)(378*progress),176,accent);
  float level=SpotifyVisualizer.level();if(s.musicBars)for(int i=0;i<4;i++){int bar=pause?4:5+(int)(level*(12+i*4));g.fill(220+i*9,49-bar,225+i*9,49,accent);}
  g.pose().popMatrix();
  // Time is a tiny cached strip refreshed once per second, never an 840px repaint per frame.
  renderTime(g,b,media);
 }
 private static SmoothTexture timeTexture;private static long timeKey=Long.MIN_VALUE;private static String durationKey="";
 private static void renderTime(GuiGraphics g,Bounds b,LyricsService.Media m){long seconds=m==null?0:m.currentPosition()/1000;String end=time(m==null?0:m.duration());if(timeTexture==null)timeTexture=new SmoothTexture("spotify_time",210,52);if(seconds!=timeKey||!end.equals(durationKey)){timeKey=seconds;durationKey=end;BufferedImage img=new BufferedImage(210,52,BufferedImage.TYPE_INT_ARGB);var a=img.createGraphics();quality(a);a.setFont(UiCanvas.font(27,false));a.setColor(new Color(190,189,199));a.drawString(time(seconds*1000)+" / "+end,3,35);a.dispose();timeTexture.update(img);}float scale=(float)(b.width/W);g.pose().pushMatrix();g.pose().translate((float)b.x,(float)b.y);g.pose().scale(scale,scale);g.blit(RenderPipelines.GUI_TEXTURED,timeTexture.id,616,143,0,0,210,52,210,52);g.pose().popMatrix();}
 public static String time(long ms){long t=Math.max(0,ms)/1000;return (t/60)+":"+String.format(java.util.Locale.ROOT,"%02d",t%60);}
 private static void quality(Graphics2D a){a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);a.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);a.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
 private static void paint(LyricsService.Media media,BufferedImage artwork){var s=CakeClient.settings;var a=IMAGE.createGraphics();a.setComposite(AlphaComposite.Clear);a.fillRect(0,0,W,H);a.setComposite(AlphaComposite.SrcOver);quality(a);a.setColor(new Color(13,13,17,(int)(s.musicOpacity*255)));a.fillRoundRect(0,0,W,H,22,22);a.setColor(new Color(255,255,255,24));a.drawRoundRect(1,1,W-3,H-3,22,22);
  Shape clip=a.getClip();a.setClip(new RoundRectangle2D.Float(14,14,180,180,15,15));if(s.musicArtwork&&artwork!=null)a.drawImage(artwork,14,14,180,180,null);else{a.setPaint(new GradientPaint(14,14,new Color(91,52,150),194,194,new Color(26,19,42)));a.fillRect(14,14,180,180);a.setColor(new Color(239,227,255));a.setFont(UiCanvas.font(78,true));a.drawString("♪",65,133);}a.setClip(clip);
  a.setColor(new Color(245,245,248));a.setFont(UiCanvas.font(35,true));a.drawString(ellipsis(a,media==null?"Spotify · en attente":media.title(),535),267,52);a.setFont(UiCanvas.font(30,false));a.setColor(new Color(177,177,190));a.drawString(ellipsis(a,media==null?"Lance un morceau sur ton PC":media.artist(),570),222,111);if(media!=null&&!media.playing()){a.setFont(UiCanvas.font(20,true));a.setColor(new Color(159,130,210));a.drawString("EN PAUSE",222,144);}a.dispose();
 }
 private static String ellipsis(Graphics2D a,String t,int width){if(a.getFontMetrics().stringWidth(t)<=width)return t;while(!t.isEmpty()&&a.getFontMetrics().stringWidth(t+"…")>width)t=t.substring(0,t.length()-1);return t+"…";}
 public static void close(){if(texture!=null){texture.close();texture=null;}if(timeTexture!=null){timeTexture.close();timeTexture=null;}}
}
