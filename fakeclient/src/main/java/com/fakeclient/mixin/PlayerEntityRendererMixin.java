/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeMedia
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_10055
 *  net.minecraft.class_1007
 *  net.minecraft.class_11659
 *  net.minecraft.class_11890
 *  net.minecraft.class_12075
 *  net.minecraft.class_1799
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_4587
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeMedia;
import com.fakeclient.render.TextureSwaps;
import net.minecraft.class_10055;
import net.minecraft.class_1007;
import net.minecraft.class_11659;
import net.minecraft.class_11890;
import net.minecraft.class_12075;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_4587;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1007.class})
public class PlayerEntityRendererMixin {
    private static boolean fakeclient$active() {
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeMedia m = mm == null ? null : mm.fakeMedia;
        return m != null && m.isEnabled();
    }

    @Inject(method={"method_74935"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fakeclient$labelSelf(class_11890 entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerEntityRendererMixin.fakeclient$active()) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null && entity == mc.field_1724) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method={"method_62604"}, at={@At(value="RETURN")}, require=0)
    private void fakeclient$swapWornChest(class_11890 entity, class_10055 state, float tickDelta, CallbackInfo ci) {
        class_1799 worn = state.field_53418;
        if (worn == null || worn.method_7960()) {
            return;
        }
        class_1799 swapped = TextureSwaps.itemFor((class_1799)worn);
        if (swapped != null) {
            state.field_53418 = swapped;
        }
    }

    @Inject(method={"method_4213"}, at={@At(value="HEAD")}, require=0)
    private void fakeclient$mediaNameplate(class_10055 state, class_4587 matrices, class_11659 queue, class_12075 camera, CallbackInfo ci) {
        if (!PlayerEntityRendererMixin.fakeclient$active() || state.field_53337 == null) {
            return;
        }
        class_310 mc = class_310.method_1551();
        if (mc.field_1724 != null && state.field_53528 == mc.field_1724.method_5628()) {
            state.field_53337 = FakeMedia.decorate((class_2561)state.field_53337);
        }
    }
}
