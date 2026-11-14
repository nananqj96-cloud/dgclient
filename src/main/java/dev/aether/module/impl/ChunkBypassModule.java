package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.chunkbypass.ChunkBypassCapture;
import dev.aether.chunkbypass.ChunkBypassLeak;
import dev.aether.chunkbypass.ChunkBypassLedger;
import dev.aether.chunkbypass.ChunkBypassTier;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.render.ChunkBypassRenderer;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import dev.aether.util.UiSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Chunk Bypass — port of the Relapse addon's suspicious chunk finder. Clusters the digging,
 * breaking and sound evidence the server leaks into chunk cells and renders inhabited ground
 * underground. Presented with the main SusChunkFinder design (flat chunk quads, smart zones,
 * centroid markers, radar blips) but drawn in purple.
 */
public final class ChunkBypassModule extends Module {
   private static final int[] SENSITIVITY_MIN_HITS = {1, 2, 3, 3, 4, 5, 6, 7, 9, 11, 14, 18, 23, 28, 35};
   private static final int REBUILD_INTERVAL_TICKS = 20;
   private static final int MAX_RECENT = 4000;
   private static final int DISTANCE_SCAN_RADIUS_CHUNKS = 32;
   private static final int SPAWNER_WEIGHT = 3;
   private static final int REDSTONE_WEIGHT = 1;
   public final SliderSetting sensitivity = this.addSetting(
      new SliderSetting(
            "Sensitivity",
            "Higher = stricter: more activity records before a chunk cell flags",
            3.0,
            1.0,
            15.0,
            1.0
         )
         .withLabel(v -> (int)v + " (" + minHits((int)v) + " hits)")
   );
   public final SliderSetting range = this.addSetting(
      new SliderSetting("Range", "Draw distance in blocks", 384.0, 16.0, 2048.0, 16.0, " b")
   );
   public final SliderSetting maxY = this.addSetting(
      new SliderSetting("Max Y", "Only count activity at or below this Y", 0.0, -64.0, 320.0, 1.0)
   );
   public final BooleanSetting distanceScan = this.addSetting(
      new BooleanSetting("Distance Scan", "Scan loaded chunks for spawners & redstone so bases surface while flying", true)
   );
   public final SliderSetting renderY = this.addSetting(
      new SliderSetting("Render Y", "Height the flat chunk highlights render at", 100.0, -64.0, 320.0, 1.0)
   );
   public final SliderSetting fillOpacity = this.addSetting(
      new SliderSetting("Fill Opacity", "Fill opacity of the chunk quads", 90.0, 0.0, 255.0, 1.0)
   );
   public final BooleanSetting outline = this.addSetting(
      new BooleanSetting("Outline", "Crisp border along the chunk edges", true)
   );
   public final SliderSetting outlineOpacity = this.addSetting(
      new SliderSetting("Outline Opacity", "Border opacity", 200.0, 0.0, 255.0, 1.0)
   );
   public final BooleanSetting smartMode = this.addSetting(
      new BooleanSetting("Smart Mode", "Merge nearby flags into zones with a centroid marker", true)
   );
   public final SliderSetting mergeRadius = this.addSetting(
      new SliderSetting("Merge Radius", "Flags within this many chunks merge into one zone", 3.0, 1.0, 8.0, 1.0, " ch")
   );
   public final BooleanSetting centroidMarker = this.addSetting(
      new BooleanSetting("Centroid Marker", "Mark each zone's weighted centre — the likely base spot", true)
   );
   public final BooleanSetting showOnRadar = this.addSetting(
      new BooleanSetting("Show on Radar", "Pulse chunk zones on the Radar; far zones clamp to the edge", true)
   );
   public final ModeSetting notifications = this.addSetting(
      new ModeSetting("Notifications", "Announce each new zone once, with coords and distance", "Toast", "Toast", "Chat", "Off")
   );
   private final Map<Long, Integer> cells = new LinkedHashMap<>();
   private final Set<Long> alertedCells = new HashSet<>();
   private volatile List<CellFlag> flags = List.of();
   private volatile List<Zone> zones = List.of();
   private int timer;

   public ChunkBypassModule() {
      super(
         "ChunkBypass",
         "Clusters activity leaks and scanned spawner/redstone evidence into purple chunk cells to reveal inhabited ground.",
         Category.RENDER
      );
      this.outlineOpacity.visibleWhen(this.outline::get);
   }

