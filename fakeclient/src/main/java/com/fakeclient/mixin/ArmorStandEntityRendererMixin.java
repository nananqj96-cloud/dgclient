/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_1531
 *  net.minecraft.class_1799
 *  net.minecraft.class_877
 *  net.minecraft.class_9998
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_1531;
import net.minecraft.class_1799;
import net.minecraft.class_877;
import net.minecraft.class_9998;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_877.class})
public class ArmorStandEntityRendererMixin {
    @Inject(method={"method_62375"}, at={@At(value="RETURN")}, require=0)
    private void fakeclient$swapWornChest(class_1531 entity, class_9998 state, float tickDelta, CallbackInfo ci) {
        class_1799 worn = state.field_53418;
        if (worn == null || worn.method_7960()) {
            return;
        }
        class_1799 swapped = TextureSwaps.itemFor((class_1799)worn);
        if (swapped != null) {
            state.field_53418 = swapped;
        }
    }
}
