/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_1087
 *  net.minecraft.class_2248
 *  net.minecraft.class_2680
 *  net.minecraft.class_773
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_1087;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_773;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_773.class})
public class BlockModelsMixin {
    private static boolean fakeclient$resolving = false;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"method_3335"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fakeclient$swapModel(class_2680 state, CallbackInfoReturnable<class_1087> cir) {
        if (fakeclient$resolving) {
            return;
        }
        class_2248 to = TextureSwaps.blockFor((class_2680)state);
        if (to == null) {
            return;
        }
        fakeclient$resolving = true;
        try {
            cir.setReturnValue(((class_773)(Object)this).method_3335(to.method_9564()));
        }
        finally {
            fakeclient$resolving = false;
        }
    }
}
