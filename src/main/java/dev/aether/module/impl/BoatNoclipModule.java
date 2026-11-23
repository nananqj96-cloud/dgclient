package dev.aether.module.impl;

import java.util.ArrayList;
import java.util.List;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoatNoclipModule extends Module {
    public static volatile BoatNoclipModule active;
    private static final double MAX_MISS = 0.24;
    private static final double MAX_PACKET_LENGTH = 1.0;
    private static final int MAX_PACKETS_PER_TICK = 16;
    private static final double MAX_SERVER_LEAD = 19.0;
    private static final double MAX_UNCONFIRMED = 28.5;
    private static final int STALE_SERVER_TICKS = 40;
    private static final double MIN_BURST = 2.0;
    private static final double MOUNT_BURST = 4.0;
    private static final double BURST_RECOVERY = 0.025;
    private static final double CAUTION_STEP = 0.5;
    private static final double CAUTION_GROWTH = 1.25;
    private static final double LAVA_MARGIN = 3.0;
    private final SliderSetting speed = this.addSetting(new SliderSetting("Speed", 10.0, 0.0, 320.0, 0.5F));
    private final SliderSetting speedInsideBlocks = this.addSetting(
        "speed-inside-blocks", new SliderSetting("Speed Inside Blocks", 5.0, 0.0, 320.0, 0.5F)
    );
    private final SliderSetting verticalSpeed = this.addSetting(new SliderSetting("Vertical Speed", 6.0, 0.0, 320.0, 0.5F));
    private final SliderSetting fallSpeed = this.addSetting(new SliderSetting("Fall Speed", 0.0, 0.0, 10.0, 0.1F));
    private final BooleanSetting lavaProtection = this.addSetting(new BooleanSetting("Lava Protection", true));
    private final BooleanSetting antiKick = this.addSetting(new BooleanSetting("Anti Fly Kick", true));
    private final SliderSetting delay = this.addSetting(new SliderSetting("Delay", 40.0, 1.0, 80.0, 1.0F));
    private final VehicleAntiKick antiKicker = new VehicleAntiKick();
    private AbstractBoat noclipBoat;
    private Vec3 serverPos;
    private Vec3 lastBase;
    private int staleTicks;
    private double cautionCap = Double.POSITIVE_INFINITY;
    private boolean sendingStops;
    private int sinceBase;
    private double burst = 4.0;
    private double lastMoveLength;

    public BoatNoclipModule() {
        super("boat-noclip", "Fly your boat through blocks and entities.", Category.MOVEMENT);
        this.delay.visibleWhen(this.antiKick::get);
    }

    @Override
    public boolean persistEnabled() {
        return false;
    }

    @Override
    protected void onEnable() {
//         BoatFlyModule fly = ModuleRegistry.get(BoatFlyModule.class);
        if (fly != null) {
            fly.setEnabled(false);
        }

        this.antiKicker.start(this.delay.getInt());
        active = this;
        EntityControlVehicleHook.refresh();
    }

    @Override
    protected void onDisable() {
        active = null;
        EntityControlVehicleHook.refresh();
        if (FreecamModule.active == null) {
            Minecraft.getInstance().smartCull = true;
        }

        this.antiKicker.stop();
        this.releaseBoat();
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (this.noclipBoat != null && (player == null || player.getVehicle() != this.noclipBoat)) {
            this.releaseBoat();
        }

        this.antiKicker.tick();
        this.updateCulling(mc);
    }

    private void updateCulling(Minecraft mc) {
        if (FreecamModule.active == null && mc.level != null) {
            boolean sealed = this.noclipBoat != null && mc.level.getBlockState(mc.gameRenderer.getMainCamera().blockPosition()).isSolidRender();
            mc.smartCull = !sealed;
        }
    }

//     private void onPacketSend(PacketSendEvent event) {
        if (this.antiKick.get() && event.packet instanceof ServerboundMoveVehiclePacket packet) {
            if (this.sendingStops) {
                this.antiKicker.observe(packet);
            } else if (this.antiKicker.onPacketSend(packet, this.delay.getInt())) {
                event.setCancelled(true);
            }
        }
    }

    public void onVehicleSetback() {
        this.antiKicker.forget();
        if (this.noclipBoat != null) {
            this.serverPos = this.noclipBoat.position();
        }

        this.cautionCap = 0.5;
    }

    private void releaseBoat() {
        if (this.noclipBoat != null) {
            this.noclipBoat.noPhysics = false;
        }

        this.noclipBoat = null;
        this.serverPos = null;
        this.lastBase = null;
        this.sinceBase = 0;
        this.burst = 4.0;
    }

    public Vec3 boatMovement(AbstractBoat boat, LocalPlayer player) {
        if (this.noclipBoat != boat) {
            this.releaseBoat();
            this.noclipBoat = boat;
        }

        boat.noPhysics = true;
        forceServerBroadcast(boat);
        double budget = this.serverBudget(boat);
        if (this.antiKick.get() && this.antiKicker.isHolding()) {
            return Vec3.ZERO;
        }

        budget = Math.min(budget, this.cautionCap);
        this.cautionCap *= 1.25;
        double bps = isInsideBlock(boat, player) ? this.speedInsideBlocks.get() : this.speed.get();
        Vec3 horizontal = EntityControlVehicleHook.horizontalVelocity(player, bps);
        Minecraft mc = Minecraft.getInstance();
        double velY = 0.0;
        if (EntityControlVehicleHook.isPhysicallyDown(mc.options.keyJump)) {
            velY += this.verticalSpeed.get() / 20.0;
        }

        if (EntityControlVehicleHook.isPhysicallyDown(mc.options.keySprint)) {
            velY -= this.verticalSpeed.get() / 20.0;
        } else {
            velY -= this.fallSpeed.get() / 20.0;
        }

        Vec3 move = new Vec3(horizontal.x, velY, horizontal.z);
        double length = move.length();
        if (length > budget) {
            move = move.scale(budget / length);
        }

        Vec3 planned = this.planMove(boat, move);
        this.lastMoveLength = planned.length();
        return planned;
    }

    private static void forceServerBroadcast(AbstractBoat boat) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundPaddleBoatPacket(!boat.getPaddleState(0), boat.getPaddleState(1)));
        }
    }

    private double serverBudget(AbstractBoat boat) {
        Vec3 base = boat.getPositionCodec().getBase();
        if (!base.equals(this.lastBase)) {
            this.burst = Math.max(this.burst, this.sinceBase + 1);
            this.sinceBase = 0;
            this.lastBase = base;
            this.serverPos = base;
            this.staleTicks = 0;
        } else {
            if (this.lastMoveLength > 0.01) {
                this.sinceBase++;
            }

            if (++this.staleTicks >= 40) {
                this.serverPos = boat.position();
            }
        }

        this.burst = Math.max(2.0, this.burst - 0.025);
        if (this.serverPos == null) {
            this.serverPos = boat.position();
        }

        double lead = boat.position().distanceTo(this.serverPos);
        return Math.max(0.0, Math.min(19.0 / this.burst, 28.5 - lead));
    }

    private Vec3 planMove(AbstractBoat boat, Vec3 move) {
        Vec3 start = boat.position();
        Vec3 pos = start;
        Vec3 left = move;
        List<AABB> lava = this.lavaProtection.get() ? nearbyLava(boat, move) : List.of();
        List<Vec3> stops = new ArrayList<>();

        while (stops.size() < 16 && left.lengthSqr() > 1.0E-8) {
            Vec3 hop = left.scale(Math.min(1.0, 1.0 / left.length()));
            double fraction = allowedFraction(boat, pos, hop);
            if (fraction <= 0.0) {
                break;
            }

            Vec3 step = hop.scale(fraction);
            if (reachesUnloadedChunk(boat, pos, step) || !lava.isEmpty() && approachesLava(boat, pos, step, lava)) {
                break;
            }

            pos = pos.add(step);
            left = left.subtract(step);
            stops.add(pos);
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            this.sendingStops = true;

            try {
                for (int i = 0; i < stops.size() - 1; i++) {
                    connection.send(new ServerboundMoveVehiclePacket(stops.get(i), boat.getYRot(), boat.getXRot(), false));
                }
            } finally {
                this.sendingStops = false;
            }
        }

        return pos.subtract(start);
    }

    private static List<AABB> nearbyLava(AbstractBoat boat, Vec3 move) {
        AABB area = boat.getBoundingBox().expandTowards(move).inflate(4.0);
        List<AABB> lava = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(
            Mth.floor(area.minX), Mth.floor(area.minY), Mth.floor(area.minZ), Mth.floor(area.maxX), Mth.floor(area.maxY), Mth.floor(area.maxZ)
        )) {
            if (boat.level().getFluidState(pos).is(FluidTags.LAVA)) {
                lava.add(new AABB(pos));
            }
        }

        return lava;
    }

    private static boolean approachesLava(AbstractBoat boat, Vec3 from, Vec3 step, List<AABB> lava) {
        AABB box = boat.getBoundingBox().move(from.subtract(boat.position()));
        double after = distanceTo(box.move(step), lava);
        return after < 3.0 && after < distanceTo(box, lava);
    }

    private static double distanceTo(AABB box, List<AABB> blocks) {
        double best = Double.MAX_VALUE;

        for (AABB block : blocks) {
            double dx = Math.max(0.0, Math.max(block.minX - box.maxX, box.minX - block.maxX));
            double dy = Math.max(0.0, Math.max(block.minY - box.maxY, box.minY - block.maxY));
            double dz = Math.max(0.0, Math.max(block.minZ - box.maxZ, box.minZ - block.maxZ));
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }

        return Math.sqrt(best);
    }

    private static boolean reachesUnloadedChunk(AbstractBoat boat, Vec3 from, Vec3 step) {
        AABB box = boat.getBoundingBox().move(from.subtract(boat.position())).expandTowards(step);

        for (int x = SectionPos.blockToSectionCoord(box.minX); x <= SectionPos.blockToSectionCoord(box.maxX); x++) {
            for (int z = SectionPos.blockToSectionCoord(box.minZ); z <= SectionPos.blockToSectionCoord(box.maxZ); z++) {
                if (!boat.level().getChunkSource().hasChunk(x, z)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static double allowedFraction(AbstractBoat boat, Vec3 from, Vec3 move) {
        AABB box = boat.getBoundingBox().move(from.subtract(boat.position()));
        List<VoxelShape> entities = serverEntityShapes(boat, box.expandTowards(move));
        if (replayMiss(boat, box, move, entities) <= 0.24) {
            return 1.0;
        }

        double lo = 0.0;
        double hi = 1.0;

        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) / 2.0;
            if (replayMiss(boat, box, move.scale(mid), entities) <= 0.24) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        return lo;
    }

    private static double replayMiss(AbstractBoat boat, AABB box, Vec3 move, List<VoxelShape> entities) {
        Vec3 replay = Entity.collideBoundingBox(boat, move, box, boat.level(), entities);
        return Math.hypot(move.x - replay.x, move.z - replay.z);
    }

    private static List<VoxelShape> serverEntityShapes(AbstractBoat boat, AABB area) {
        List<VoxelShape> shapes = new ArrayList<>();

        for (Entity other : boat.level().getEntities(boat, area.inflate(1.0E-7), EntitySelector.NO_SPECTATORS)) {
            if (AbstractBoat.canVehicleCollide(boat, other)) {
                shapes.add(Shapes.create(other.getBoundingBox()));
            }
        }

        return shapes;
    }

    public static boolean hidesWallOverlay(Player player) {
        return active != null && player.getVehicle() instanceof AbstractBoat;
    }

    private static boolean isInsideBlock(AbstractBoat boat, LocalPlayer player) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        AABB boatBox = boat.getBoundingBox();
        AABB box = boatBox.contract(0.0, 0.05, 0.0).inflate(0.5, 0.0, 0.5);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        MutableBlockPos pos = new MutableBlockPos();

        for (int x = Mth.floor(box.minX); x <= maxX; x++) {
            for (int y = Mth.floor(box.minY); y <= maxY; y++) {
                for (int z = Mth.floor(box.minZ); z <= maxZ; z++) {
                    BlockState state = level.getBlockState(pos.set(x, y, z));
                    if (!state.isAir()) {
                        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
                        if (!shape.isEmpty() && boatBox.intersects(shape.bounds().move(pos))) {
                            return true;
                        }
                    }
                }
            }
        }

        for (Entity other : level.getEntities(boat, box)) {
            if (other != player && !other.hasPassenger(boat) && !boat.hasPassenger(other) && other.isAlive() && other.getBoundingBox().intersects(box)) {
                return true;
            }
        }

        return false;
    }
}
