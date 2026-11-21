package odinclient.ui.draw;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public final class GuiGraphicsHolder {
    private static final ThreadLocal<GuiGraphics> current = new ThreadLocal<>();

    private GuiGraphicsHolder() {}

    public static void set(GuiGraphics context) {
        current.set(context);
    }

    public static GuiGraphics get() {
        return current.get();
    }

    public static void clear() {
        current.remove();
    }
}
