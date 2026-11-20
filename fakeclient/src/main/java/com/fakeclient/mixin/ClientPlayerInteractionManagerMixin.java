/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.gui.SpawnerScreen
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeSpawner
 *  net.minecraft.class_1268
 *  net.minecraft.class_1269
 *  net.minecraft.class_2246
 *  net.minecraft.class_310
 *  net.minecraft.class_3965
 *  net.minecraft.class_437
 *  net.minecraft.class_636
 *  net.minecraft.class_746
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.gui.SpawnerScreen;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeSpawner;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_2246;
import net.minecraft.class_310;
import net.minecraft.class_3965;
import net.minecraft.class_437;
import net.minecraft.class_636;
import net.minecraft.class_746;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_636.class})
public class ClientPlayerInteractionManagerMixin {
    @Inject(method={"method_2896"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fakeclient$ghostSpawner(class_746 player, class_1268 hand, class_3965 hit, CallbackInfoReturnable<class_1269> cir) {
        FakeSpawner m;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeSpawner fakeSpawner = m = mm == null ? null : mm.fakeSpawner;
        if (m == null || !m.isEnabled() || player == null) {
            return;
        }
        if (!player.method_73183().method_8320(hit.method_17777()).method_27852(class_2246.field_10049)) {
            return;
        }
        class_310.method_1551().method_1507((class_437)new SpawnerScreen());
        cir.setReturnValue(class_1269.field_5812);
    }
}
