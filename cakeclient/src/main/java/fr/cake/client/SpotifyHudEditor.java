package fr.cake.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
public final class SpotifyHudEditor extends Screen {
 private final Screen parent;private boolean dragging;private double grabX,grabY;
 public SpotifyHudEditor(Screen parent){super(Component.literal("Déplacer Spotify"));this.parent=parent;}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void renderBackground(GuiGraphics g,int x,int y,float dt){}
 @Override public void render(GuiGraphics g,int x,int y,float dt){g.fill(0,0,width,height,0x35000000);SpotifyHud.render(g,true);var b=SpotifyHud.bounds(width,height);g.renderOutline((int)b.x()-2,(int)b.y()-2,(int)b.width()+4,(int)b.height()+4,0xffb490ed);g.drawCenteredString(font,"Glisse la carte · Échap pour enregistrer",width/2,height-28,0xffeee7fa);}
 @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){var b=SpotifyHud.bounds(width,height);if(e.button()==0&&b.contains(e.x(),e.y())){dragging=true;grabX=e.x()-b.x();grabY=e.y()-b.y();return true;}return false;}
 @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(!dragging)return false;SpotifyHud.move(e.x()-grabX,e.y()-grabY,width,height);return true;}
 @Override public boolean mouseReleased(MouseButtonEvent e){dragging=false;CakeClient.settings.save();return true;}
 @Override public void onClose(){CakeClient.settings.save();minecraft.setScreen(parent);}
}
