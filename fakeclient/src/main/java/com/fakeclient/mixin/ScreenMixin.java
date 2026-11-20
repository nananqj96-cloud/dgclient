/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_437;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_437.class})
public class ScreenMixin {
    @Inject(method={"method_25408"}, at={@At(value="RETURN")}, cancellable=true, require=0)
    private static void fakeclient$trimSwappedTooltip(class_310 client, class_1799 stack, CallbackInfoReturnable<List<class_2561>> cir) {
        String plain;
        if (stack == null || stack.method_7960()) {
            return;
        }
        if (TextureSwaps.nameFor((class_1799)stack) == null) {
            return;
        }
        List lines = (List)cir.getReturnValue();
        if (lines == null || lines.size() < 2) {
            return;
        }
        ArrayList<class_2561> kept = new ArrayList<class_2561>();
        kept.add((class_2561)lines.get(0));
        for (int i = 1; i < lines.size() && !(plain = ((class_2561)lines.get(i)).getString()).isEmpty() && !plain.startsWith("When "); ++i) {
            kept.add((class_2561)lines.get(i));
        }
        cir.setReturnValue(kept);
    }
}
