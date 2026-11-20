/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_2498
 *  net.minecraft.class_2680
 *  net.minecraft.class_4970$class_4971
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_2498;
import net.minecraft.class_2680;
import net.minecraft.class_4970;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_4970.class_4971.class})
public class BlockSoundGroupMixin {
    private static boolean fakeclient$resolving = false;

    @Inject(method={"method_26231"}, at={@At(value="RETURN")}, cancellable=true, require=0)
    private void fakeclient$swapSound(CallbackInfoReturnable<class_2498> cir) {
        if (fakeclient$resolving) {
            return;
        }
        fakeclient$resolving = true;
        try {
            class_2498 swapped = TextureSwaps.soundFor((class_2680)((class_2680)(Object)this));
            if (swapped != null) {
                cir.setReturnValue(swapped);
            }
        }
        finally {
            fakeclient$resolving = false;
        }
    }
}
