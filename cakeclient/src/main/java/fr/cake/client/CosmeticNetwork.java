package fr.cake.client;
import fr.cake.protocol.CosmeticState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class CosmeticNetwork {
 public record Packet(byte[] data)implements CustomPacketPayload{
  public static final Type<Packet> TYPE=new Type<>(Identifier.fromNamespaceAndPath("cakeclient","cosmetics"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Packet> CODEC=StreamCodec.of((b,p)->b.writeBytes(p.data),(b)->{if(b.readableBytes()>CosmeticState.MAX_BYTES)throw new IllegalArgumentException("Oversized state");byte[] a=new byte[b.readableBytes()];b.readBytes(a);return new Packet(a);});
  public Type<? extends CustomPacketPayload> type(){return TYPE;}
 }
 private static final Map<UUID,CosmeticState> peers=new HashMap<>();private static int ticks;private static Object connection;private static CosmeticState last;private static boolean shared;
 public static CosmeticState local(){var s=CakeClient.settings;return new CosmeticState((s.knives?1:0)|(s.trailEnabled?2:0)|(s.jumpEnabled?4:0)|(s.skinEnabled?8:0)|(s.backEnabled?16:0),s.knifeModel,s.knifeFinish,s.trailStyle,s.jumpStyle,s.avatarSkin,(float)s.trailHue,(float)s.jumpHue,(float)s.trailSize,(float)s.trailLife,(float)s.trailDensity,(float)s.jumpSize,(float)s.jumpLife,(float)s.skinScale,s.backStyle,(float)s.backScale,(float)s.backSpeed);}
 public static void init(){PayloadTypeRegistry.playC2S().register(Packet.TYPE,Packet.CODEC);PayloadTypeRegistry.playS2C().register(Packet.TYPE,Packet.CODEC);ClientPlayNetworking.registerGlobalReceiver(Packet.TYPE,(packet,context)->{try{var m=CosmeticState.decode(packet.data);context.client().execute(()->{if(m.state()==null)peers.remove(m.player());else peers.put(m.player(),m.state());});}catch(Exception ignored){}});ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{peers.clear();last=null;connection=null;});}
 public static CosmeticState get(UUID id){var mc=Minecraft.getInstance();return mc.player!=null&&id.equals(mc.player.getUUID())?local():peers.get(id);}
 public static boolean marked(UUID id){var mc=Minecraft.getInstance();return mc.player!=null&&id.equals(mc.player.getUUID())||peers.containsKey(id);}
 public static void tick(){var mc=Minecraft.getInstance();if(connection!=mc.getConnection()){peers.clear();last=null;shared=false;connection=mc.getConnection();}if(mc.player==null||connection==null||++ticks%20!=0||!ClientPlayNetworking.canSend(Packet.TYPE))return;boolean share=CakeClient.settings.shareCosmetics;var state=local();if(share){if(!shared||!state.equals(last)||ticks%200==0){ClientPlayNetworking.send(new Packet(state.packet(mc.player.getUUID())));last=state;shared=true;}}else if(shared){ClientPlayNetworking.send(new Packet(CosmeticState.encode(mc.player.getUUID(),null)));shared=false;last=null;}}
}
