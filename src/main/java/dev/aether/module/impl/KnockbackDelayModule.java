package dev.aether.module.impl;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.player.Player;

public class KnockbackDelayModule extends Module {
    private static final double FIGHT_RANGE = 6.0;
    private final SliderSetting chance = this.addSetting(new SliderSetting("Chance (%)", 40.0, 0.0, 100.0, 5.0F));
    private final SliderSetting airMin = this.addSetting(new SliderSetting("Air Delay Min (ms)", 50.0, 0.0, 500.0, 10.0F));
    private final SliderSetting airMax = this.addSetting(new SliderSetting("Air Delay Max (ms)", 100.0, 0.0, 500.0, 10.0F));
    private final SliderSetting groundMin = this.addSetting(new SliderSetting("Ground Delay Min (ms)", 200.0, 0.0, 1000.0, 10.0F));
    private final SliderSetting groundMax = this.addSetting(new SliderSetting("Ground Delay Max (ms)", 250.0, 0.0, 1000.0, 10.0F));
    private final BooleanSetting waterCheck = this.addSetting(new BooleanSetting("Not In Water", false));
    private final BooleanSetting onlyFights = this.addSetting(new BooleanSetting("Only Near Enemies", true));
    private final Minecraft mc = Minecraft.getInstance();
    private final Deque<KnockbackDelayModule.Held> held = new ArrayDeque<>();
    private ClientPacketListener heldFor;
    private volatile boolean fightNear;

    public KnockbackDelayModule() {
        super("knockback-delay", "Takes some hits' knockback a random moment late: a little in the air, longer on the ground.", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        synchronized (this.held) {
            this.held.clear();
        }
            this.applyDue(false);
        });
    }

    @Override
    protected void onDisable() {
        this.mc.execute(() -> this.applyDue(true));
    }

//     @EventHandler
//     public void onPacketReceive(PacketReceiveNettyEvent event) {
        // LocalPlayer player = this.mc.player;
        // if (player != null && pushesMe(event.packet, player)) {
            // boolean waiting;
            // synchronized (this.held) {
                // waiting = !this.held.isEmpty();
            // }

            // if (waiting || this.delays(player)) {
                // boolean ground = player.onGround();
                // long wait = waiting ? 0L : randomBetween(ground ? this.groundMin.get() : this.airMin.get(), ground ? this.groundMax.get() : this.airMax.get());
                // synchronized (this.held) {
                    // if (this.held.isEmpty()) {
                        // this.heldFor = player.connection;
                    // }

                    // long due = Math.max(System.currentTimeMillis() + wait, this.held.isEmpty() ? 0L : this.held.peekLast().due());
                    // this.held.addLast(new KnockbackDelayModule.Held(event.packet, due));
                // }

                // event.setCancelled(true);
            // }
        // }
    // }

    private static boolean pushesMe(Packet<?> packet, LocalPlayer player) {
        return packet instanceof ClientboundSetEntityMotionPacket motion
            ? motion.getId() == player.getId()
            : packet instanceof ClientboundExplodePacket explode && explode.playerKnockback().isPresent();
    }

    private boolean delays(LocalPlayer player) {
        if (this.waterCheck.get() && player.isInWater()) {
            return false;
        } else {
            return this.onlyFights.get() && !this.fightNear ? false : ThreadLocalRandom.current().nextDouble(100.0) < this.chance.get();
        }
    }

    private boolean enemyNear(LocalPlayer player) {
        for (Player other : player.level().players()) {
            if (other != player && other.distanceTo(player) <= 6.0 && MeleeAttacks.isEnemy(player, other)) {
                return true;
            }
        }

        return false;
    }

    private static long randomBetween(float a, float b) {
        long low = Math.round(Math.min(a, b));
        long high = Math.round(Math.max(a, b));
        return high <= low ? low : ThreadLocalRandom.current().nextLong(low, high + 1L);
    }

    private void applyDue(boolean all) {
        ClientPacketListener connection = this.mc.getConnection();
        long now = System.currentTimeMillis();

        while (true) {
            KnockbackDelayModule.Held next;
            synchronized (this.held) {
                next = this.held.peekFirst();
                if (next == null || !all && next.due() > now) {
                    return;
                }

                this.held.pollFirst();
                if (connection == null || connection != this.heldFor) {
                    this.held.clear();
                    return;
                }
            }

            ((Packet<ClientPacketListener>)next.packet()).handle(connection);
        }
    }

        private record Held(Packet<?> packet, long due) {
    }
}
