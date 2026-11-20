/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.module.modules.FakeScoreboard
 *  net.minecraft.class_2561
 *  net.minecraft.class_329
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 */
package com.fakeclient.mixin;

import com.fakeclient.module.modules.FakeScoreboard;
import net.minecraft.class_2561;
import net.minecraft.class_329;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={class_329.class})
public class InGameHudMixin {
    @ModifyVariable(method={"method_1758"}, at=@At(value="HEAD"), argsOnly=true, ordinal=0)
    private class_2561 fakeclient$fakeBalanceOverlay(class_2561 message) {
        return FakeScoreboard.rewriteBalance((class_2561)message);
    }
}
