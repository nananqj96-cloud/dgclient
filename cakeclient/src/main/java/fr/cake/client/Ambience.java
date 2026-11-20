package fr.cake.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public final class Ambience {
 private static final List<AmbientParticle> particles=new ArrayList<>();
 private static Object world;
 private static int tick,fxTick,fxCooldown;private static Vec3 previousFx;
 public static void reset(){for(var p:particles)p.remove();particles.clear();world=null;previousFx=null;fxCooldown=0;}
 public static void tick(Minecraft mc){
  Settings s=CakeClient.settings;
  if(mc.player==null||mc.level==null||(!s.sakura&&!s.motes&&!s.embers&&!s.snow&&!s.pollen&&!s.trailEnabled&&!s.jumpEnabled)){if(!particles.isEmpty())reset();return;}
  if(world!=mc.level){reset();world=mc.level;}if(mc.isPaused())return;
  particles.removeIf(p->!p.isAlive());
  if(!s.sakura&&!s.motes&&!s.embers&&!s.snow&&!s.pollen)return;
  int cap=12+(int)(s.ambientDensity*44);if(++tick%3!=0||particles.size()>=cap)return;
  int[] modes=new int[6];int n=0;if(s.sakura){modes[n++]=0;modes[n++]=0;}if(s.motes)modes[n++]=1;if(s.embers)modes[n++]=2;if(s.snow)modes[n++]=3;if(s.pollen)modes[n++]=4;
  int mode=modes[(tick/3)%n];Vec3 player=mc.player.position();
  if((mode==0||mode==3)&&!mc.level.canSeeSky(BlockPos.containing(player).above(3)))return;
  var p=new AmbientParticle(mc.level,player,mode);particles.add(p);mc.particleEngine.add(p);
 }
 private static void effects(Minecraft mc,Settings s){
  Vec3 position=mc.player.position();if(fxCooldown>0)fxCooldown--;
  if(previousFx!=null){
   if(s.trailEnabled&&++fxTick%2==0&&position.distanceToSqr(previousFx)>.007)spawnFx(mc,position,5,2);
   if(s.jumpEnabled&&fxCooldown==0&&position.y-previousFx.y>.12){spawnFx(mc,position,6,12);fxCooldown=20;}
  }previousFx=position;
 }
 private static void spawnFx(Minecraft mc,Vec3 center,int mode,int count){
  for(int i=0;i<count&&particles.size()<96;i++){var p=new AmbientParticle(mc.level,center,mode);particles.add(p);mc.particleEngine.add(p);}
 }
 private static final class AmbientParticle extends SingleQuadParticle {
  final int mode;final double phase;
  AmbientParticle(ClientLevel level,Vec3 center,int mode){
   super(level,center.x,center.y,center.z,Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(net.minecraft.data.AtlasIds.PARTICLES).getSprite(Identifier.fromNamespaceAndPath("cakeclient",mode==0?"sakura":mode==1?"mote":mode==2?"ember":mode==3?"snow":"pollen")));
   this.mode=mode;phase=random.nextDouble()*6.28;hasPhysics=false;lifetime=mode==0?230+random.nextInt(100):100+random.nextInt(100);
   setPos(center.x+(random.nextDouble()-.5)*10,center.y+((mode==0||mode==3)?2.5+random.nextDouble()*4:.4+random.nextDouble()*4),center.z+(random.nextDouble()-.5)*10);
   if(mode>=5){double[] at=CakeFxMath.locate(center.x,center.y,center.z,mode,phase);setPos(at[0],at[1],at[2]);lifetime=24;}
   xo=x;yo=y;zo=z;quadSize=mode==0?.14f:mode==3?.035f:mode==4?.03f:.025f;roll=(float)phase;if(mode>=5)quadSize=.075f;
   if(mode==0)setColor(1,.66f,.82f);if(mode==1)setColor(.83f,.79f,1);if(mode==2)setColor(1,.63f,.34f);if(mode==3)setColor(.88f,.94f,1);if(mode==4)setColor(1,.86f,.42f);if(mode>=5){float[] rgb=CakeFxMath.rgb(mode==5?CakeClient.settings.trailStyle:CakeClient.settings.jumpStyle);setColor(rgb[0],rgb[1],rgb[2]);}
  }
  @Override public void tick(){
   Settings s=CakeClient.settings;if(++age>lifetime||!enabled(s)){remove();return;}
   xo=x;yo=y;zo=z;oRoll=roll;
   double t=age*.055+phase,wind=.002+s.wind*.025;
   setPos(x+wind+Math.sin(t)*.004,y+(mode==0?-.037-s.wind*.02:mode==3?-.012:mode==1||mode==4?Math.sin(t*.5)*.0015:.009),z+Math.cos(t*.7)*.005);
   roll+=(mode==0?.035f:.008f);alpha=(float)(Math.min(1,age/20.)*Math.min(1,(lifetime-age)/35.)*(mode==0?.95:.45));
   if(mode==0)quadSize=.14f*(.72f+.28f*(float)Math.abs(Math.cos(t)));
   if(age%10==0&&level.getBlockState(BlockPos.containing(x,y,z)).isSolidRender())remove();
   var player=Minecraft.getInstance().player;if(player==null||player.position().distanceToSqr(new Vec3(x,y,z))>400)remove();
  }
  private boolean enabled(Settings s){return mode==5?s.trailEnabled:mode==6?s.jumpEnabled:mode==0?s.sakura:mode==1?s.motes:mode==2?s.embers:mode==3?s.snow:s.pollen;}
  @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
  @Override protected int getLightColor(float partial){return mode==0?0xC000C0:0xF000F0;}
 }
}
