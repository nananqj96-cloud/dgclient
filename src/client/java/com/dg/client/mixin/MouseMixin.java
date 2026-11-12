package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.MouseScrollEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void dgclient$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null || window != mc.getWindow().getHandle()) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            MouseScrollEvent event = inst.getEventBus().post(new MouseScrollEvent(horizontal, vertical));
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
