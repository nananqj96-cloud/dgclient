package odinclient.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

import odinclient.ui.scale.ScreenScaleCompat;

@Environment(EnvType.CLIENT)
public final class Render2DEvent {
    public GuiGraphics context;
    public float tickDelta;
    public int scaledWidth;
    public int scaledHeight;

    public Render2DEvent(GuiGraphics context, float tickDelta) {
        this.context = context;
        this.tickDelta = tickDelta;
        this.scaledWidth =
                ScreenScaleCompat.isScopeActive()
                        ? ScreenScaleCompat.getScaledWidth()
                        : context.getScaledWindowWidth();
        this.scaledHeight =
                ScreenScaleCompat.isScopeActive()
                        ? ScreenScaleCompat.getScaledHeight()
                        : context.getScaledWindowHeight();
    }
}
