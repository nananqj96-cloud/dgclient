package dev.aether.module.impl;

import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class NetheriteFinderModule extends Module {
   private static final int SCAN_INTERVAL_TICKS = 20;
   public final SliderSetting maxShown = this.addSetting(
      new SliderSetting("Max Shown", "Maximum number of nearest results to display.", 100.0, 1.0, 1000.0, 1.0)
   );
   public final BooleanSetting tracers = this.addSetting(
      new BooleanSetting("Tracers", "Draw tracers to suspicious anti-xray sections as well as exact debris.", false)
   );
   public final ModeSetting closestLayer = this.addSetting(
      new ModeSetting(
         "Closest Layer", "Selects which Netherite layer supplies the one specially highlighted suspicious box.", "ALL", "ALL", "TOP", "MIDDLE", "BOTTOM"
      )
   );
   public final ModeSetting terrainFiltering = this.addSetting(
      new ModeSetting(
         "Terrain Filtering",
         "Requires at least 90% selected terrain when compared only with its competing Nether terrain.",
         "ALL",
         "ALL",
         "NETHERRACK",
         "BASALT + BLACKSTONE"
      )
   );
   public final ColorSetting closestColor = this.addSetting(
      new ColorSetting("Closest Color", "Color used for the closest suspicious Netherite box in the selected layer.", -23296)
   );
   public final BooleanSetting debrisHotspot = this.addSetting(
      new BooleanSetting("Debris Hotspot", "Shows the best 7x7x7 hotspot and its terrain confidence in every detected search section.", false)
   );
   public final SliderSetting hotspotTarget = this.addSetting(
      new SliderSetting("Hotspot Chance", "Minimum confidence required before a hotspot and percentage are shown.", 75.0, 25.0, 95.0, 5.0, "%")
   );
   public final SliderSetting hotspotMoveDelay = this.addSetting(
      new SliderSetting("Hotspot Move Delay", "Seconds an invalid hotspot remains before choosing another qualifying area.", 15.0, 0.0, 30.0, 1.0, "s")
   );
   public final BooleanSetting hotspotMovement = this.addSetting(
      new BooleanSetting("Hotspot Movement", "Allows an invalid hotspot to move to another qualifying area after the delay.", true)
   );
   public final ColorSetting outerBoxColor = this.addSetting(
      new ColorSetting("Outer Box Color", "Color used for debris and suspicious-region boxes.", -4302081)
   );
   public final ColorSetting hotspotColor = this.addSetting(
      new ColorSetting("Hotspot Color", "Color used for the separate probability box and percentage.", -12388997)
   );
   private final AtomicBoolean scanning = new AtomicBoolean();
   private final Set<NetheriteFinderModule.BlockKey> playerPlacedBlocks = ConcurrentHashMap.newKeySet();
   private final Set<NetheriteFinderModule.BlockKey> minedDebrisBlocks = ConcurrentHashMap.newKeySet();
   private final Set<NetheriteFinderModule.SectionKey> foundDebrisSections = ConcurrentHashMap.newKeySet();
   private final Set<NetheriteFinderModule.SectionKey> retiredSections = ConcurrentHashMap.newKeySet();
   private final Set<NetheriteFinderModule.SectionKey> missedHotspotSections = ConcurrentHashMap.newKeySet();
   private final Map<NetheriteFinderModule.SectionKey, DebrisHotspot.Result> lockedHotspots = new ConcurrentHashMap<>();
   private final Map<NetheriteFinderModule.SectionKey, Long> invalidHotspotSince = new ConcurrentHashMap<>();
   private volatile List<NetheriteFinderModule.Hit> hits = List.of();
   private ExecutorService worker;
   private int timer;
   private ClientLevel trackedWorld;

   public NetheriteFinderModule() {
      super("Netherite Finder", "Finds visible Ancient Debris and suspicious anti-xray sections.", Category.RENDER);
      this.hotspotTarget.visibleWhen(this.debrisHotspot::get);
      this.hotspotMovement.visibleWhen(this.debrisHotspot::get);
      this.hotspotMoveDelay.visibleWhen(() -> this.debrisHotspot.get() && this.hotspotMovement.get());
      this.hotspotColor.visibleWhen(this.debrisHotspot::get);
   }

   @Override
   protected synchronized void onEnable() {
      this.hits = List.of();
      this.timer = 20;
      this.scanning.set(false);
      this.clearTracking();
      this.worker = Executors.newSingleThreadExecutor(runnable -> {
         Thread var1 = new Thread(runnable, "aether-netherite-scan");
         var1.setDaemon(true);
         return var1;
      });
   }

   @Override
   protected synchronized void onDisable() {
      ExecutorService var1 = this.worker;
      this.worker = null;
      if (var1 != null) {
         var1.shutdownNow();
      }

      this.scanning.set(false);
      this.hits = List.of();
      this.timer = 0;
      this.clearTracking();
   }

   @Override
   public void onTick() {
      Minecraft var1 = Minecraft.getInstance();
      ClientLevel var2 = var1.level;
      LocalPlayer var3 = var1.player;
      if (var2 != null && var3 != null) {
         if (this.trackedWorld != var2) {
            this.clearTracking();
            this.trackedWorld = var2;
         }

         boolean var4 = this.debrisHotspot.get();
         String var5 = this.terrainFiltering.get();
         if (!var4) {
            this.lockedHotspots.clear();
            this.invalidHotspotSince.clear();
         }

         if (++this.timer >= 20) {
            this.timer = 0;
            ExecutorService var6 = this.worker;
            if (var6 != null && !var6.isShutdown() && this.scanning.compareAndSet(false, true)) {
               double var7 = var3.getX();
               double var9 = var3.getY();
               double var11 = var3.getZ();
               int var13 = (Integer)var1.options.renderDistance().get();
               ChunkPos var14 = var3.chunkPosition();
               ArrayList var15 = new ArrayList();

               for (int var16 = var14.x - var13; var16 <= var14.x + var13; var16++) {
                  for (int var17 = var14.z - var13; var17 <= var14.z + var13; var17++) {
                     if (var2.getChunkSource().hasChunk(var16, var17)) {
                        var15.add(var2.getChunk(var16, var17));
                     }
                  }
               }

               int var24 = this.maxShown.getInt();
               double var25 = this.hotspotTarget.get() / 100.0;
               long var19 = (long)this.hotspotMoveDelay.getInt() * 1000L;
               boolean var21 = this.hotspotMovement.get();

               try {
                  var6.submit(() -> {
                     try {
                        List var17x = this.scan(var15, var24, var7, var9, var11, var25, var19, var21, var4, var5);
                        if (this.isEnabled() && Minecraft.getInstance().level == var2) {
                           this.hits = this.applyMiningResults(var17x);
                        }
                     } catch (Throwable var21x) {
                     } finally {
                        this.scanning.set(false);
                     }
                  });
               } catch (RuntimeException var23) {
                  this.scanning.set(false);
               }
            }
         }
      } else {
         this.hits = List.of();
         this.clearTracking();
      }
   }

   public List<NetheriteFinderModule.Hit> hits() {
      return this.hits;
   }

   public static NetheriteFinderModule.Hit closestSuspiciousHit(Iterable<NetheriteFinderModule.Hit> iterable, String string, double d, double e) {
      NetheriteFinderModule.Hit var6 = null;
      double var7 = Double.POSITIVE_INFINITY;

      for (NetheriteFinderModule.Hit var10 : iterable) {
         if (!var10.exact() && var10.matchesLayer(string)) {
            double var11 = (var10.x1() + var10.x2()) * 0.5 - d;
            double var13 = (var10.z1() + var10.z2()) * 0.5 - e;
            double var15 = var11 * var11 + var13 * var13;
            if (var15 < var7 || var15 == var7 && comesBefore(var10, var6)) {
               var6 = var10;
               var7 = var15;
            }
         }
      }

      return var6;
   }

   private static boolean comesBefore(NetheriteFinderModule.Hit hit, NetheriteFinderModule.Hit hit2) {
      if (hit2 == null) {
         return true;
      } else if (hit.x1() != hit2.x1()) {
         return hit.x1() < hit2.x1();
      } else {
         return hit.z1() != hit2.z1() ? hit.z1() < hit2.z1() : hit.y1() < hit2.y1();
      }
   }

   public void recordPlayerPlacement(BlockPos blockPos) {
      if (this.isEnabled() && blockPos != null) {
         this.playerPlacedBlocks.add(new NetheriteFinderModule.BlockKey(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
      }
   }

   public void onServerBlockUpdate(BlockPos blockPos, BlockState blockState) {
      ClientLevel var3 = Minecraft.getInstance().level;
      if (this.isEnabled() && var3 != null && blockPos != null && blockState != null) {
         BlockState var4 = var3.getBlockState(blockPos);
         if (isAncientDebris(var4) && !isAncientDebris(blockState)) {
            this.retireMinedSection(blockPos);
         }
      }
   }

   public void recordMinedDebris(BlockPos blockPos) {
      if (this.isEnabled() && blockPos != null) {
         this.retireMinedSection(blockPos);
      }
   }

   private synchronized void retireMinedSection(BlockPos blockPos) {
      NetheriteFinderModule.SectionKey var2 = NetheriteFinderModule.SectionKey.from(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      NetheriteFinderModule.BlockKey var3 = new NetheriteFinderModule.BlockKey(blockPos.getX(), blockPos.getY(), blockPos.getZ());
      this.foundDebrisSections.remove(var2);
      this.retiredSections.add(var2);
      this.lockedHotspots.remove(var2);
      this.invalidHotspotSince.remove(var2);
      this.minedDebrisBlocks.add(var3);
      this.hits = this.applyMiningResults(this.hits);
   }

   private static boolean contains(DebrisHotspot.Result result, NetheriteFinderModule.BlockKey blockKey) {
      int var2 = Math.floorMod(blockKey.x(), 16);
      int var3 = Math.floorMod(blockKey.y(), 16);
      int var4 = Math.floorMod(blockKey.z(), 16);
      return var2 >= result.x1() && var2 < result.x2() && var3 >= result.y1() && var3 < result.y2() && var4 >= result.z1() && var4 < result.z2();
   }

   private List<NetheriteFinderModule.Hit> applyMiningResults(List<NetheriteFinderModule.Hit> list) {
      ArrayList var2 = new ArrayList(list.size() + this.missedHotspotSections.size());

      for (NetheriteFinderModule.Hit var4 : list) {
         NetheriteFinderModule.SectionKey var5 = var4.section();
         if ((var4.exact() || !this.retiredSections.contains(var5)) && (!var4.exact() || !this.minedDebrisBlocks.contains(var4.block()))) {
            var2.add(var4);
         }
      }

      return List.copyOf(var2);
   }

   private void clearTracking() {
      this.playerPlacedBlocks.clear();
      this.minedDebrisBlocks.clear();
      this.foundDebrisSections.clear();
      this.retiredSections.clear();
      this.missedHotspotSections.clear();
      this.lockedHotspots.clear();
      this.invalidHotspotSince.clear();
      this.trackedWorld = null;
   }

   private List<NetheriteFinderModule.Hit> scan(
      List<LevelChunk> list, int i, double d, double e, double f, double g, long l, boolean bl, boolean bl2, String string
   ) {
      ArrayList var16 = new ArrayList();

      for (LevelChunk var18 : list) {
         LevelChunkSection[] var19 = var18.getSections();
         int var20 = var18.getMinSectionY();
         ChunkPos var21 = var18.getPos();

         for (int var22 = 0; var22 < var19.length; var22++) {
            LevelChunkSection var23 = var19[var22];
            if (var23 != null && !var23.hasOnlyAir()) {
               int var24 = var20 + var22 << 4;
               NetheriteFinderModule.SectionKey var25 = new NetheriteFinderModule.SectionKey(var21.x, var24 >> 4, var21.z);
               if (!var23.getStates().maybeHas(NetheriteFinderModule::isAncientDebris)) {
                  this.lockedHotspots.remove(var25);
                  this.invalidHotspotSince.remove(var25);
                  if (this.missedHotspotSections.contains(var25)) {
                     var16.add(NetheriteFinderModule.Hit.outerSection(var25));
                  }
               } else {
                  int var26 = var16.size();
                  BlockState[] var27 = new BlockState[4096];
                  boolean[] var28 = new boolean[4096];
                  int var29 = var21.getMinBlockX();
                  int var30 = var21.getMinBlockZ();

                  for (int var31 = 0; var31 < 16; var31++) {
                     for (int var32 = 0; var32 < 16; var32++) {
                        for (int var33 = 0; var33 < 16; var33++) {
                           BlockState var34 = var23.getBlockState(var33, var31, var32);
                           int var35 = (var31 * 16 + var32) * 16 + var33;
                           boolean var36 = this.playerPlacedBlocks.contains(new NetheriteFinderModule.BlockKey(var29 + var33, var24 + var31, var30 + var32));
                           var28[var35] = var36;
                           var27[var35] = var36 ? null : var34;
                           if (!var36 && var34.is(Blocks.ANCIENT_DEBRIS)) {
                              int var37 = var29 + var33;
                              int var38 = var24 + var31;
                              int var39 = var30 + var32;
                              var16.add(
                                 new NetheriteFinderModule.Hit(
                                    (double)var37, (double)var38, (double)var39, (double)(var37 + 1), (double)(var38 + 1), (double)(var39 + 1), true, List.of()
                                 )
                              );
                           }
                        }
                     }
                  }

                  if (var16.size() != var26) {
                     this.foundDebrisSections.add(var25);
                     this.lockedHotspots.remove(var25);
                     this.invalidHotspotSince.remove(var25);
                  } else if (!matchesHostFilter(var27, string)) {
                     this.lockedHotspots.remove(var25);
                     this.invalidHotspotSince.remove(var25);
                  } else if (this.retiredSections.contains(var25)) {
                     this.lockedHotspots.remove(var25);
                  } else {
                     DebrisHotspot.Analysis var40;
                     if (bl2) {
                        DebrisHotspot.Result var41 = this.lockedHotspots.get(var25);
                        if (var41 != null && var41.chance() < g) {
                           this.lockedHotspots.remove(var25, var41);
                           this.invalidHotspotSince.remove(var25);
                           var41 = null;
                        }

                        if (var41 != null) {
                           DebrisHotspot.Result var43 = DebrisHotspot.validateLocked(var27, var24, var28, var41);
                           if (var43 != null) {
                              this.invalidHotspotSince.remove(var25);
                              var40 = new DebrisHotspot.Analysis(List.of(var41));
                           } else {
                              long var45 = System.currentTimeMillis();
                              long var46 = this.invalidHotspotSince.computeIfAbsent(var25, sectionKey -> var45);
                              if (bl && var45 - var46 >= l) {
                                 this.lockedHotspots.remove(var25, var41);
                                 this.invalidHotspotSince.remove(var25);
                                 var40 = DebrisHotspot.find(var27, var24, g, var28);
                              } else {
                                 var40 = new DebrisHotspot.Analysis(List.of(var41));
                              }
                           }
                        } else {
                           var40 = DebrisHotspot.find(var27, var24, g, var28);
                        }

                        if (var40.predictionResults().isEmpty()) {
                           this.lockedHotspots.remove(var25);
                           this.invalidHotspotSince.remove(var25);
                        } else {
                           DebrisHotspot.Result var44 = var40.predictionResults().getFirst();
                           if (var44.chance() >= g) {
                              this.lockedHotspots.put(var25, var44);
                              if (var41 == null || !var44.equals(var41)) {
                                 this.invalidHotspotSince.remove(var25);
                              }
                           } else {
                              this.lockedHotspots.remove(var25);
                              this.invalidHotspotSince.remove(var25);
                              var40 = new DebrisHotspot.Analysis(List.of());
                           }
                        }
                     } else {
                        var40 = new DebrisHotspot.Analysis(List.of());
                     }

                     List var42 = var40.predictionResults()
                        .stream()
                        .map(
                           result -> new NetheriteFinderModule.ProbabilityRegion(
                                 (double)(var29 + result.x1()),
                                 (double)(var24 + result.y1()),
                                 (double)(var30 + result.z1()),
                                 (double)(var29 + result.x2()),
                                 (double)(var24 + result.y2()),
                                 (double)(var30 + result.z2()),
                                 result.chance()
                              )
                        )
                        .toList();
                     var16.add(
                        new NetheriteFinderModule.Hit(
                           (double)var29, (double)var24, (double)var30, (double)(var29 + 16), (double)(var24 + 16), (double)(var30 + 16), false, var42
                        )
                     );
                  }
               }
            }
         }
      }

      var16.sort(Comparator.<NetheriteFinderModule.Hit>comparingInt(hit -> hit.exact ? 0 : 1).thenComparingDouble(hit -> hit.distanceSquared(d, e, f)));
      return var16.size() > i ? List.copyOf(var16.subList(0, i)) : List.copyOf(var16);
   }

   private static boolean isAncientDebris(BlockState blockState) {
      return blockState.is(Blocks.ANCIENT_DEBRIS);
   }

   public static boolean matchesHostFilter(BlockState[] blockStates, String string) {
      if ("ALL".equals(string)) {
         return true;
      } else {
         int var2 = 0;
         int var3 = 0;

         for (BlockState var7 : blockStates) {
            if (var7 != null) {
               boolean var8 = var7.is(Blocks.NETHERRACK);
               boolean var9 = var7.is(Blocks.BASALT) || var7.is(Blocks.SMOOTH_BASALT) || var7.is(Blocks.BLACKSTONE);
               if ("NETHERRACK".equals(string)) {
                  if (var8) {
                     var2++;
                  } else if (var9) {
                     var3++;
                  }
               } else if ("BASALT + BLACKSTONE".equals(string)) {
                  if (var9) {
                     var2++;
                  } else if (var8) {
                     var3++;
                  }
               }
            }
         }

         int var10 = var2 + var3;
         return var10 > 0 && var2 * 100 >= var10 * 90;
      }
   }

   private static record BlockKey(int x, int y, int z) {
   }

   public static record Hit(
      double x1, double y1, double z1, double x2, double y2, double z2, boolean exact, List<NetheriteFinderModule.ProbabilityRegion> probabilityRegions
   ) {
      public boolean matchesLayer(String string) {
         if ("ALL".equals(string)) {
            return true;
         } else {
            int var2 = (int)Math.floor((this.y1 + this.y2) * 0.5);

            return switch (string) {
               case "TOP" -> var2 >= 93 && var2 <= 127;
               case "MIDDLE" -> var2 >= 45 && var2 <= 92;
               case "BOTTOM" -> var2 >= 0 && var2 <= 44;
               default -> false;
            };
         }
      }

      private boolean isInSection(NetheriteFinderModule.SectionKey sectionKey) {
         return this.section().equals(sectionKey);
      }

      private NetheriteFinderModule.SectionKey section() {
         return NetheriteFinderModule.SectionKey.from((int)Math.floor(this.x1), (int)Math.floor(this.y1), (int)Math.floor(this.z1));
      }

      private NetheriteFinderModule.BlockKey block() {
         return new NetheriteFinderModule.BlockKey((int)Math.floor(this.x1), (int)Math.floor(this.y1), (int)Math.floor(this.z1));
      }

      private NetheriteFinderModule.Hit withoutHotspot() {
         return new NetheriteFinderModule.Hit(this.x1, this.y1, this.z1, this.x2, this.y2, this.z2, false, List.of());
      }

      private static NetheriteFinderModule.Hit outerSection(NetheriteFinderModule.SectionKey sectionKey) {
         int var1 = sectionKey.x() << 4;
         int var2 = sectionKey.y() << 4;
         int var3 = sectionKey.z() << 4;
         return new NetheriteFinderModule.Hit(
            (double)var1, (double)var2, (double)var3, (double)(var1 + 16), (double)(var2 + 16), (double)(var3 + 16), false, List.of()
         );
      }

      private double distanceSquared(double d, double e, double f) {
         double var7 = (this.x1 + this.x2) * 0.5 - d;
         double var9 = (this.y1 + this.y2) * 0.5 - e;
         double var11 = (this.z1 + this.z2) * 0.5 - f;
         return var7 * var7 + var9 * var9 + var11 * var11;
      }
   }

   public static record ProbabilityRegion(double x1, double y1, double z1, double x2, double y2, double z2, double chance) {
   }

   private static record SectionKey(int x, int y, int z) {
      private static NetheriteFinderModule.SectionKey from(int i, int j, int k) {
         return new NetheriteFinderModule.SectionKey(i >> 4, j >> 4, k >> 4);
      }
   }
}
