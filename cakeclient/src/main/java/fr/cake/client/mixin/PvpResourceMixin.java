package fr.cake.client.mixin;
import fr.cake.client.PvpOverrides;
import net.minecraft.server.packs.resources.*;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;import java.util.function.Predicate;
@Mixin(FallbackResourceManager.class)
public class PvpResourceMixin {
 @Shadow @Final private PackType type;@Shadow @Final private String namespace;
 @Inject(method="getResource",at=@At("HEAD"),cancellable=true)
 private void cake$override(Identifier id,CallbackInfoReturnable<Optional<Resource>> ci){if(type==PackType.CLIENT_RESOURCES)PvpOverrides.find(id).ifPresent(r->ci.setReturnValue(Optional.of(r)));}
 @Inject(method="getResourceStack",at=@At("RETURN"),cancellable=true)
 private void cake$stack(Identifier id,CallbackInfoReturnable<List<Resource>> ci){if(type!=PackType.CLIENT_RESOURCES)return;PvpOverrides.find(id).ifPresent(r->{var out=new ArrayList<>(ci.getReturnValue());out.add(r);ci.setReturnValue(out);});}
 @Inject(method="listResourceStacks",at=@At("RETURN"),cancellable=true)
 private void cake$stacks(String path,Predicate<Identifier> filter,CallbackInfoReturnable<Map<Identifier,List<Resource>>> ci){if(type!=PackType.CLIENT_RESOURCES||!namespace.equals("minecraft"))return;var out=new HashMap<>(ci.getReturnValue());Map<Identifier,Resource> extra=new HashMap<>();PvpOverrides.append(path,filter,extra);for(var e:extra.entrySet()){var list=new ArrayList<>(out.getOrDefault(e.getKey(),List.of()));list.add(e.getValue());out.put(e.getKey(),list);}ci.setReturnValue(out);}
 @Inject(method="listResources",at=@At("RETURN"),cancellable=true)
 private void cake$list(String path,Predicate<Identifier> filter,CallbackInfoReturnable<Map<Identifier,Resource>> ci){if(type!=PackType.CLIENT_RESOURCES||!namespace.equals("minecraft"))return;Map<Identifier,Resource> out=new HashMap<>(ci.getReturnValue());PvpOverrides.append(path,filter,out);ci.setReturnValue(out);}
}
