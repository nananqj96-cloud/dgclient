package fr.cake.client.mixin;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.gen.Accessor;import java.util.List;
@Mixin(ModelPart.class)
public interface ModelCubesAccess {@Accessor("cubes") List<ModelPart.Cube> cake$cubes();}
