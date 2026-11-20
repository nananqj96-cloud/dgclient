package fr.cake.client;
import java.awt.*;import java.awt.geom.*;import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.renderer.RenderPipelines;
public final class CrosshairHud {
 public static final String[] NAMES={"Classic","Dot","Ring","Chevron","Diamond","Brackets","T-Cross","Orbit"};private static SmoothTexture texture;private static long revision=-1;
 public static boolean visible(){var mc=Minecraft.getInstance();return CakeClient.settings!=null&&CakeClient.settings.crosshairEnabled&&mc.player!=null&&!mc.player.isSpectator()&&mc.options.getCameraType().isFirstPerson()&&!mc.options.hideGui;}
 public static void render(GuiGraphics g){if(!visible())return;if(texture==null)texture=new SmoothTexture("crosshair69",256,256);if(revision!=Settings.revision){var s=CakeClient.settings;var im=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);var d=im.createGraphics();d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);float r=(float)(18+s.crosshairSize*68),gap=(float)(4+s.crosshairGap*36),w=(float)(2+s.crosshairThickness*9);var p=new Path2D.Float();int style=s.crosshairStyle;
 if(style==0||style==6){for(int a=0;a<4;a++){if(style==6&&a==3)continue;double t=a*Math.PI/2;p.moveTo(128+Math.cos(t)*gap,128+Math.sin(t)*gap);p.lineTo(128+Math.cos(t)*(gap+r*.5),128+Math.sin(t)*(gap+r*.5));}}
 else if(style==3){p.moveTo(128-r/2,128+r/3);p.lineTo(128,128-r/3);p.lineTo(128+r/2,128+r/3);}
 else if(style==4){p.moveTo(128,128-r/2);p.lineTo(128+r/2,128);p.lineTo(128,128+r/2);p.lineTo(128-r/2,128);p.closePath();}
 else if(style==5){for(int side:new int[]{-1,1}){p.moveTo(128+side*(gap+10),128-r/3);p.lineTo(128+side*gap,128-r/3);p.lineTo(128+side*gap,128+r/3);p.lineTo(128+side*(gap+10),128+r/3);}}
 Shape shape=(style==1)?new Ellipse2D.Float(124,124,8,8):(style==2||style==7)?new Ellipse2D.Float(128-r/2,128-r/2,r,r):p;
 d.setColor(new Color(0,0,0,210));d.setStroke(new BasicStroke(w+4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));d.draw(shape);d.setColor(Color.getHSBColor((float)s.crosshairHue,.5f,1));d.setStroke(new BasicStroke(w,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));d.draw(shape);if(style==1||style==7)d.fill(new Ellipse2D.Float(124,124,8,8));d.dispose();texture.update(im);revision=Settings.revision;}
 g.blit(RenderPipelines.GUI_TEXTURED,texture.id,g.guiWidth()/2-32,g.guiHeight()/2-32,0,0,64,64,256,256,256,256);
 }
}
