package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.misc.InventoryHelpersModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Inject(method = "drawMouseoverTooltip", at = @At("RETURN"))
    private void dgclient$onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (this.focusedSlot == null || !this.focusedSlot.hasStack()) {
            return;
        }
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            InventoryHelpersModule inv = inst.getModuleManager().get(InventoryHelpersModule.class);
            if (inv != null) {
                inv.renderShulkerPreviewIfPresent(context, this.focusedSlot.getStack(), x, y);
            }
        }
    }
}
