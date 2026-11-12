package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.ViewmodelModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique
    private boolean dgclient$pushedViewmodel = false;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void dgclient$beforeRenderItem(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci
    ) {
        dgclient$pushedViewmodel = false;
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        ViewmodelModule vm = inst.getModuleManager().get(ViewmodelModule.class);
        if (vm == null || !vm.isEnabled()) {
            return;
        }
        if (hand == Hand.OFF_HAND && vm.hideOffhand.getValue()) {
            ci.cancel();
            return;
        }
        matrices.push();
        dgclient$pushedViewmodel = true;
        if (hand == Hand.MAIN_HAND) {
            matrices.translate(vm.mainX.getValue(), vm.mainY.getValue(), vm.mainZ.getValue());
            float s = vm.mainScale.getValue().floatValue();
            matrices.scale(s, s, s);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(vm.rotPitch.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(vm.rotYaw.getValue().floatValue()));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(vm.rotRoll.getValue().floatValue()));
        } else {
            matrices.translate(vm.offX.getValue(), vm.offY.getValue(), vm.offZ.getValue());
            float s = vm.offScale.getValue().floatValue();
            matrices.scale(s, s, s);
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void dgclient$afterRenderItem(
            AbstractClientPlayerEntity player,
            float tickProgress,
            float pitch,
            Hand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            CallbackInfo ci
    ) {
        if (dgclient$pushedViewmodel) {
            matrices.pop();
            dgclient$pushedViewmodel = false;
        }
    }
}
