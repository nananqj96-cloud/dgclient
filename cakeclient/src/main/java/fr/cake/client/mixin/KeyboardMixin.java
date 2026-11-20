package fr.cake.client.mixin;
import fr.cake.client.CakeClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
 @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
 private void cake$key(long window,int action,KeyEvent key,CallbackInfo ci) {
  if(fr.cake.client.MenuKey.capture(key.key(),action)){ci.cancel();return;}
  var mc=net.minecraft.client.Minecraft.getInstance();
  if(CakeClient.settings!=null&&key.key()==CakeClient.settings.menuKey&&action==GLFW.GLFW_PRESS&&(mc.screen==null||mc.screen instanceof fr.cake.client.ClientScreen||mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen||mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen)){
   if((key.modifiers()&GLFW.GLFW_MOD_SHIFT)!=0&&CakeClient.settings.spatial&&(mc.screen==null||mc.screen instanceof fr.cake.client.ClientScreen)){fr.cake.client.SpatialPanel.placeFromMenu();ci.cancel();}else if(CakeClient.toggleMenu())ci.cancel();return;
  }
  if(key.key()==GLFW.GLFW_KEY_V&&action==GLFW.GLFW_PRESS&&CakeClient.settings!=null&&CakeClient.settings.followEnabled&&mc.screen==null){fr.cake.client.PlayerFollow.select();ci.cancel();return;}
  if(fr.cake.client.Knives.keyEvent(key.key(),action))ci.cancel();
 }
}
