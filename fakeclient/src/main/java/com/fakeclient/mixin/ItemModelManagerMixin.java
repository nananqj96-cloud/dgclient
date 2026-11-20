/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_10442
 *  net.minecraft.class_1799
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 */
package com.fakeclient.mixin;

import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_10442;
import net.minecraft.class_1799;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={class_10442.class})
public class ItemModelManagerMixin {
    @ModifyVariable(method={"method_65596"}, at=@At(value="HEAD"), argsOnly=true)
    private class_1799 fakeclient$swapStack(class_1799 stack) {
        class_1799 to = TextureSwaps.itemFor((class_1799)stack);
        return to == null ? stack : to;
    }
}
