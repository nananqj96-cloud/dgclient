package fr.cake.client.mixin;
import net.minecraft.client.gui.Gui;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.client.DeltaTracker;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)public class CrosshairMixin{
 @Inject(method="renderCrosshair",at=@At("HEAD"),cancellable=true)private void cake$crosshair(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){if(fr.cake.client.CrosshairHud.visible()){fr.cake.client.CrosshairHud.render(g);ci.cancel();}}
}
