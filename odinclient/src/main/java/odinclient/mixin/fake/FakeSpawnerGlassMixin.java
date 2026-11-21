package odinclient.mixin.fake;

import net.minecraft.block.Block;
import net.minecraft.block.RenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import odinclient.module.fake.FakeSpawner;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class FakeSpawnerGlassMixin {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void odinclient$hideSpawnerGlass(
            BlockState state, CallbackInfoReturnable<RenderType> cir) {
        if ((Object) this == Blocks.YELLOW_STAINED_GLASS && FakeSpawner.isEnabled()) {
            cir.setReturnValue(RenderType.INVISIBLE);
        }
    }
}
