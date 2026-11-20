/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakePay
 *  net.minecraft.class_2535
 *  net.minecraft.class_2596
 *  net.minecraft.class_2797
 *  net.minecraft.class_7472
 *  net.minecraft.class_9449
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakePay;
import net.minecraft.class_2535;
import net.minecraft.class_2596;
import net.minecraft.class_2797;
import net.minecraft.class_7472;
import net.minecraft.class_9449;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_2535.class})
public class ClientConnectionMixin {
    @Inject(method={"method_10743"}, at={@At(value="HEAD")}, cancellable=true)
    private void fakeclient$onSend(class_2596<?> packet, CallbackInfo ci) {
        String message;
        FakePay module;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakePay fakePay = module = mm == null ? null : mm.fakePay;
        if (module == null || !module.isEnabled()) {
            return;
        }
        if (packet instanceof class_7472) {
            class_7472 p = (class_7472)packet;
            message = p.comp_808();
        } else if (packet instanceof class_9449) {
            class_9449 p = (class_9449)packet;
            message = p.comp_2532();
        } else if (packet instanceof class_2797) {
            class_2797 p = (class_2797)packet;
            message = p.comp_945();
        } else {
            return;
        }
        if (module.matches(message)) {
            ci.cancel();
        }
    }
}
