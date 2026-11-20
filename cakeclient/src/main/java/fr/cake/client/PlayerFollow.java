package fr.cake.client;
import net.minecraft.client.Minecraft;import net.minecraft.world.entity.player.Player;import net.minecraft.tags.ItemTags;import net.minecraft.world.phys.Vec3;import net.minecraft.world.InteractionHand;import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
/** Silent Aura: server aim is scoped to the attack; local camera orientation is never changed. */
public final class PlayerFollow {
 private static Player target;private static Object world;private static boolean armed;
 public static String label(){return !armed?"Silent Aura · V: activate":target==null?"Silent Aura · searching · V: stop":"Silent Aura · "+target.getName().getString()+" · V: stop";}
 public static void clear(){target=null;armed=false;}
 public static void select(){armed=!armed;target=null;}
 public static void tick(Minecraft mc){if(world!=mc.level){clear();world=mc.level;}var s=CakeClient.settings;if(s==null||!s.followEnabled){clear();return;}target=null;if(!armed||mc.player==null||mc.level==null||mc.gameMode==null||mc.getConnection()==null||mc.screen!=null||mc.isPaused()||!mc.isWindowActive()||!mc.player.isAlive()||mc.player.isSpectator()||mc.player.isUsingItem()||s.followSword&&!mc.player.getMainHandItem().is(ItemTags.SWORDS))return;
  var player=mc.player;Vec3 eye=player.getEyePosition();double distance=SilentAimMath.range(s.followRange,player.entityInteractionRange());double best=distance*distance;Vec3 aim=null;
  for(var p:mc.level.players()){if(p==player||!p.isAlive()||p.isRemoved()||p.isSpectator()||p.isInvisible()||player.isAlliedTo(p)||!player.hasLineOfSight(p))continue;Vec3 point=p.position().add(0,p.getBbHeight()*.65,0);double d=eye.distanceToSqr(point);if(d<best){best=d;target=p;aim=point;}}
  if(target==null||player.getAttackStrengthScale(0)<.95f)return;var rotation=SilentAimMath.rotation(eye,aim);float yaw=player.getYRot(),pitch=player.getXRot();var connection=mc.getConnection();connection.send(new ServerboundMovePlayerPacket.Rot(rotation.yaw(),rotation.pitch(),player.onGround(),player.horizontalCollision));
  try{mc.gameMode.attack(player,target);player.swing(InteractionHand.MAIN_HAND);Knives.attack(0);}finally{connection.send(new ServerboundMovePlayerPacket.Rot(yaw,pitch,player.onGround(),player.horizontalCollision));}
 }
 // Kept for source compatibility with existing movement-math tests; movement is no longer redirected.
 static Vec3 redirect(Vec3 input,double playerYaw,double dx,double dz){if(input.horizontalDistanceSqr()<1e-12)return input;double yaw=Math.toDegrees(Math.atan2(-dx,dz));return input.yRot((float)-Math.toRadians(yaw-playerYaw));}
 public static Vec3 direction(Object entity,Vec3 input){return input;}
}
