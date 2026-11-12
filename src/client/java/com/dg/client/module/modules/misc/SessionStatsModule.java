package com.dg.client.module.modules.misc;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
import com.dg.client.event.events.ChunkDataEvent;
import com.dg.client.event.events.PacketEvent;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.TickEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render2DUtil;
import com.dg.client.render.font.DGFontRenderer;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.util.ColorUtil;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Session Stats: tracks session playtime, distance traveled, blocks mined,
 * totems popped, and chunks scanned, rendered as a clean neon green/white card.
 */
public final class SessionStatsModule extends Module {
    public final BooleanSetting renderCard = addSetting(new BooleanSetting(
            "Overlay Card", "Draws live session statistics card on screen", true));

    private final long sessionStartMs = System.currentTimeMillis();
    private double distanceMeters = 0.0;
    private Vec3d lastPos = null;
    private final AtomicInteger blocksMined = new AtomicInteger();
    private final AtomicInteger totemsPopped = new AtomicInteger();
    private final AtomicInteger chunksObserved = new AtomicInteger();

    public SessionStatsModule() {
        super("SessionStats", "Tracks playtime, distance, blocks mined, totems popped & chunks", ModuleCategory.MISC);
    }

    public void incrementSelfTotemPop() {
        totemsPopped.incrementAndGet();
    }

    @Override
    public String getDisplayInfo() {
        long mins = (System.currentTimeMillis() - sessionStartMs) / 60000L;
        return mins + "m";
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) {
            lastPos = null;
            return;
        }
        Vec3d cur = mc.player.getPos();
        if (lastPos != null) {
            double d = cur.distanceTo(lastPos);
            if (d > 0.001 && d < 90.0) {
                distanceMeters += d;
            }
        }
        lastPos = cur;
    }

    @EventHandler
    public void onChunkLoad(ChunkDataEvent.Load event) {
        chunksObserved.incrementAndGet();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getPacket() instanceof PlayerActionC2SPacket act
                && act.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) {
            blocksMined.incrementAndGet();
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!renderCard.getValue() || mc.options.hudHidden) {
            return;
        }
        long elapsedSec = (System.currentTimeMillis() - sessionStartMs) / 1000L;
        String timeStr = String.format(Locale.ROOT, "%02d:%02d:%02d",
                elapsedSec / 3600L, (elapsedSec % 3600L) / 60L, elapsedSec % 60L);
        String distStr = distanceMeters >= 1000.0
                ? String.format(Locale.ROOT, "%.2f km", distanceMeters / 1000.0)
                : String.format(Locale.ROOT, "%.0f m", distanceMeters);

        String[] rows = {
                "Playtime: " + timeStr,
                "Distance: " + distStr,
                "Blocks Mined: " + blocksMined.get(),
                "Totems Popped: " + totemsPopped.get(),
                "Chunks Seen: " + chunksObserved.get()
        };

        float w = 136.0f;
        float h = 20.0f + rows.length * 11.0f;
        float x = 8.0f;
        float y = event.getScreenHeight() - h - 42.0f;

        Render2DUtil.drawGaussianGlow(event.getContext().getMatrices(), x, y, w, h, 5.0f, 6.0f, ColorUtil.NEON_GREEN, 0.55f);
        Render2DUtil.drawRoundedRect(event.getContext().getMatrices(), x, y, w, h, 5.0f, ColorUtil.PANEL_DARK);
        Render2DUtil.drawRoundedOutline(event.getContext().getMatrices(), x, y, w, h, 5.0f, 1.0f, ColorUtil.BORDER_SUBTLE);
        DGFontRenderer.drawScaledText(event.getContext(), "SESSION STATS", x + 7.0f, y + 5.0f, ColorUtil.NEON_GREEN, true, 0.82f, false);

        float ry = y + 17.0f;
        for (String r : rows) {
            DGFontRenderer.drawScaledText(event.getContext(), r, x + 7.0f, ry, ColorUtil.PURE_WHITE, false, 0.78f, false);
            ry += 11.0f;
        }
    }
}
