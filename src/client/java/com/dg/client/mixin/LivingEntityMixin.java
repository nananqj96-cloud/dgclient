package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.ViewmodelModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void dgclient$modifySwingDuration(CallbackInfoReturnable<Integer> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || (Object) this != mc.player) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        ViewmodelModule vm = inst.getModuleManager().get(ViewmodelModule.class);
        if (vm != null && vm.isEnabled()) {
            int scaled = Math.max(1, (int) Math.round(cir.getReturnValueI() * vm.swingDurationMult.getValue()));
            cir.setReturnValue(scaled);
        }
    }
}
