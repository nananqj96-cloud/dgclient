package fr.cake.client.mixin;
import fr.cake.client.CakeClient;
import net.minecraft.client.Options;
import net.minecraft.client.CloudStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Options.class)
public abstract class CloudsMixin {
 @Inject(method="getCloudsType",at=@At("HEAD"),cancellable=true)private void cake$clouds(CallbackInfoReturnable<CloudStatus> ci){if(CakeClient.settings!=null&&((CakeClient.settings.sky&&!CakeClient.settings.skyClouds)||fr.cake.client.RealisticMode.active()))ci.setReturnValue(CloudStatus.OFF);}
}
