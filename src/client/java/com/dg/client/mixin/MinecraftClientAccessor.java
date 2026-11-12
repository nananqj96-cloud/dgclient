package com.dg.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Accessor("itemUseCooldown")
    int dgclient$getItemUseCooldown();

    @Accessor("itemUseCooldown")
    void dgclient$setItemUseCooldown(int cooldown);
}
