package odinclient.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

import odinclient.ui.draw.GuiGraphicsHolder;

@Environment(EnvType.CLIENT)
public final class RectRenderer {
    public static final RectRenderer COLOR = new RectRenderer();

    public void begin() {}

    public void fillRect(double x, double y, double width, double height, Color color) {
        GuiGraphics context = GuiGraphicsHolder.get();
        if (context != null && color != null) {
            context.fill(
                    (int) Math.floor(x),
                    (int) Math.floor(y),
                    (int) Math.ceil(x + width),
                    (int) Math.ceil(y + height),
                    color.toArgb());
        }
    }

    public void render() {}
}
