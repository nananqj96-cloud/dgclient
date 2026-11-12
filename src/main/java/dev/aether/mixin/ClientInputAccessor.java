package dev.aether.mixin;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientInput.class})
public interface ClientInputAccessor {
   @Accessor("keyPresses")
   void aetherclient$setKeyPresses(Input input);

   @Accessor("moveVector")
   void aetherclient$setMoveVector(Vec2 vec2);
}
