package fr.cake.client.mixin;
import fr.cake.client.Knives;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class KnifeRenderMixin {
 @Inject(method="renderLevel",at=@At("TAIL"))private void cake$knife(CallbackInfo ci){Knives.render();fr.cake.client.PanelBenchmark.frame();fr.cake.client.FpsBenchmark.frame();}
}
