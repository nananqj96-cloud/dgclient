package fr.cake.client;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.resources.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.util.*;import java.io.*;
/** Immutable snapshot used by both individual resources and atlas enumeration. */
public final class PvpOverrides extends AbstractPackResources {
 private static volatile PvpOverrides active;
 private final Map<String,byte[]> files;
 private PvpOverrides(Map<String,byte[]> files){super(new PackLocationInfo("cake-pvp-runtime",Component.literal("Cake PvP"),PackSource.BUILT_IN,Optional.empty()));this.files=Map.copyOf(files);}
 public static void install(Map<String,byte[]> files){active=new PvpOverrides(files);}
 public static Optional<Resource> find(Identifier id){var pack=active;if(pack==null)return Optional.empty();var data=pack.files.get("assets/"+id.getNamespace()+"/"+id.getPath());if(data==null)return Optional.empty();return Optional.of(new Resource(pack,()->new ByteArrayInputStream(data),()->{byte[] meta=pack.files.get("assets/"+id.getNamespace()+"/"+id.getPath()+".mcmeta");return meta==null?ResourceMetadata.EMPTY:ResourceMetadata.fromJsonStream(new ByteArrayInputStream(meta));}));}
 public static void append(String path,java.util.function.Predicate<Identifier> filter,Map<Identifier,Resource> result){var pack=active;if(pack==null)return;for(var entry:pack.files.entrySet()){String n=entry.getKey();if(!n.startsWith("assets/minecraft/")||n.endsWith(".mcmeta"))continue;Identifier id=Identifier.withDefaultNamespace(n.substring(17));if(id.getPath().startsWith(path)&&filter.test(id))result.put(id,find(id).orElseThrow());}}
 @Override public IoSupplier<InputStream> getRootResource(String... path){byte[] b=files.get(String.join("/",path));return b==null?null:()->new ByteArrayInputStream(b);}
 @Override public IoSupplier<InputStream> getResource(PackType type,Identifier id){if(type!=PackType.CLIENT_RESOURCES)return null;byte[] b=files.get("assets/"+id.getNamespace()+"/"+id.getPath());return b==null?null:()->new ByteArrayInputStream(b);}
 @Override public Set<String> getNamespaces(PackType type){return Set.of("minecraft");}
 @Override public void listResources(PackType type,String namespace,String path,PackResources.ResourceOutput output){for(var e:files.entrySet()){String prefix="assets/"+namespace+"/";if(e.getKey().startsWith(prefix+path)){var id=Identifier.fromNamespaceAndPath(namespace,e.getKey().substring(prefix.length()));output.accept(id,()->new ByteArrayInputStream(e.getValue()));}}}
 @Override public void close(){}
}
