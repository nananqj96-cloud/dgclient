package odinclient.mixin.fake;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.sounds.BlockSoundGroup;

import odinclient.module.fake.FakeSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class FakeSpawnerSoundMixin {
    @Inject(method = "getSoundGroup", at = @At("HEAD"), cancellable = true, require = 0)
    private void odinclient$spawnerSound(
            BlockState state, CallbackInfoReturnable<BlockSoundGroup> cir) {
        if (FakeSpawner.isEnabled() && (Object) this == Blocks.YELLOW_STAINED_GLASS) {
            cir.setReturnValue(Blocks.SPAWNER.getDefaultState().getSoundGroup());
        }
    }
}
