package com.dg.client.mixin;

import com.dg.client.DGClient;
import com.dg.client.module.modules.render.BetterTabListModule;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.util.ColorUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true, require = 0)
    private void dgclient$onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        BetterTabListModule mod = inst.getModuleManager().get(BetterTabListModule.class);
        if (mod != null && mod.isEnabled()) {
            cir.setReturnValue(mod.decorateName(entry, cir.getReturnValue()));
        }
    }

    @Inject(method = "renderLatencyIcon", at = @At("HEAD"), cancellable = true, require = 0)
    private void dgclient$onRenderLatencyIcon(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        DGClient inst = DGClient.getInstance();
        if (inst == null || !inst.isReady()) {
            return;
        }
        BetterTabListModule mod = inst.getModuleManager().get(BetterTabListModule.class);
        if (mod != null && mod.isEnabled() && mod.numericPing.getValue()) {
            int ping = Math.max(0, entry.getLatency());
            String label = ping + "ms";
            int color = ping < 80 ? ColorUtil.NEON_GREEN : (ping < 160 ? ColorUtil.MINT_GREEN : ColorUtil.PURE_WHITE);
            float lw = DGFontRenderer.getWidth(label, false, 0.72f);
            DGFontRenderer.drawScaledText(context, label, x + width - lw - 1.0f, y + 1.0f, color, false, 0.72f, true);
            ci.cancel();
        }
    }
}
