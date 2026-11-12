package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void dgclient$onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int key = input.getKeycode();
        if (mc == null || mc.getWindow() == null || window != mc.getWindow().getHandle() || key == GLFW.GLFW_KEY_UNKNOWN) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            KeyEvent event = inst.getEventBus().post(new KeyEvent(key, input.scancode(), action, input.modifiers()));
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
