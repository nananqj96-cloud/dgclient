/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_11515
 *  net.minecraft.class_2248
 *  net.minecraft.class_2680
 *  net.minecraft.class_4696
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_11515;
import net.minecraft.class_2248;
import net.minecraft.class_2680;
import net.minecraft.class_4696;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4696.class})
public class BlockRenderLayersMixin {
    private static boolean fakeclient$resolving = false;

    @Inject(method={"method_23679"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private static void fakeclient$swapLayer(class_2680 state, CallbackInfoReturnable<class_11515> cir) {
        if (fakeclient$resolving) {
            return;
        }
        class_2248 to = TextureSwaps.blockFor((class_2680)state);
        if (to == null) {
            return;
        }
        fakeclient$resolving = true;
        try {
            cir.setReturnValue(class_4696.method_23679((class_2680)to.method_9564()));
        }
        finally {
            fakeclient$resolving = false;
        }
    }
}
