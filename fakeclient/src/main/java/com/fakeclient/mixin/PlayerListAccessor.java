/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_634
 *  net.minecraft.class_640
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package com.fakeclient.mixin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.class_634;
import net.minecraft.class_640;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={class_634.class})
public interface PlayerListAccessor {
    @Accessor(value="field_3693")
    public Map<UUID, class_640> fakeclient$entries();

    @Accessor(value="field_40489")
    public Set<class_640> fakeclient$listed();
}
