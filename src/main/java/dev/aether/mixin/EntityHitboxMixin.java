package dev.aether.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Entity.class})
public class EntityHitboxMixin {
   @ModifyReturnValue(
      method = {"getBoundingBox"},
      at = {@At("RETURN")}
   )
   private AABB aetherclient$expandHitbox(AABB original) {
      return original;
   }
}
