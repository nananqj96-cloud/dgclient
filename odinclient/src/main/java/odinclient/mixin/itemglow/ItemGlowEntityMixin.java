package odinclient.mixin.itemglow;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import odinclient.module.render.itemglow.ItemGlow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ItemGlowEntityMixin {
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void odinclient$makeDroppedItemGlow(
            Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ItemGlow.shouldGlowEntity(entity)) {
            cir.setReturnValue(true);
        }
    }
}
