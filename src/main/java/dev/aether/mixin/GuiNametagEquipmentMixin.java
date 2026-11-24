package dev.aether.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class GuiNametagEquipmentMixin {
   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private void aetherclient$nametagEquipment(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      // World nametag equipment rendering stub - Anubis-internal API disabled
   }
}
