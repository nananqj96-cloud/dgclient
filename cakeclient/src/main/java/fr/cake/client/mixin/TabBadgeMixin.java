package fr.cake.client.mixin;
import fr.cake.client.CosmeticNetwork;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PlayerTabOverlay.class)
public abstract class TabBadgeMixin {
 @Inject(method="getNameForDisplay",at=@At("RETURN"),cancellable=true)
 private void cake$badge(PlayerInfo p,CallbackInfoReturnable<Component> ci){if(CosmeticNetwork.marked(p.getProfile().id()))ci.setReturnValue(ci.getReturnValue().copy().append(Component.literal(" [Cake Client]").withStyle(s->s.withColor(0xb48af4))));}
}
