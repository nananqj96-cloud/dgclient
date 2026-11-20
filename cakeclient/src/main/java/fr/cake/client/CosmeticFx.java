package fr.cake.client;
import fr.cake.protocol.CosmeticState;
import net.minecraft.client.Minecraft;import net.minecraft.client.Camera;import net.minecraft.world.phys.Vec3;import org.joml.Matrix4f;import java.util.*;
public final class CosmeticFx {
 public static final String[] TRAILS={"Orbes","Diamants","Étoiles","Sakura","Flammes","Neige","Étincelles","Ruban"};
 public static final String[] RINGS={"Halo","Double anneau","Onde","Hexagone","Rune","Soleil","Orbites","Pointillés","Fleur","Étoile","Éclipse","Radar"};
 private record Mark(Vec3 at,long time){}private static final class Track{Vec3 last;boolean ground;CosmeticState state;final ArrayDeque<Mark> points=new ArrayDeque<>(),rings=new ArrayDeque<>();}
 private static final Map<UUID,Track> tracks=new LinkedHashMap<>();private static Object world;private static SmoothTexture atlas;
 public static void tick(Minecraft mc){if(world!=mc.level){tracks.clear();world=mc.level;}if(mc.player==null||mc.level==null||mc.isPaused())return;long now=System.nanoTime();var valid=new HashSet<UUID>();var players=new ArrayList<>(mc.level.players());players.sort(Comparator.comparingDouble(p->p.distanceToSqr(mc.player)));int n=0;
  for(var p:players){var s=CosmeticNetwork.get(p.getUUID());if(s==null||!p.isAlive()||p.isInvisible()||p.isSpectator()||p.distanceTo(mc.player)>40)continue;if(++n>8)break;valid.add(p.getUUID());var t=tracks.computeIfAbsent(p.getUUID(),id->new Track());t.state=s;var at=p.position();if(t.last!=null&&at.distanceToSqr(t.last)>64){t.points.clear();t.rings.clear();}
   if(t.last!=null&&(s.flags()&2)!=0&&at.distanceToSqr(t.last)>.003){int steps=Math.min(5,Math.max(1,(int)(at.distanceTo(t.last)/(.08+(1-s.trailDensity())*.12))));for(int k=1;k<=steps;k++){t.points.add(new Mark(t.last.lerp(at,k/(double)steps).add(0,.11,0),now));while(t.points.size()>(p==mc.player?48:24))t.points.removeFirst();}}
   if(t.last!=null&&(s.flags()&4)!=0&&t.ground&&!p.onGround()&&at.y-t.last.y>.08){t.rings.add(new Mark(t.last.add(0,.035,0),now));while(t.rings.size()>3)t.rings.removeFirst();}
   t.last=at;t.ground=p.onGround();t.points.removeIf(m->(s.flags()&2)==0||now-m.time>(.45+s.trailLife()*1.75)*1e9);t.rings.removeIf(m->(s.flags()&4)==0||now-m.time>(.7+s.jumpLife()*2)*1e9);
  }tracks.keySet().retainAll(valid);HitParticles.tick(mc);CombatEffects.tick(mc);
 }
 private static SmoothTexture atlas(){if(atlas==null)try(var in=CosmeticFx.class.getResourceAsStream("/assets/cakeclient/fx/atlas.png")){var image=javax.imageio.ImageIO.read(in);atlas=new SmoothTexture("cosmetic_atlas",image.getWidth(),image.getHeight());atlas.update(image);}catch(Exception e){throw new IllegalStateException(e);}return atlas;}
 public static void render(Camera camera,Matrix4f matrix){if(Minecraft.getInstance().player==null)return;SpriteBatch.begin(camera,matrix);long now=System.nanoTime();int budget=240;
  for(var track:tracks.values()){var s=track.state;int color=java.awt.Color.HSBtoRGB(s.trailHue(),.6f,1)&0xffffff;Mark previous=null;
   for(var m:track.points){if(--budget<0)break;double age=(now-m.time)/1e9,life=.45+s.trailLife()*1.75,fade=Math.max(0,1-age/life),size=(.055+s.trailSize()*.16)*(.5+.5*fade);var region=SpriteBatch.cell(s.trail());if(s.trail()==7&&previous!=null)SpriteBatch.ribbon(previous.at,m.at,size*.65,color,(float)fade,(float)fade,region);else SpriteBatch.add(m.at,size,(float)(age*.7),color,(float)(fade*.9),region);previous=m;}
   int ringColor=java.awt.Color.HSBtoRGB(s.jumpHue(),.55f,1)&0xffffff;for(var m:track.rings){if(--budget<0)break;double progress=(now-m.time)/1e9/(.7+s.jumpLife()*2);float alpha=(float)(Math.pow(Math.max(0,Math.sin(Math.PI*Math.clamp(progress,0,1))),.55));double radius=(.9+s.jumpSize()*1.7)*(.55+.65*(1-Math.pow(1-progress,3)));SpriteBatch.floor(m.at,radius,(float)(progress*.25),ringColor,alpha,8+s.jump());if(progress>.12)SpriteBatch.floor(m.at.add(0,.007,0),radius*.77,(float)(-progress*.35),ringColor,alpha*.55f,8+s.jump());}
  }HitParticles.render();CombatEffects.render();SpriteBatch.end(atlas().getTextureView());
 }
 public static void close(){tracks.clear();SpriteBatch.close();if(atlas!=null){atlas.close();atlas=null;}HitParticles.clear();}
}
