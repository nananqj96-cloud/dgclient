package dev.aether.chunkbypass;

import dev.aether.chunkbypass.ChunkBypassLedger.ChunkScore;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared evidence ledger. Port of the Relapse addon's Ledger: records every leak the packet
 * capture feeds it, keeps a bounded recent snapshot, and can rank chunks by weighted evidence.
 */
public final class ChunkBypassLedger {
   public static final ChunkBypassLedger INSTANCE = new ChunkBypassLedger();
   private static final int MAX_RECORDS = 250000;
   private static final int MAX_RECENT = 4000;
   private final Map<Long, Map<ChunkBypassSource, ChunkBypassLeak>> leaks = new ConcurrentHashMap<>();
   private final Deque<ChunkBypassLeak> recent = new ArrayDeque<>();
   private int size;

   private ChunkBypassLedger() {
   }

   public void add(BlockPos pos, BlockState state, ChunkBypassSource source, ChunkBypassTier tier, String note) {
      if (pos != null) {
         if (this.size < MAX_RECORDS) {
            long key = pos.asLong();
            Map<ChunkBypassSource, ChunkBypassLeak> bySource = this.leaks.computeIfAbsent(key, k -> new EnumMap<>(ChunkBypassSource.class));
            ChunkBypassLeak created = null;
            synchronized (bySource) {
               ChunkBypassLeak existing = bySource.get(source);
               if (existing != null) {
                  existing.hits++;
                  return;
               }

               created = new ChunkBypassLeak(pos, state, source, tier, note);
               bySource.put(source, created);
            }

            this.size++;
            synchronized (this.recent) {
               this.recent.addFirst(created);

               while (this.recent.size() > MAX_RECENT) {
                  this.recent.removeLast();
               }
            }
         }
      }
   }

   public int size() {
      return this.size;
   }

   public void clear() {
      this.leaks.clear();
      synchronized (this.recent) {
         this.recent.clear();
      }

      this.size = 0;
   }

   public List<ChunkBypassLeak> records() {
      List<ChunkBypassLeak> out = new ArrayList<>(this.size);

      for (Map<ChunkBypassSource, ChunkBypassLeak> m : this.leaks.values()) {
         out.addAll(m.values());
      }

      return out;
   }

   public List<ChunkBypassLeak> recentSnapshot(int max) {
      List<ChunkBypassLeak> out = new ArrayList<>();
      synchronized (this.recent) {
         int n = 0;

         for (ChunkBypassLeak l : this.recent) {
            if (n++ >= max) {
               break;
            }

            out.add(l);
         }

         return out;
      }
   }

   public Map<ChunkBypassSource, Integer> countBySource() {
      Map<ChunkBypassSource, Integer> out = new EnumMap<>(ChunkBypassSource.class);

      for (Map<ChunkBypassSource, ChunkBypassLeak> m : this.leaks.values()) {
         for (ChunkBypassSource s : m.keySet()) {
            out.merge(s, 1, Integer::sum);
         }
      }

      return out;
   }

   public List<ChunkScore> topChunks(int limit, int minScore) {
      Map<Long, Double> scores = new HashMap<>();
      long now = System.currentTimeMillis();

      for (Map<ChunkBypassSource, ChunkBypassLeak> m : this.leaks.values()) {
         for (ChunkBypassLeak l : m.values()) {
            long chunk = ChunkPos.asLong(l.pos.getX() >> 4, l.pos.getZ() >> 4);
            double ageMin = (now - l.time) / 60000.0;
            double decay = ageMin < 60.0 ? 1.0 : 1.0 / Math.pow(2.0, ageMin / 720.0);
            double weight = l.tier.weight * decay * Math.min(3, l.hits);
            scores.merge(chunk, weight, Double::sum);
         }
      }

      List<ChunkScore> out = new ArrayList<>();

      for (Entry<Long, Double> e : scores.entrySet()) {
         if (!(e.getValue() < minScore)) {
            long key = e.getKey();
            out.add(new ChunkScore(ChunkPos.getX(key), ChunkPos.getZ(key), e.getValue()));
         }
      }

      out.sort(Comparator.comparingDouble(c -> c.score).reversed());
      return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
   }

   public String toCsv() {
      StringBuilder sb = new StringBuilder("x,y,z,source,tier,hits,block,note\n");

      for (Map<ChunkBypassSource, ChunkBypassLeak> m : this.leaks.values()) {
         for (ChunkBypassLeak l : m.values()) {
            sb.append(l.pos.getX())
               .append(',')
               .append(l.pos.getY())
               .append(',')
               .append(l.pos.getZ())
               .append(',')
               .append(l.source)
               .append(',')
               .append(l.tier)
               .append(',')
               .append(l.hits)
               .append(',')
               .append(l.state == null ? "" : l.state.getBlock().toString())
               .append(',')
               .append('"')
               .append(l.note.replace("\"", "'"))
               .append("\"\n");
         }
      }

      return sb.toString();
   }

   public record ChunkScore(int x, int z, double score) {
   }
}