   @Override
   protected void onEnable() {
      ChunkBypassLedger.INSTANCE.clear();
      ChunkBypassCapture.resetCounters();
      this.clear();
      if (AetherClient.notifications() != null) {
         AetherClient.notifications().pushBig("Chunk Bypass", "Credits turkeyguy — original addon");
      }
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.player != null) {
         if (++this.timer >= REBUILD_INTERVAL_TICKS) {
            this.timer = 0;
            this.rebuild();
         }
      }
   }

   private void rebuild() {
      this.cells.clear();
      int yCap = this.maxY.getInt();

      for (ChunkBypassLeak leak : ChunkBypassLedger.INSTANCE.recentSnapshot(MAX_RECENT)) {
         if (leak.tier == ChunkBypassTier.ACTIVITY && leak.pos.getY() <= yCap) {
            long key = pack(leak.pos.getX() >> 4, leak.pos.getY() >> 4, leak.pos.getZ() >> 4);
            this.cells.merge(key, 1, Integer::sum);
         }
      }

      if (this.distanceScan.get()) {
         this.scanLoadedChunks(yCap);
      }

      this.buildFlagsAndZones();
   }

   /**
    * Distance scan: walk the chunks loaded around the player and fold mechanical habitation
    * evidence (spawners, redstone components) into the cell grid. Unlike the packet-leak model
    * this reads already-loaded chunk data, so it works at render distance while flying — the
    * same complement to SusChunkFinder the addon intends, but overlaid on the activity grid.
    */
   private void scanLoadedChunks(int yCap) {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel level = mc.level;
      if (level == null || mc.player == null) {
         return;
      }

      int pcx = mc.player.getChunkX();
      int pcz = mc.player.getChunkZ();
      for (int dx = -DISTANCE_SCAN_RADIUS_CHUNKS; dx <= DISTANCE_SCAN_RADIUS_CHUNKS; dx++) {
         for (int dz = -DISTANCE_SCAN_RADIUS_CHUNKS; dz <= DISTANCE_SCAN_RADIUS_CHUNKS; dz++) {
            LevelChunk chunk = level.getChunkSource().getChunk(pcx + dx, pcz + dz, false);
            if (chunk != null) {
               this.scanChunkForHabitation(chunk, yCap);
            }
         }
      }
   }

   private void scanChunkForHabitation(LevelChunk chunk, int yCap) {
      LevelChunkSection[] sections = chunk.getSections();
      int minSectionY = chunk.getMinSectionY();
      int minX = chunk.getPos().getMinBlockX();
      int minZ = chunk.getPos().getMinBlockZ();

      for (int i = 0; i < sections.length; i++) {
         LevelChunkSection section = sections[i];
         if (section.hasOnlyAir()) {
            continue;
         }

         int baseY = minSectionY + (i << 4);
         if (baseY > yCap) {
            continue;
         }

         if (!section.getStates().maybeHas(ChunkBypassCapture::isMechanicalEvidence)) {
            continue;
         }

         for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  BlockState state = section.getBlockState(x, y, z);
                  if (!ChunkBypassCapture.isMechanicalEvidence(state)) {
                     continue;
                  }

                  int by = baseY + y;
                  if (by <= yCap) {
                     // Aggregate per chunk column (y-slice 0) so all mechanical evidence in a
                     // chunk counts together and renders through the existing flat-quad pipeline.
                     long key = pack((minX + x) >> 4, 0, (minZ + z) >> 4);
                     int weight = state.getBlock() == Blocks.SPAWNER ? SPAWNER_WEIGHT : REDSTONE_WEIGHT;
                     this.cells.merge(key, weight, Integer::sum);
                  }
               }
            }
         }
      }
   }

   private void buildFlagsAndZones() {
      int minHits = minHits(this.sensitivity.getInt());
      List<CellFlag> flagList = new ArrayList<>();

      for (Entry<Long, Integer> entry : this.cells.entrySet()) {
         if (entry.getValue() >= minHits) {
            flagList.add(new CellFlag(entry.getKey(), entry.getValue()));
         }
      }

      this.flags = List.copyOf(flagList);
      this.zones = this.buildZones(flagList);
      this.fireAlerts();
   }

   private List<Zone> buildZones(List<CellFlag> flags) {
      int radius = Math.max(1, this.mergeRadius.getInt());
      HashMap<Long, CellFlag> byKey = new HashMap<>();

      for (CellFlag f : flags) {
         byKey.put(f.cellKey, f);
      }

      List<Zone> zones = new ArrayList<>();
      Set<Long> visited = new HashSet<>();

      for (CellFlag start : flags) {
         if (visited.add(start.cellKey)) {
            ArrayList<CellFlag> members = new ArrayList<>();
            Deque<CellFlag> queue = new ArrayDeque<>(List.of(start));

            while (!queue.isEmpty()) {
               CellFlag f = queue.poll();
               members.add(f);
               int cx = chunkX(f.cellKey);
               int cz = chunkZ(f.cellKey);

               for (int dx = -radius; dx <= radius; dx++) {
                  for (int dz = -radius; dz <= radius; dz++) {
                     if (dx != 0 || dz != 0) {
                        CellFlag n = byKey.get(ChunkPos.asLong(cx + dx, cz + dz));
                        if (n != null && visited.add(n.cellKey)) {
                           queue.add(n);
                        }
                     }
                  }
               }
            }

            zones.add(makeZone(members));
         }
      }

      zones.sort(Comparator.comparingDouble(Zone::totalScore).reversed());
      return zones;
   }

   private static Zone makeZone(List<CellFlag> members) {
      Set<Long> keys = new HashSet<>();
      double total = 0.0;
      double max = 0.0;
      double hitX = 0.0;
      double hitZ = 0.0;
      double hitWeight = 0.0;

      for (CellFlag f : members) {
         keys.add(f.cellKey);
         total += f.hits;
         max = Math.max(max, f.hits);
         hitWeight += f.hits;
         hitX += (chunkX(f.cellKey) * 16.0 + 8.0) * f.hits;
         hitZ += (chunkZ(f.cellKey) * 16.0 + 8.0) * f.hits;
      }

      double cx = hitWeight > 0.0 ? hitX / hitWeight : 0.0;
      double cz = hitWeight > 0.0 ? hitZ / hitWeight : 0.0;
      return new Zone(keys, cx, cz, total, max);
   }

   private void fireAlerts() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) {
         return;
      }

      for (Zone zone : this.zones) {
         boolean seen = false;

         for (long key : zone.members()) {
            if (this.alertedCells.contains(key)) {
               seen = true;
               break;
            }
         }

         if (seen) {
            this.alertedCells.addAll(zone.members());
            continue;
         }

         this.alertedCells.addAll(zone.members());
         if (!this.notifications.is("Off")) {
            int x = (int)Math.round(zone.centroidX);
            int z = (int)Math.round(zone.centroidZ);
            int dist = (int)Math.hypot(x - mc.player.getX(), z - mc.player.getZ());
            String msg = "Chunk zone · " + dist + "m · " + x + ", " + z;
            if (this.notifications.is("Toast") && AetherClient.notifications() != null) {
               AetherClient.notifications().pushInfo(msg);
               UiSounds.notification(true);
            } else if (this.notifications.is("Chat")) {
               mc.player.displayClientMessage(Component.literal("§b[Aether] §f" + msg), false);
            }
         }
      }
   }

   public void clear() {
      this.cells.clear();
      this.alertedCells.clear();
      this.flags = List.of();
      this.zones = List.of();
      this.timer = 0;
      ChunkBypassRenderer.reset();
   }

   public List<CellFlag> flags() {
      return this.flags;
   }

   public List<Zone> zones() {
      return this.zones;
   }

   public int minHits() {
      return minHits(this.sensitivity.getInt());
   }

   private static int minHits(int sensitivity) {
      int s = Math.clamp(sensitivity, 1, 15);
      return SENSITIVITY_MIN_HITS[s - 1];
   }

   private static int chunkX(long key) {
      return (int)(key >> 42);
   }

   private static int chunkZ(long key) {
      return (int)(key & 2097151L);
   }

   private static long pack(int x, int y, int z) {
      return (x & 2097151L) << 42 | (y & 2097151L) << 21 | z & 2097151L;
   }

   public record CellFlag(long cellKey, int hits) {
   }

   public record Zone(Set<Long> members, double centroidX, double centroidZ, double totalScore, double maxScore) {
   }
}
