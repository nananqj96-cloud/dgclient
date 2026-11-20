package fr.cake.client.mixin;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import fr.cake.client.SpatialPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
public abstract class MouseMixin {
 @Inject(method="onButton",at=@At("HEAD"),cancellable=true)
 private void cake$button(long window,MouseButtonInfo info,int action,CallbackInfo ci){if(SpatialPanel.mouse(info.button(),action))ci.cancel();else if(action==org.lwjgl.glfw.GLFW.GLFW_PRESS)fr.cake.client.Knives.attack(info.button());}
}
