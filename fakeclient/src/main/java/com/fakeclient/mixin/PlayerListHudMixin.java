/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.util.FakeOnline
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.class_355
 *  net.minecraft.class_640
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.util.FakeOnline;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_355;
import net.minecraft.class_640;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_355.class})
public class PlayerListHudMixin {
    private static final int TAB_LIMIT = 80;

    @Inject(method={"method_48213"}, at={@At(value="RETURN")}, cancellable=true, require=0)
    private void fakeclient$showPayers(CallbackInfoReturnable<List<class_640>> cir) {
        List<class_640> fakes = FakeOnline.entries();
        if (fakes.isEmpty()) {
            return;
        }
        List entries = (List)cir.getReturnValue();
        if (entries == null) {
            return;
        }
        ArrayList<class_640> out = new ArrayList<class_640>(entries);
        for (class_640 fake : fakes) {
            int at;
            if (out.contains(fake)) continue;
            for (at = 0; at < out.size() && PlayerListHudMixin.nameOf((class_640)out.get(at)).compareToIgnoreCase(PlayerListHudMixin.nameOf(fake)) < 0; ++at) {
            }
            out.add(at, fake);
        }
        for (int i = out.size() - 1; out.size() > 80 && i >= 0; --i) {
            if (fakes.contains(out.get(i))) continue;
            out.remove(i);
        }
        cir.setReturnValue(out);
    }

    private static String nameOf(class_640 entry) {
        GameProfile p = entry.method_2966();
        String n = p == null ? null : p.name();
        return n == null ? "" : n;
    }
}
