package fr.cake.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

public final class ClientScreen extends Screen {
 public static final UiCanvas canvas=new UiCanvas();
 private final Screen parent;
 private final long opened=System.nanoTime();
 private long closing;
 private double left,top,drawWidth,drawHeight;
 public ClientScreen(Screen parent){super(Component.literal("Cake Client"));this.parent=parent;}
 @Override protected void init(){canvas.setSpatial(false);canvas.invalidate();sound("open",.45f);}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float dt){
  if(minecraft.level==null)renderPanorama(g,dt);
  renderBlurredBackground(g);
  // The client occupies only its compact rectangle; the rest of the scene remains visible.
 }
 @Override public void render(GuiGraphics g,int mx,int my,float dt){
  long now=System.nanoTime();
  if(closing!=0&&now-closing>210_000_000L){minecraft.setScreen(parent);return;}
  double p=Math.min(1,(now-opened)/360_000_000.);double ease=1-Math.pow(1-p,4);
  if(closing!=0)ease=1-Math.pow(Math.min(1,(now-closing)/210_000_000.),3);
  double scale=Math.min(width/1920.,height/1080.)*CakeClient.settings.menuScale;
  drawWidth=1920*scale;drawHeight=1080*scale;left=(width-drawWidth)/2;top=(height-drawHeight)/2-(1-ease)*(height+drawHeight)/2;
  canvas.update((mx-left)/scale,(my-top)/scale);
  var texture=canvas.texture();
  g.pose().pushMatrix();g.pose().translate((float)left,(float)top);g.pose().scale((float)scale,(float)scale);
  g.blit(RenderPipelines.GUI_TEXTURED,texture.id,0,0,0f,0f,1920,1080,1920,1080);g.pose().popMatrix();
 }
 private double x(double mouse){return (mouse-left)*1920/Math.max(1,drawWidth);}
 private double y(double mouse){return (mouse-top)*1080/Math.max(1,drawHeight);}
 @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){return closing==0&&event.button()==0&&canvas.click(x(event.x()),y(event.y()));}
 @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(event.button()==0){canvas.drag(x(event.x()),y(event.y()));return true;}return false;}
 @Override public boolean mouseReleased(MouseButtonEvent event){if(event.button()==0){canvas.release();return true;}return false;}
 @Override public void onClose(){closeAnimated();}
 public void closeAnimated(){if(closing==0){canvas.release();closing=System.nanoTime();CakeClient.settings.save();sound("close",.45f);}}
 @Override public void removed(){canvas.release();CakeClient.settings.save();var mouse=(fr.cake.client.mixin.MouseMotionAccess)(Object)minecraft.mouseHandler;mouse.cake$pendingX(0);mouse.cake$pendingY(0);minecraft.mouseHandler.setIgnoreFirstMove();}
 public static void tap(){sound("tap",.35f);}
 private static void sound(String name,float volume){if(CakeClient.settings!=null&&CakeClient.settings.sounds)Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("cakeclient",name)),1,volume));}
}
