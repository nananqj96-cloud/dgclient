package fr.cake.client.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Entity.class)
public abstract class FollowMovementMixin {
 @ModifyVariable(method="moveRelative",at=@At("HEAD"),argsOnly=true)
 private Vec3 cake$follow(Vec3 input){return fr.cake.client.PlayerFollow.direction(this,input);}
}
