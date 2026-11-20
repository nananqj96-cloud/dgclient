package fr.cake.client.mixin;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;import net.minecraft.world.entity.player.Player;import net.minecraft.world.entity.Entity;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MultiPlayerGameMode.class)public abstract class HitParticleMixin{
 @Inject(method="attack",at=@At("HEAD"))private void cake$attack(Player p,Entity target,CallbackInfo ci){fr.cake.client.HitParticles.attack(target);fr.cake.client.CombatEffects.attack(target);}
}
