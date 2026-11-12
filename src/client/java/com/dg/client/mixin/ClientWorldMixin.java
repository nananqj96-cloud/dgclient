package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.event.events.BlockUpdateEvent;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void dgclient$onHandleBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            ClientWorld self = (ClientWorld) (Object) this;
            BlockState old = self.getBlockState(pos);
            inst.getEventBus().post(new BlockUpdateEvent(pos, old, state));
        }
    }
}
