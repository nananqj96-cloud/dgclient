package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.ScreenOpenEvent;
import com.dg.client.event.events.TickEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void dgclient$onPreTick(CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            inst.getEventBus().post(TickEvent.Pre.INSTANCE);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dgclient$onPostTick(CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            inst.getEventBus().post(TickEvent.Post.INSTANCE);
        }
    }

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void dgclient$onSetScreen(Screen screen, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            ScreenOpenEvent event = inst.getEventBus().post(new ScreenOpenEvent(screen));
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
