package fr.cake.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import java.util.*;

/** World-anchored insects rendered AFTER temporal blur, so wings cannot turn into trails. */
public final class Fireflies {
 private static Object world;private static Vec3 previousPlayer;private static int sequence;
 private static final Random RANDOM=new Random();
 private static final List<Fly> flies=new ArrayList<>();
 private static TextureAtlasSprite[] sprites;
 static final class Fly {
  final Vec3 anchor;final double phase;final int lifetime;int age,retiring;double time;Vec3 position,previous;
  Fly(Vec3 player){phase=sequence++*2.399963;double r=2.2+RANDOM.nextDouble()*(3+CakeClient.settings.radius*7),a=RANDOM.nextDouble()*6.283;
   anchor=player.add(Math.cos(a)*r,.5+RANDOM.nextDouble()*2.3,Math.sin(a)*r);time=RANDOM.nextDouble()*80;lifetime=500+RANDOM.nextInt(500);position=at();previous=position;}
  Vec3 at(){return anchor.add(Math.sin(time+phase)*.6+Math.sin(time*.37)*.2,Math.sin(time*.8+phase)*.3,Math.cos(time*.71+phase)*.6);}
 }
 public static int count(){return 6+(int)Math.round(CakeClient.settings.density*18);}
 public static void reset(){flies.clear();world=null;previousPlayer=null;sprites=null;}
 public static void tick(Minecraft mc){
  if(mc.level==null||mc.player==null||!CakeClient.settings.fireflies){if(!flies.isEmpty())reset();return;}
  Vec3 p=mc.player.position();if(world!=mc.level||previousPlayer==null||p.distanceToSqr(previousPlayer)>4096){reset();world=mc.level;}previousPlayer=p;if(mc.isPaused())return;
  double far=8+CakeClient.settings.radius*10;
  for(Fly f:flies){f.age++;f.previous=f.position;f.time+=(.18+CakeClient.settings.speed*1.6)/20;f.position=f.at();if(f.anchor.distanceToSqr(p)>far*far)f.retiring++;}
  flies.removeIf(f->f.age>=f.lifetime||f.retiring>25);
  while(flies.size()>count())flies.removeLast();for(int i=0;i<2&&flies.size()<count();i++)flies.add(new Fly(p));
 }
 public static void render(Camera camera,Matrix4f matrix,float partial){
  if(!CakeClient.settings.fireflies||flies.isEmpty()||world!=Minecraft.getInstance().level)return;
  var mc=Minecraft.getInstance();var atlas=mc.getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.PARTICLES);
  if(sprites==null){sprites=new TextureAtlasSprite[4];for(int i=0;i<4;i++)sprites[i]=atlas.getSprite(Identifier.fromNamespaceAndPath("cakeclient","firefly_"+i));}
  InsectBatch.begin(camera,matrix);
  for(Fly f:flies){
   Vec3 pos=f.previous.lerp(f.position,partial);
   if(mc.level.getBlockState(net.minecraft.core.BlockPos.containing(pos)).isSolidRender())continue;
   double fade=Math.min(1,f.age/20.)*Math.min(1,(f.lifetime-f.age)/30.)*(1-f.retiring/26.);
   float alpha=(float)(fade*(.75+CakeClient.settings.glow*.25));double size=.10+CakeClient.settings.glow*.06;
   InsectBatch.add(pos,size,(float)(Math.sin(f.time*1.4+f.phase)*.18),CakeClient.settings.color(f.time),alpha,sprites[Math.floorMod((int)(f.time*30+f.phase),4)]);
  }
  InsectBatch.end(atlas.getTextureView());
 }
}
