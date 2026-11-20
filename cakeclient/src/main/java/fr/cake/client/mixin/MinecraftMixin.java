package fr.cake.client.mixin;
import fr.cake.client.CakeClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
 @Inject(method="tick",at=@At("TAIL")) private void cake$tick(CallbackInfo ci) { CakeClient.tick(); }
 @Inject(method="close",at=@At("HEAD")) private void cake$close(CallbackInfo ci) { fr.cake.client.CakeClient.shutdown(); }
}
