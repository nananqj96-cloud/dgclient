package dev.aether.mixin;

import dev.aether.AetherClient;
import dev.aether.module.ModuleManager;
import dev.aether.module.impl.SpeedMineModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockBehaviour.class})
public abstract class BlockBehaviourSpeedMineMixin {
   @Inject(
      method = {"getDestroyProgress"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void aetherclient$applySpeedMine(
      BlockState blockState, Player player, BlockGetter blockGetter, BlockPos blockPos, CallbackInfoReturnable<Float> callbackInfoReturnable
   ) {
      ModuleManager var6 = AetherClient.modules();
      if (var6 != null) {
         SpeedMineModule var7 = var6.speedMine;
         if (var7 != null && var7.isEnabled()) {
            callbackInfoReturnable.setReturnValue(var7.apply(callbackInfoReturnable.getReturnValueF()));
         }
      }
   }
}
