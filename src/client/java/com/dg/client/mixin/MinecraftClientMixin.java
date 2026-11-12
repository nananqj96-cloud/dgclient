package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.ScreenOpenEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.modules.esp.EntityEspModule;
import com.dg.client.module.modules.esp.PlayerEspModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void dgclient$onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        PlayerEspModule pe = inst.getModuleManager().get(PlayerEspModule.class);
        if (pe != null && pe.shouldShaderOutline(entity)) {
            cir.setReturnValue(true);
            return;
        }
        EntityEspModule ee = inst.getModuleManager().get(EntityEspModule.class);
        if (ee != null && ee.shouldShaderOutline(entity)) {
            cir.setReturnValue(true);
        }
    }
}
