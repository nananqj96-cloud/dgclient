package fr.cake.client;
import net.minecraft.world.phys.Vec3;
public final class SilentAimMath {
 public record Rotation(float yaw,float pitch){}
 public static Rotation rotation(Vec3 eye,Vec3 point){Vec3 d=point.subtract(eye);return new Rotation((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
 public static double range(double slider,double vanilla){return Math.min(Math.max(0,vanilla),2+Math.clamp(slider,0,1));}
}
