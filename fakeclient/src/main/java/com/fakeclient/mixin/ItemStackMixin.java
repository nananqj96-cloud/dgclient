/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fakeclient.FakeClient
 *  com.fakeclient.module.ModuleManager
 *  com.fakeclient.module.modules.FakeSpawner
 *  com.fakeclient.render.TextureSwaps
 *  net.minecraft.class_124
 *  net.minecraft.class_1799
 *  net.minecraft.class_1802
 *  net.minecraft.class_2561
 *  net.minecraft.class_5250
 *  net.minecraft.class_5251
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.fakeclient.mixin;

import com.fakeclient.FakeClient;
import com.fakeclient.module.ModuleManager;
import com.fakeclient.module.modules.FakeSpawner;
import com.fakeclient.render.TextureSwaps;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_124;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_5250;
import net.minecraft.class_5251;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_1799.class})
public class ItemStackMixin {
    private static final int SET_MOB_TYPE_BLUE = 2770632;
    private static final class_5251 CREATIVE_TAB_BLUE = class_5251.method_27718((class_124)class_124.field_1078);

    private boolean fakeclient$isFakeSpawner() {
        ModuleManager mm = FakeClient.modulesOrNull();
        FakeSpawner m = mm == null ? null : mm.fakeSpawner;
        class_1799 self = (class_1799)(Object)this;
        return m != null && m.isEnabled() && self.method_31574(class_1802.field_8095) && !TextureSwaps.isExempt((class_1799)self);
    }

    @Inject(method={"method_7964"}, at={@At(value="RETURN")}, cancellable=true)
    private void fakeclient$name(CallbackInfoReturnable<class_2561> cir) {
        if (this.fakeclient$isFakeSpawner()) {
            cir.setReturnValue(class_2561.method_43470((String)"Spawner").method_27692(class_124.field_1076));
            return;
        }
        class_2561 swapped = TextureSwaps.nameFor((class_1799)((class_1799)(Object)this));
        if (swapped != null) {
            cir.setReturnValue(swapped);
        }
    }

    @Inject(method={"method_7950"}, at={@At(value="RETURN")}, cancellable=true)
    private void fakeclient$tooltip(CallbackInfoReturnable<List<class_2561>> cir) {
        if (this.fakeclient$isFakeSpawner()) {
            ArrayList<class_2561> lines = new ArrayList<class_2561>();
            lines.add(class_2561.method_43470((String)"Spawner").method_27692(class_124.field_1076));
            lines.add(class_2561.method_43470((String)"Skeleton").method_27694(s -> s.method_36139(15925082)));
            lines.add(class_2561.method_43473());
            lines.add(class_2561.method_43470((String)"Interact with Spawn Egg:").method_27692(class_124.field_1080));
            lines.add(class_2561.method_43470((String)"Sets Mob Type").method_27694(s -> s.method_36139(2770632)));
            cir.setReturnValue(lines);
            return;
        }
        this.fakeclient$hideCreativeTabs(cir);
    }

    private void fakeclient$hideCreativeTabs(CallbackInfoReturnable<List<class_2561>> cir) {
        class_1799 self = (class_1799)(Object)this;
        if (TextureSwaps.nameFor((class_1799)self) == null) {
            return;
        }
        List<class_2561> lines = (List<class_2561>)cir.getReturnValue();
        if (lines == null || lines.size() < 2) {
            return;
        }
        ArrayList<class_2561> kept = new ArrayList<class_2561>(lines.size());
        for (class_2561 line : lines) {
            class_5251 color = line.method_10866().method_10973();
            if (color != null && color.equals((Object)CREATIVE_TAB_BLUE)) continue;
            kept.add(line);
        }
        if (kept.size() != lines.size()) {
            cir.setReturnValue(kept);
        }
    }
}
