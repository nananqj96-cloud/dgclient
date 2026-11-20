package fr.cake.client;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

/** Native item pass, custom high resolution furniture. No inventory emulation. */
public final class HotbarHud {
 public static final String[] STYLES={"Glass","Cartes","Terminal","Obsidian"};
 private static SmoothTexture background,selection,offhand;private static long revision=-1,last;private static double selected;
 public static boolean active(){var mc=Minecraft.getInstance();return CakeClient.settings!=null&&CakeClient.settings.hotbarEnabled&&mc.player!=null&&!mc.player.isSpectator()&&!mc.options.hideGui;}
 public static boolean replaces(String path){return active()&&(path.equals("hud/hotbar")||path.equals("hud/hotbar_selection")||path.equals("hud/hotbar_offhand_left")||path.equals("hud/hotbar_offhand_right"));}
 public static void begin(GuiGraphics g){if(!active())return;var s=CakeClient.settings;g.pose().pushMatrix();float scale=(float)(.8+s.hotbarScale*.5);g.pose().translate(g.guiWidth()/2f,g.guiHeight());g.pose().scale(scale,scale);g.pose().translate(-g.guiWidth()/2f,-g.guiHeight()-(float)(s.hotbarLift*32));
  if(background==null){background=new SmoothTexture("hotbar_background",784,128);selection=new SmoothTexture("hotbar_selection",112,112);offhand=new SmoothTexture("hotbar_offhand",112,112);revision=-1;}
  if(revision!=Settings.revision){revision=Settings.revision;repaint();}
  long now=System.nanoTime();double dt=last==0?1:Math.min(.1,(now-last)/1e9);last=now;int slot=Minecraft.getInstance().player.getInventory().getSelectedSlot();selected+=(slot-selected)*(1-Math.exp(-dt*20));int x=g.guiWidth()/2-90,y=g.guiHeight()-22;
  g.blit(RenderPipelines.GUI_TEXTURED,background.id,x-8,y-8,0,0,196,32,784,128,784,128);
  g.pose().pushMatrix();g.pose().translate((float)(x+selected*20-4),y-4);g.blit(RenderPipelines.GUI_TEXTURED,selection.id,0,0,0,0,28,28,112,112,112,112);g.pose().popMatrix();
  var player=Minecraft.getInstance().player;if(!player.getOffhandItem().isEmpty()){boolean left=player.getMainArm()==net.minecraft.world.entity.HumanoidArm.RIGHT;g.blit(RenderPipelines.GUI_TEXTURED,offhand.id,left?x-31:x+183,y-4,0,0,28,28,112,112,112,112);}
 }
 public static void end(GuiGraphics g){if(active())g.pose().popMatrix();}
 private static Graphics2D draw(BufferedImage img){var a=img.createGraphics();a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);return a;}
 private static Color alpha(Color c,int a){return new Color(c.getRed(),c.getGreen(),c.getBlue(),Math.clamp(a,0,255));}
 private static void repaint(){var s=CakeClient.settings;Color accent=new Color(Color.HSBtoRGB((float)s.hotbarHue,.58f,1));int opacity=(int)(s.hotbarOpacity*245);var bg=new BufferedImage(784,128,BufferedImage.TYPE_INT_ARGB);var a=draw(bg);a.setColor(new Color(0,0,0,70));a.fillRoundRect(9,12,766,110,35,35);
  if(s.hotbarStyle==1){for(int i=0;i<9;i++){int x=31+i*80;a.setPaint(new GradientPaint(x,22,new Color(35,31,48,opacity),x,111,new Color(9,9,15,opacity)));a.fillRoundRect(x,22,76,88,15,15);a.setColor(alpha(accent,65));a.drawRoundRect(x,22,76,88,15,15);}}
  else if(s.hotbarStyle==2){a.setColor(new Color(4,16,14,opacity));a.fillRect(23,19,737,94);a.setStroke(new BasicStroke(2));a.setColor(alpha(accent,170));a.drawRect(23,19,737,94);for(int i=1;i<9;i++){a.setColor(alpha(accent,45));a.drawLine(30+i*80,22,30+i*80,108);}}
  else {int arc=s.hotbarStyle==3?7:34;a.setPaint(new GradientPaint(0,18,new Color(36,32,47,opacity),0,116,new Color(9,9,16,opacity)));a.fillRoundRect(21,18,742,96,arc,arc);a.setColor(alpha(accent,85));a.setStroke(new BasicStroke(2));a.drawRoundRect(21,18,742,96,arc,arc);a.setColor(new Color(255,255,255,22));a.drawLine(39,21,744,21);}
  a.dispose();background.update(bg);
  var sel=new BufferedImage(112,112,BufferedImage.TYPE_INT_ARGB);a=draw(sel);for(int i=6;i>0;i--){a.setColor(alpha(accent,7));a.fillRoundRect(9-i,8-i,94+i*2,97+i*2,20,20);}a.setPaint(new GradientPaint(0,8,alpha(accent,90),0,101,alpha(accent,13)));a.fillRoundRect(12,10,88,94,15,15);a.setStroke(new BasicStroke(3));a.setColor(alpha(accent,235));a.drawRoundRect(12,10,88,94,15,15);a.setColor(new Color(255,255,255,210));a.fillRoundRect(41,5,30,4,4,4);a.dispose();selection.update(sel);
  var off=new BufferedImage(112,112,BufferedImage.TYPE_INT_ARGB);a=draw(off);a.setColor(new Color(16,14,24,opacity));a.fillRoundRect(6,7,100,100,21,21);a.setStroke(new BasicStroke(2));a.setColor(alpha(accent,85));a.drawRoundRect(6,7,100,100,21,21);a.dispose();offhand.update(off);
 }
}
