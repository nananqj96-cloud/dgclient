/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeItemCount
 *  net.minecraft.class_332
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeItemCount;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={class_332.class})
public class DrawContextMixin {
    @Redirect(method={"method_64859"}, at=@At(value="INVOKE", target="Ljava/lang/String;valueOf(I)Ljava/lang/String;"))
    private String fakeclient$fakeCount(int count) {
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeItemCount m = mm == null ? null : mm.fakeItemCount;
        return m != null && m.isEnabled() ? String.valueOf(m.fake(count)) : String.valueOf(count);
    }
}
