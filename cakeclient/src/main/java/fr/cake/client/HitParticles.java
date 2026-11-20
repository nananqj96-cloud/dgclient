package fr.cake.client;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Confirmed damage bursts, bounded pool and a single shared world batch. */
public final class HitParticles {
 private static final int MAX_HIT=84,MAX_AMBIENT=96;
 private static final class Dot {Vec3 p,previous,v;int age,life,cell,color;float size;boolean hit;Dot(Vec3 p,Vec3 v,int life,int cell,int color,float size,boolean hit){this.p=this.previous=p;this.v=v;this.life=life;this.cell=cell;this.color=color;this.size=size;this.hit=hit;}}
 private record Flash(Vec3 position,long time,int color,float size){}
 private static final ArrayList<Dot> dots=new ArrayList<>();private static final ArrayDeque<Flash> flashes=new ArrayDeque<>();private static final Random random=new Random();private static int target=-1,previousHurt,tick;private static long attack;private static Object world;
 public static void attack(Entity e){target=e.getId();attack=System.nanoTime();previousHurt=e instanceof LivingEntity l?l.hurtTime:0;}
 public static void clear(){dots.clear();flashes.clear();world=null;target=-1;}
 public static void tick(Minecraft mc){if(world!=mc.level){clear();world=mc.level;}if(mc.level==null||mc.player==null||mc.isPaused())return;var s=CakeClient.settings;
  if(s.hitParticles&&target>=0&&System.nanoTime()-attack<900_000_000L&&mc.level.getEntity(target) instanceof LivingEntity entity&&entity.hurtTime>previousHurt){burst(entity.position().add(0,entity.getBbHeight()*.58,0),s);target=-1;}
  dots.removeIf(d->++d.age>=d.life||(d.hit?!s.hitParticles:!s.extraParticles));for(var d:dots){d.previous=d.p;d.p=d.p.add(d.v);d.v=d.v.scale(d.hit?.95:.99).add(0,d.hit?-.002:0,0);}
  long now=System.nanoTime();flashes.removeIf(f->now-f.time>400_000_000L||!s.hitParticles);
  if(s.extraParticles&&++tick%2==0&&dots.stream().filter(d->!d.hit).count()<MAX_AMBIENT){var p=mc.player.position().add((random.nextDouble()-.5)*20,.2+random.nextDouble()*7,(random.nextDouble()-.5)*20);int cell=s.extraParticleStyle==1?6:s.extraParticleStyle==2?3:s.extraParticleStyle==3?22:0;dots.add(new Dot(p,new Vec3(.002,(-.004+random.nextDouble()*.008),.001),100+random.nextInt(100),cell,java.awt.Color.HSBtoRGB((float)s.extraParticleHue,.45f,1)&0xffffff,.025f+(float)s.extraParticleSize*.045f,false));}
 }
 private static void burst(Vec3 p,Settings s){int n=18+(int)(s.hitParticleCount*28),cell=s.hitParticleStyle==1?20:s.hitParticleStyle==2?1:s.hitParticleStyle==3?0:2;int color=java.awt.Color.HSBtoRGB((float)s.hitParticleHue,.57f,1)&0xffffff;
  while(dots.stream().filter(d->d.hit).count()+n>MAX_HIT){int at=-1;for(int i=0;i<dots.size();i++)if(dots.get(i).hit){at=i;break;}if(at<0)break;dots.remove(at);}
  for(int i=0;i<n;i++){double a=i*Math.PI*(3-Math.sqrt(5)),y=1-2*(i+.5)/n,r=Math.sqrt(1-y*y),speed=.055+random.nextDouble()*.13;Vec3 velocity=new Vec3(Math.cos(a)*r,y*.7+.2,Math.sin(a)*r).scale(speed);dots.add(new Dot(p.add(velocity.scale(.15)),velocity,16+random.nextInt(15),cell,color,.065f+(float)s.hitParticleSize*.14f,true));}
  flashes.add(new Flash(p,System.nanoTime(),color,.45f+(float)s.hitParticleSize*.55f));while(flashes.size()>4)flashes.removeFirst();
 }
 public static void render(){float partial=Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);long now=System.nanoTime();
  for(var f:flashes){float t=(now-f.time)/400_000_000f;if(t<0||t>1)continue;float fade=(1-t)*(1-t);SpriteBatch.add(f.position,f.size*(.6+t*.9),t*.8f,f.color,fade*.65f,SpriteBatch.cell(21));SpriteBatch.add(f.position,f.size*.40*(1+t),0,0xffffff,fade*.8f,SpriteBatch.cell(0));}
  for(var d:dots){double age=d.age+partial,life=age/d.life;float fade=(float)(Math.min(1,age/2)*Math.pow(Math.max(0,1-life),1.15));Vec3 at=d.previous.lerp(d.p,partial);double size=d.size*(.6+.4*fade);SpriteBatch.add(at,size,(float)(age*.07),d.color,fade,SpriteBatch.cell(d.cell));
   if(d.hit){Vec3 tail=at.subtract(d.v.scale(2.6));SpriteBatch.ribbon(tail,at,size*.34,d.color,0,fade*.8f,SpriteBatch.cell(7));SpriteBatch.add(at,size*.23,0,0xffffff,fade,SpriteBatch.cell(0));}
  }
 }
}
