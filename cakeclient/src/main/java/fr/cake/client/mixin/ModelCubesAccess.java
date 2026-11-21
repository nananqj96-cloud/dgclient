package fr.cake.client.mixin;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for the {@code cubes} field of {@link ModelPart}.
 *
 * <p>The {@code @Accessor} requires a refMap to resolve at runtime. When no refMap is available
 * (production intermediary environment), the mixin fails to apply and the method would remain
 * abstract, causing {@link AbstractMethodError}. The default implementation returns an empty list.
 */
@Mixin(ModelPart.class)
public interface ModelCubesAccess {
   @Accessor("cubes")
   default List<ModelPart.Cube> cake$cubes() {
      return Collections.emptyList();
   }
}
