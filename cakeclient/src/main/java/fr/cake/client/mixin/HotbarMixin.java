package fr.cake.client.mixin;
import fr.cake.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class)
public abstract class HotbarMixin {
 @Inject(method="render",at=@At("TAIL"))
 private void cake$hud(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){var mc=Minecraft.getInstance();var s=CakeClient.settings;if(mc.options.hideGui)return;SpotifyHud.render(g,false);if(s!=null&&s.followEnabled&&mc.player!=null)g.drawString(mc.font,PlayerFollow.label(),8,g.guiHeight()-40,0xffd6bbff,true);}
 @Inject(method="renderItemHotbar",at=@At("HEAD"))
 private void cake$start(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){HotbarHud.begin(g);}
 @Inject(method="renderItemHotbar",at=@At("RETURN"))
 private void cake$end(GuiGraphics g,DeltaTracker delta,CallbackInfo ci){HotbarHud.end(g);}
 @Redirect(method="renderItemHotbar",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
 private void cake$background(GuiGraphics g,com.mojang.blaze3d.pipeline.RenderPipeline p,net.minecraft.resources.Identifier sprite,int x,int y,int w,int h){if(!HotbarHud.replaces(sprite.getPath()))g.blitSprite(p,sprite,x,y,w,h);}
}
