package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.movement.FreecamModule;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean thirdPerson;

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("RETURN"))
    private void dgclient$onCameraUpdate(
            BlockView area,
            Entity focusedEntity,
            boolean thirdPersonArg,
            boolean inverseView,
            float tickDelta,
            CallbackInfo ci
    ) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        FreecamModule freecam = inst.getModuleManager().get(FreecamModule.class);
        if (freecam != null && freecam.isEnabled()) {
            this.thirdPerson = true;
            Vec3d pos = freecam.getInterpolatedPos(tickDelta);
            this.setPos(pos.x, pos.y, pos.z);
            this.setRotation(freecam.getCamYaw(), freecam.getCamPitch());
        }
    }
}
