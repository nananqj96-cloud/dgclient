package fr.cake.client;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import fr.cake.protocol.CosmeticState;
import org.joml.Matrix4f;import org.joml.Vector3f;
import java.util.*;
public final class AvatarSkins {
 public static final String[] IDS={"frog","chicken","pigeon","alpaking","fish","glub","mushnub","mushroom_king","cactoro","ghost","yeti","green_blob","pink_blob","squidle","birb","dino","bunny","cat","alien","ninja","sahur","amogus","lego","neighbor","shrek","knuckles","donkey","sonic","reserved29","patrick","mario","luigi"};
 public static final String[] NAMES={"Grenouille","Poulet","Pigeon","Alpaking","Poisson pattes","Glub","Mini champignon","Roi champignon","Cactus","Fantôme","Yéti","Blob vert","Slime rose","Pieuvre","Birb","Dinosaure","Lapin costaud","Chat","Alien","Ninja","Tung Tung Sahur","Amogus","Lego · LDraw","Hello Neighbor","Shrek","Ugandan Knuckles","Donkey · Shrek","Sonic","Reserved","Patrick","Mario 64","Luigi"};
 private static final Map<Integer,AvatarMesh> models=new HashMap<>();private static final RenderType[] types=new RenderType[4];
 public static CosmeticState state(EntityRenderState state){var mc=Minecraft.getInstance();if(mc.level==null||!(state instanceof AvatarRenderState avatar))return null;Player p=mc.player!=null&&avatar.id==mc.player.getId()?mc.player:mc.level.getEntity(avatar.id) instanceof Player player?player:null;if(p==null||p.isSpectator()||p!=mc.player&&(p.isInvisible()||state.distanceToCameraSq>1600))return null;var s=p==mc.player?CosmeticNetwork.local():CosmeticNetwork.get(p.getUUID());return s!=null&&(s.flags()&8)!=0&&SkinCatalog.available(s.skin())&&(p!=mc.player||Edition.skin(Edition.PLAN,s.skin()))?s:null;}
 private static AvatarMesh load(int id){try(var in=AvatarSkins.class.getResourceAsStream("/assets/cakeclient/avatars/"+IDS[id-1]+".avm")){return AvatarMesh.read(in);}catch(Exception e){CakeClient.LOG.error("Avatar model unavailable",e);return null;}}
 private static RenderType type(int skin){return RenderTypes.entityCutoutNoCull(Identifier.fromNamespaceAndPath("cakeclient",skin<=20?"avatars/atlas.png":"avatars/"+IDS[skin-1]+"-atlas.png"));}
 public static boolean submit(Object raw,PoseStack stack,SubmitNodeCollector collector){if(!(raw instanceof LivingEntityRenderState state))return false;var s=state(state);if(s==null)return false;var mesh=models.computeIfAbsent(s.skin(),AvatarSkins::load);if(mesh==null)return false;
  double seconds=System.nanoTime()/1e9;var bones=mesh.pose(seconds+((AvatarRenderState)state).id*.137,state.walkAnimationPos*.13f,Math.min(1,state.walkAnimationSpeed*3));
  stack.pushPose();stack.translate(0,1.501,0);float scale=.85f+s.skinScale()*.3f;stack.scale(-scale,-scale,scale);
  collector.submitCustomGeometry(stack,type(s.skin()),(pose,vertices)->{
   float[] p=mesh.scratch;Vector3f point=new Vector3f(),normal=new Vector3f(),temp=new Vector3f();
   // Skin each indexed vertex once, rather than repeating the work for every triangle.
   for(int v=0;v<mesh.vertices;v++){int i=v*12;float[] d=mesh.data;point.zero();normal.zero();for(int k=0;k<4;k++){float w=d[i+8+k];if(w<.00001f)continue;var bone=bones[mesh.joints[v*4+k]];bone.transformPosition(d[i],d[i+1],d[i+2],temp);point.fma(w,temp);bone.transformDirection(d[i+3],d[i+4],d[i+5],temp);normal.fma(w,temp);}pose.pose().transformPosition(point);pose.transformNormal(normal.x,normal.y,normal.z,normal);normal.normalize();int o=v*6;p[o]=point.x;p[o+1]=point.y;p[o+2]=point.z;p[o+3]=normal.x;p[o+4]=normal.y;p[o+5]=normal.z;}
   for(int q=0;q<mesh.indices.length/3*4;q++){int v=mesh.indices[(q/4)*3+Math.min(q%4,2)];int o=v*6,i=v*12;vertices.addVertex(p[o],p[o+1],p[o+2]).setUv(mesh.data[i+6],mesh.data[i+7]).setColor(CombatEffects.tint(state,0xffffffff)).setLight(state.lightCoords).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setNormal(p[o+3],p[o+4],p[o+5]);}
  });stack.popPose();return true;
 }
}
