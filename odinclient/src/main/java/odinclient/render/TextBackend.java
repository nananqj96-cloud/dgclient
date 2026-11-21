package odinclient.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import odinclient.ui.draw.GuiGraphicsHolder;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public interface TextBackend {
    TextBackend INSTANCE =
            new TextBackend() {
                @Override
                public void begin() {}

                @Override
                public double getWidth(String text) {
                    return this.getWidth(text, false);
                }

                @Override
                public double getWidth(String text, boolean shadow) {
                    return Minecraft.getInstance().textRenderer.getWidth(text);
                }

                @Override
                public double getHeight(boolean shadow) {
                    Objects.requireNonNull(Minecraft.getInstance().textRenderer);
                    return 9.0;
                }

                @Override
                public double render(String text, double x, double y, Color color) {
                    return this.render(text, x, y, color, false);
                }

                @Override
                public double render(String text, double x, double y, Color color, boolean shadow) {
                    GuiGraphics context = GuiGraphicsHolder.get();
                    if (context != null) {
                        context.drawText(
                                Minecraft.getInstance().textRenderer,
                                text,
                                (int) Math.round(x),
                                (int) Math.round(y),
                                color.toArgb(),
                                shadow);
                    }

                    return x + this.getWidth(text, shadow);
                }

                @Override
                public void end() {}
            };

    static TextBackend get() {
        return INSTANCE;
    }

    void begin();

    double getWidth(String text);

    double getWidth(String text, boolean shadow);

    double getHeight(boolean shadow);

    double render(String text, double x, double y, Color color);

    double render(String text, double x, double y, Color color, boolean shadow);

    void end();
}
