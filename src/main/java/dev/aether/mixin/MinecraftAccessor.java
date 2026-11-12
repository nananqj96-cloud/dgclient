package dev.aether.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({Minecraft.class})
public interface MinecraftAccessor {
   @Accessor("rightClickDelay")
   void aetherclient$setRightClickDelay(int i);

   @Invoker("startAttack")
   boolean aetherclient$startAttack();

   @Invoker("startUseItem")
   void aetherclient$startUseItem();
}
