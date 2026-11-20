/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakePay
 *  com.fakeclient.module.modules.TextureChanger
 *  com.fakeclient.util.OnlineHarvest
 *  com.mojang.brigadier.suggestion.Suggestions
 *  net.minecraft.class_2639
 *  net.minecraft.class_2767
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakePay;
import com.fakeclient.module.modules.TextureChanger;
import com.fakeclient.util.OnlineHarvest;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.class_2639;
import net.minecraft.class_2767;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_634.class})
public class ClientPlayNetworkHandlerMixin {
    @Inject(method={"method_45730"}, at={@At(value="HEAD")}, cancellable=true)
    private void fakeclient$onSendChatCommand(String command, CallbackInfo ci) {
        FakePay module;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakePay fakePay = module = mm == null ? null : mm.fakePay;
        if (module != null && module.isEnabled() && module.handleWithNotify(command)) {
            ci.cancel();
        }
    }

    @Inject(method={"method_45729"}, at={@At(value="HEAD")}, cancellable=true)
    private void fakeclient$onSendChatMessage(String content, CallbackInfo ci) {
        FakePay module;
        ModuleManager mm = FakeClient.modulesOrNull();
        FakePay fakePay = module = mm == null ? null : mm.fakePay;
        if (module != null && module.isEnabled() && module.handleWithNotify(content)) {
            ci.cancel();
        }
    }

    @Inject(method={"method_11081"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fakeclient$onCommandSuggestions(class_2639 packet, CallbackInfo ci) {
        if (packet.comp_2262() != 8019712) {
            return;
        }
        OnlineHarvest.accept((Suggestions)packet.method_11397());
        ci.cancel();
    }

    @Inject(method={"method_11146"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void fakeclient$onPlaySound(class_2767 packet, CallbackInfo ci) {
        TextureChanger tc;
        ModuleManager mm = FakeClient.modulesOrNull();
        TextureChanger textureChanger = tc = mm == null ? null : mm.textureChanger;
        if (tc != null && tc.shouldSuppressEquipSound(packet.method_11894())) {
            ci.cancel();
        }
    }
}
