package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.esp.EntityEspModule;
import com.dg.client.module.modules.esp.PlayerEspModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void dgclient$onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (self instanceof PlayerEntity p) {
            PlayerEspModule pe = inst.getModuleManager().get(PlayerEspModule.class);
            if (pe != null && pe.shouldShaderOutline(self)) {
                cir.setReturnValue(pe.getColorForPlayer(p) & 0x00FFFFFF);
                return;
            }
        }
        EntityEspModule ee = inst.getModuleManager().get(EntityEspModule.class);
        if (ee != null && ee.shouldShaderOutline(self)) {
            cir.setReturnValue(ee.classifyColor(self) & 0x00FFFFFF);
        }
    }
}
