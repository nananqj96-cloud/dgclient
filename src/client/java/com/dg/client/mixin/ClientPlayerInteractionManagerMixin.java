package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.HitGlowModule;
import com.dg.client.module.modules.world.FastUseBreakModule;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Accessor("blockBreakingCooldown")
    public abstract int dgclient$getBlockBreakingCooldown();

    @Accessor("blockBreakingCooldown")
    public abstract void dgclient$setBlockBreakingCooldown(int cooldown);

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void dgclient$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            HitGlowModule hitGlow = inst.getModuleManager().get(HitGlowModule.class);
            if (hitGlow != null) {
                hitGlow.recordHit(target);
            }
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void dgclient$onUpdateBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst != null && inst.isReady()) {
            FastUseBreakModule fast = inst.getModuleManager().get(FastUseBreakModule.class);
            if (fast != null && fast.isEnabled() && fast.fastBreak.getValue()) {
                if (dgclient$getBlockBreakingCooldown() > fast.breakDelayTicks.getValue()) {
                    dgclient$setBlockBreakingCooldown(fast.breakDelayTicks.getValue());
                }
            }
        }
    }
}
