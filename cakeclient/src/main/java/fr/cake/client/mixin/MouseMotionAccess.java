package fr.cake.client.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Discard pointer movement made inside Cake's menu before the mouse is grabbed again. */
@Mixin(MouseHandler.class)
public interface MouseMotionAccess {
 @Accessor("accumulatedDX") void cake$pendingX(double value);
 @Accessor("accumulatedDY") void cake$pendingY(double value);
}
