package fr.cake.client.mixin;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(RenderType.class)
public interface RenderTypeInvoker {
 @Invoker("create") static RenderType cake$create(String name,RenderSetup setup){throw new AssertionError();}
}
