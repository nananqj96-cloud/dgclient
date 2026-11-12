package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.misc.InventoryHelpersModule;
import com.dg.client.util.ChatUtil;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
    private void dgclient$onDropSelectedItem(boolean entireStack, CallbackInfoReturnable<Boolean> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            InventoryHelpersModule inv = inst.getModuleManager().get(InventoryHelpersModule.class);
            if (inv != null && inv.shouldBlockDrop()) {
                ChatUtil.warn("InvHelpers: Hotbar drop is locked!");
                cir.setReturnValue(false);
            }
        }
    }
}
