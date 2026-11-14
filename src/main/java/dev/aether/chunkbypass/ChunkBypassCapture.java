package dev.aether.chunkbypass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Packet-level evidence capture. Port of the Relapse addon's Capture: turns what the server
 * accidentally reveals (block updates, particles, sounds, block entities, events, spawns) into
 * ledger records. Sounds, particles and world events carry exact positions regardless of hidden
 * blocks, so they are the strongest inhabited-ground signals.
 */
public final class ChunkBypassCapture {
   private static final Map<ChunkBypassSource, Integer> counters = new ConcurrentHashMap<>();
   private static final Map<Long, Integer> smokeParticles = new ConcurrentHashMap<>();
   private static final Map<Long, Integer> mobSpawns = new ConcurrentHashMap<>();

   private ChunkBypassCapture() {
   }

   public static int count(ChunkBypassSource source) {
      return counters.getOrDefault(source, 0);
   }

   public static Map<ChunkBypassSource, Integer> counters() {
      return counters;
   }

   public static void resetCounters() {
      counters.clear();
      smokeParticles.clear();
      mobSpawns.clear();
   }

   private static void hit(ChunkBypassSource source) {
      counters.merge(source, 1, Integer::sum);
   }

   public static void handle(Packet<?> packet) {
      if (packet instanceof ClientboundBlockUpdatePacket p) {
         onBlockUpdate(p.getPos(), p.getBlockState());
      } else if (packet instanceof ClientboundSectionBlocksUpdatePacket p) {
         hit(ChunkBypassSource.SECTION_UPDATE);
         p.runUpdates((pos, state) -> onBlockUpdate(pos, state));
      } else if (packet instanceof ClientboundBlockDestructionPacket p) {
         hit(ChunkBypassSource.BREAK_PROGRESS);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.BREAK_PROGRESS, ChunkBypassTier.ACTIVITY, "progress=" + p.getId());
      } else if (packet instanceof ClientboundLevelEventPacket p) {
         onWorldEvent(p);
      } else if (packet instanceof ClientboundLevelParticlesPacket p) {
         onParticles(p);
      } else if (packet instanceof ClientboundBlockEventPacket p) {
         hit(ChunkBypassSource.BLOCK_EVENT);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.BLOCK_EVENT, ChunkBypassTier.IMPLIED, "type=" + p.getB0() + " data=" + p.getB1());
      } else if (packet instanceof ClientboundBlockEntityDataPacket p) {
         onBlockEntity(p);
      } else if (packet instanceof ClientboundAddEntityPacket p) {
         onEntitySpawn(p);
      } else if (packet instanceof ClientboundSoundPacket p) {
         hit(ChunkBypassSource.SOUND);
         BlockPos pos = posOf(p.getX(), p.getY(), p.getZ());
         ChunkBypassLedger.INSTANCE.add(pos, null, ChunkBypassSource.SOUND, ChunkBypassTier.ACTIVITY, p.getSound().getRegisteredName());
      }
   }

   private static void onBlockUpdate(BlockPos pos, BlockState state) {
      if (pos != null && state != null) {
         hit(ChunkBypassSource.BLOCK_UPDATE);
         ChunkBypassLedger.INSTANCE.add(pos, state, ChunkBypassSource.BLOCK_UPDATE, ChunkBypassTier.DIRECT, "");
         if (state.getBlock() == Blocks.SPAWNER) {
            hit(ChunkBypassSource.SPAWNER);
            ChunkBypassLedger.INSTANCE.add(pos, state, ChunkBypassSource.SPAWNER, ChunkBypassTier.DIRECT, "spawner block update");
         }

         if (isRedstone(state)) {
            hit(ChunkBypassSource.REDSTONE);
            ChunkBypassLedger.INSTANCE.add(pos, state, ChunkBypassSource.REDSTONE, ChunkBypassTier.DIRECT, "redstone component update");
         }
      }
   }

   private static void onWorldEvent(ClientboundLevelEventPacket p) {
      hit(ChunkBypassSource.WORLD_EVENT);
      int id = p.getType();
      if (id == 2004) {
         hit(ChunkBypassSource.SPAWNER);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.SPAWNER, ChunkBypassTier.IMPLIED, "spawner flame event (2004)");
      } else if (id == 1000 || id == 1001) {
         hit(ChunkBypassSource.REDSTONE);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.REDSTONE, ChunkBypassTier.IMPLIED, "dispenser/dropper fired (" + id + ")");
      } else if (id != 1500 && id != 1501) {
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.WORLD_EVENT, ChunkBypassTier.ACTIVITY, "event=" + id + " data=" + p.getData());
      } else {
         hit(ChunkBypassSource.REDSTONE);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.REDSTONE, ChunkBypassTier.IMPLIED, "redstone event (" + id + ")");
      }
   }

   private static void onParticles(ClientboundLevelParticlesPacket p) {
      ParticleOptions effect = p.getParticle();
      if (effect != null) {
         BlockPos pos = posOf(p.getX(), p.getY(), p.getZ());
         if (effect instanceof BlockParticleOption block) {
            BlockState state = block.getState();
            if (state != null && !state.isAir()) {
               hit(ChunkBypassSource.BREAK_PARTICLE);
               ChunkBypassLedger.INSTANCE.add(pos, state, ChunkBypassSource.BREAK_PARTICLE, ChunkBypassTier.DIRECT, "block particle");
            }
         } else if (effect instanceof DustParticleOptions) {
            hit(ChunkBypassSource.REDSTONE);
            ChunkBypassLedger.INSTANCE.add(pos, null, ChunkBypassSource.REDSTONE, ChunkBypassTier.ACTIVITY, "redstone dust particle");
         } else {
            ParticleType<?> type = effect.getType();
            if (type == ParticleTypes.SMOKE || type == ParticleTypes.FLAME || type == ParticleTypes.LARGE_SMOKE) {
               long cell = cellKey(pos, 4);
               int n = smokeParticles.merge(cell, 1, Integer::sum);
               if (n == 8) {
                  hit(ChunkBypassSource.SPAWNER);
                  ChunkBypassLedger.INSTANCE.add(pos, null, ChunkBypassSource.SPAWNER, ChunkBypassTier.IMPLIED, "smoke/flame cluster");
               }
            }
         }
      }
   }

   private static void onBlockEntity(ClientboundBlockEntityDataPacket p) {
      hit(ChunkBypassSource.BLOCK_ENTITY);
      if (p.getType() == BlockEntityType.MOB_SPAWNER) {
         hit(ChunkBypassSource.SPAWNER);
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.SPAWNER, ChunkBypassTier.IMPLIED, "spawner block entity data");
      } else {
         ChunkBypassLedger.INSTANCE.add(p.getPos(), null, ChunkBypassSource.BLOCK_ENTITY, ChunkBypassTier.IMPLIED, String.valueOf(p.getType()));
      }
   }

   private static void onEntitySpawn(ClientboundAddEntityPacket p) {
      hit(ChunkBypassSource.ENTITY_SPAWN);
      BlockPos pos = posOf(p.getX(), p.getY(), p.getZ());
      EntityType<?> type = p.getType();
      boolean placedObject = type == EntityType.CHEST_MINECART
         || type == EntityType.HOPPER_MINECART
         || type == EntityType.OAK_CHEST_BOAT
         || type == EntityType.SPAWNER_MINECART;
      ChunkBypassLedger.INSTANCE.add(pos, null, ChunkBypassSource.ENTITY_SPAWN, ChunkBypassTier.IMPLIED, String.valueOf(type));
      if (!placedObject) {
         int n = mobSpawns.merge(cellKey(pos, 8), 1, Integer::sum);
         if (n == 12) {
            hit(ChunkBypassSource.SPAWNER);
            ChunkBypassLedger.INSTANCE.add(pos, null, ChunkBypassSource.SPAWNER, ChunkBypassTier.IMPLIED, "mob spawn cluster");
         }
      }
   }

   private static boolean isRedstone(BlockState state) {
      return state.getBlock() == Blocks.REDSTONE_WIRE
         || state.getBlock() == Blocks.REPEATER
         || state.getBlock() == Blocks.COMPARATOR
         || state.getBlock() == Blocks.LEVER
         || state.getBlock() == Blocks.REDSTONE_TORCH
         || state.getBlock() == Blocks.REDSTONE_BLOCK
         || state.getBlock() == Blocks.PISTON
         || state.getBlock() == Blocks.STICKY_PISTON
         || state.getBlock() == Blocks.OBSERVER
         || state.getBlock() == Blocks.DISPENSER
         || state.getBlock() == Blocks.DROPPER
         || state.getBlock() == Blocks.STONE_BUTTON;
   }

   private static BlockPos posOf(double x, double y, double z) {
      return new BlockPos((int)Math.round(x), (int)Math.round(y), (int)Math.round(z));
   }

   private static long cellKey(BlockPos pos, int size) {
      return (Math.floorDiv(pos.getX(), size) & 2097151L) << 42
         | (Math.floorDiv(pos.getY(), size) & 2097151L) << 21
         | Math.floorDiv(pos.getZ(), size) & 2097151L;
   }
}
