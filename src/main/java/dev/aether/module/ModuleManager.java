package dev.aether.module;

import dev.aether.accessory.AccessoriesModule;
import dev.aether.module.impl.AimAssistModule;
import dev.aether.module.impl.AnchorMacroModule;
import dev.aether.module.impl.AirParticlesModule;
import dev.aether.module.impl.ArmorTrimHiderModule;
import dev.aether.module.impl.AutoClickerModule;
import dev.aether.module.impl.AutoCrystalModule;
import dev.aether.module.impl.AutoInventoryTotemModule;
import dev.aether.module.impl.AutoTotemModule;
import dev.aether.module.impl.AutoPotModule;
import dev.aether.module.impl.AutoToolModule;
import dev.aether.module.impl.AutoTpaModule;
import dev.aether.module.impl.AutoWalkModule;
import dev.aether.module.impl.BlockOverlayModule;
import dev.aether.module.impl.BlockHighlightModule;
import dev.aether.module.impl.BlockEspModule;
import dev.aether.module.impl.ChatMacroModule;
import dev.aether.module.impl.ChunkBypassModule;
import dev.aether.module.impl.ChunkFinderModule;
import dev.aether.module.impl.ClusterEspModule;
import dev.aether.module.impl.CoordSnapperModule;
import dev.aether.module.impl.CustomAccessoriesModule;
import dev.aether.module.impl.CustomCrosshairModule;
import dev.aether.module.impl.CustomFovModule;
import dev.aether.module.impl.CustomGlintModule;
import dev.aether.module.impl.ActivityInspectorModule;
import dev.aether.module.impl.BaseChunkFinderModule;
import dev.aether.module.impl.SusFinderModule;
import dev.aether.module.impl.DebugHoleEspModule;
import dev.aether.module.impl.DoubleAnchorModule;
import dev.aether.module.impl.ElytraSwapModule;
import dev.aether.module.impl.FakeMediaModule;
import dev.aether.module.impl.FakeCoordinatesModule;
import dev.aether.module.impl.FakePayModule;
import dev.aether.module.impl.FakeRolesModule;
import dev.aether.module.impl.FakeStatsModule;
import dev.aether.module.impl.FastPlaceModule;
import dev.aether.module.impl.FastUseModule;
import dev.aether.module.impl.FreeLookModule;
import dev.aether.module.impl.FreecamModule;
import dev.aether.module.impl.FlyModule;
import dev.aether.module.impl.FullbrightModule;
import dev.aether.module.impl.HitBoxModule;
import dev.aether.module.impl.HitParticlesModule;
import dev.aether.module.impl.HeatColorModule;
import dev.aether.module.impl.HomeMethodModule;
import dev.aether.module.impl.HoverTotemModule;
import dev.aether.module.impl.JumpCirclesModule;
import dev.aether.module.impl.MaceBomberModule;
import dev.aether.module.impl.MaceSwapModule;
import dev.aether.module.impl.MobEspModule;
import dev.aether.module.impl.MotionBlurModule;
import dev.aether.module.impl.NameProtectModule;
import dev.aether.module.impl.NoExplosionsModule;
import dev.aether.module.impl.NoFlameModule;
import dev.aether.module.impl.NameTagsModule;
import dev.aether.module.impl.NetheriteFinderModule;
import dev.aether.module.impl.PanoramaModule;
import dev.aether.module.impl.PlayerEspModule;
import dev.aether.module.impl.LootESPModule;
import dev.aether.module.impl.PotionRefillModule;
import dev.aether.module.impl.PlayerTrailModule;
import dev.aether.module.impl.RegionMapModule;
import dev.aether.module.impl.ShieldBreakerModule;
import dev.aether.module.impl.SkinProtectModule;
import dev.aether.module.impl.SkyShaderModule;
import dev.aether.module.impl.TargetESPModule;
import dev.aether.module.impl.TotemCounterModule;
import dev.aether.module.impl.WaypointsModule;
import dev.aether.module.impl.SpawnerNametagsModule;
import dev.aether.module.impl.SpawnerProtectModule;
import dev.aether.module.impl.SpeedMineModule;
import dev.aether.module.impl.SpotifyHudModule;
import dev.aether.module.impl.StaffListModule;
import dev.aether.module.impl.StorageEspModule;
import dev.aether.module.impl.SwingSpeedModule;
import dev.aether.module.impl.TimeChangerModule;
import dev.aether.module.impl.TenacityAimAssistModule;
import dev.aether.module.impl.TenacityAutoEatModule;
import dev.aether.module.impl.TenacityAutoJumpModule;
import dev.aether.module.impl.TenacityAutoRespawnModule;
import dev.aether.module.impl.TenacityAutoToolModule;
import dev.aether.module.impl.TenacityMouseClickSoundModule;
import dev.aether.module.impl.TenacityNameScramblerModule;
import dev.aether.module.impl.TenacitySnapTapModule;
import dev.aether.module.impl.TenacitySprintModule;
import dev.aether.module.impl.TenacityStepModule;
import dev.aether.module.impl.TenacityTriggerBotModule;
import dev.aether.module.impl.TenacityVisualsModule;
import dev.aether.module.impl.TriggerbotModule;
import dev.aether.module.impl.WeatherChangerModule;
import dev.aether.module.impl.WeatherNotifierModule;
import dev.aether.module.impl.ZoomModule;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final Map<Category, List<Module>> byCategory = new LinkedHashMap<>();
   public final Modules.ClickGuiModule clickGui;
   public final Modules.HudModule hud;
   public final Modules.SpotifyModule spotify;
   public final Modules.BlockOutlineModule blockOutline;
   public final Modules.SusChunkFinderModule susChunkFinder;
   public ActivityInspectorModule activityInspector;
   public BaseChunkFinderModule baseChunkFinder;
   public SusFinderModule susFinder;
   public FlyModule fly;
   public AirParticlesModule airParticles;
   public FullbrightModule fullbright;
   public AutoWalkModule autoWalk;
   public WeatherNotifierModule weatherNotifier;
   public NoFlameModule noFlame;
   public NoExplosionsModule noExplosions;
   public TimeChangerModule timeChanger;
   public WeatherChangerModule weatherChanger;
   public AutoToolModule autoTool;
   public AutoPotModule autoPot;
   public PotionRefillModule potionRefill;
   public FakeCoordinatesModule fakeCoordinates;
   public HeatColorModule heatColor;
   public TotemCounterModule totemCounter;
   public WaypointsModule waypoints;
   public TargetESPModule targetEsp;
   public LootESPModule lootEsp;
   public BlockHighlightModule blockHighlight;
   public BlockOverlayModule blockOverlay;
   public FastPlaceModule fastPlace;
   public SwingSpeedModule swingSpeed;
   public StorageEspModule storageEsp;
   public BlockEspModule blockEsp;
   public ClusterEspModule clusterEsp;
   public AutoTotemModule autoTotem;
   public MaceSwapModule maceSwap;
   public AnchorMacroModule anchorMacro;
   public AutoCrystalModule autoCrystal;
   public HitBoxModule hitBox;
   public ElytraSwapModule elytraSwap;
   public HoverTotemModule hoverTotem;
   public ShieldBreakerModule shieldBreaker;
   public TriggerbotModule triggerbot;
   public AimAssistModule aimAssist;
   public DoubleAnchorModule doubleAnchor;
   public MaceBomberModule maceBomber;
   public NameProtectModule nameProtect;
   public SkinProtectModule skinProtect;
   public NameTagsModule nameTags;
   public FastUseModule fastUse;
   public AutoInventoryTotemModule autoInventoryTotem;
   public PlayerEspModule playerEsp;
   public MobEspModule mobEsp;
   public PlayerTrailModule playerTrail;
   public NetheriteFinderModule netheriteFinder;
   public SpawnerNametagsModule spawnerNametags;
   public DebugHoleEspModule debugHoleEsp;
   public FreecamModule freecam;
   public AutoTpaModule autoTpa;
   public JumpCirclesModule jumpCircles;
   public CustomCrosshairModule customCrosshair;
   public ZoomModule zoom;
   public CustomFovModule customFov;
   public HitParticlesModule hitParticles;
   public MotionBlurModule motionBlur;
   public CustomGlintModule customGlint;
   public CustomAccessoriesModule customAccessories;
   public ChunkFinderModule chunkFinder;
   public ChunkBypassModule chunkBypass;
   public FreeLookModule freeLook;
   public AutoClickerModule autoClicker;
   public CoordSnapperModule coordSnapper;
   public RegionMapModule regionMap;
   public ChatMacroModule chatMacro;
   public FakePayModule fakePay;
   public FakeMediaModule fakeMedia;
   public FakeStatsModule fakeStats;
   public FakeRolesModule fakeRoles;
   public StaffListModule staffList;
   public ArmorTrimHiderModule armorTrimHider;
   public SpawnerProtectModule spawnerProtect;
   public SkyShaderModule skyShader;
   public AccessoriesModule accessories;
   public SpeedMineModule speedMine;
   public HomeMethodModule homeMethod;
   public PanoramaModule panorama;
   // Tenacity ported modules
   public TenacitySprintModule tenacitySprint;
   public TenacitySnapTapModule tenacitySnapTap;
   public TenacityStepModule tenacityStep;
   public TenacityAutoJumpModule tenacityAutoJump;
   public TenacityAutoEatModule tenacityAutoEat;
   public TenacityAutoRespawnModule tenacityAutoRespawn;
   public TenacityAutoToolModule tenacityAutoTool;
   public TenacityAimAssistModule tenacityAimAssist;
   public TenacityVisualsModule tenacityVisuals;
   public TenacityTriggerBotModule tenacityTriggerBot;
   public TenacityMouseClickSoundModule tenacityClickSound;
   public TenacityNameScramblerModule tenacityNameScrambler;
   private Runnable openGuiAction = () -> {
   };
   private BiConsumer<Module, Boolean> toggleListener = (module, boolean_) -> {
   };

   public ModuleManager() {
      for (Category var4 : Category.values()) {
         this.byCategory.put(var4, new ArrayList<>());
      }

      this.susChunkFinder = new Modules.SusChunkFinderModule();
      this.registerPlaceholders();
      this.blockOutline = new Modules.BlockOutlineModule();
      this.register(this.blockOutline);
      this.hud = new Modules.HudModule();
      this.register(this.hud);
      this.spotify = new SpotifyHudModule();
      this.register(this.spotify);
      this.chatMacro = new ChatMacroModule();
      this.register(this.chatMacro);
      this.swingSpeed = new SwingSpeedModule();
      this.register(this.swingSpeed);
      this.jumpCircles = new JumpCirclesModule();
      this.register(this.jumpCircles);
      this.clickGui = new Modules.ClickGuiModule();
      this.register(this.clickGui);
      this.registerTenacityModules();
   }

   private void registerTenacityModules() {
      this.tenacitySprint = new TenacitySprintModule();
      this.register(this.tenacitySprint);
      this.tenacitySnapTap = new TenacitySnapTapModule();
      this.register(this.tenacitySnapTap);
      this.tenacityStep = new TenacityStepModule();
      this.register(this.tenacityStep);
      this.tenacityAutoJump = new TenacityAutoJumpModule();
      this.register(this.tenacityAutoJump);
      this.tenacityAutoEat = new TenacityAutoEatModule();
      this.register(this.tenacityAutoEat);
      this.tenacityAutoRespawn = new TenacityAutoRespawnModule();
      this.register(this.tenacityAutoRespawn);
      this.tenacityAutoTool = new TenacityAutoToolModule();
      this.register(this.tenacityAutoTool);
      this.tenacityAimAssist = new TenacityAimAssistModule();
      this.register(this.tenacityAimAssist);
      this.tenacityTriggerBot = new TenacityTriggerBotModule();
      this.register(this.tenacityTriggerBot);
      this.tenacityVisuals = new TenacityVisualsModule();
      this.register(this.tenacityVisuals);
      this.tenacityClickSound = new TenacityMouseClickSoundModule();
      this.register(this.tenacityClickSound);
      this.tenacityNameScrambler = new TenacityNameScramblerModule();
      this.register(this.tenacityNameScrambler);
   }

   private void registerPlaceholders() {
      this.autoTotem = new AutoTotemModule();
      this.register(this.autoTotem);
      this.autoCrystal = new AutoCrystalModule();
      this.register(this.autoCrystal);
      this.anchorMacro = new AnchorMacroModule();
      this.register(this.anchorMacro);
      this.doubleAnchor = new DoubleAnchorModule();
      this.register(this.doubleAnchor);
      this.autoInventoryTotem = new AutoInventoryTotemModule();
      this.register(this.autoInventoryTotem);
      this.maceSwap = new MaceSwapModule();
      this.register(this.maceSwap);
      this.hitBox = new HitBoxModule();
      this.register(this.hitBox);
      this.elytraSwap = new ElytraSwapModule();
      this.register(this.elytraSwap);
      this.hoverTotem = new HoverTotemModule();
      this.register(this.hoverTotem);
      this.shieldBreaker = new ShieldBreakerModule();
      this.register(this.shieldBreaker);
      this.triggerbot = new TriggerbotModule();
      this.register(this.triggerbot);
      this.maceBomber = new MaceBomberModule();
      this.register(this.maceBomber);
      this.skinProtect = new SkinProtectModule();
      this.register(this.skinProtect);
      this.nameProtect = new NameProtectModule();
      this.register(this.nameProtect);
      this.freecam = new FreecamModule();
      this.register(this.freecam);
      this.autoTpa = new AutoTpaModule();
      this.register(this.autoTpa);
      this.autoClicker = new AutoClickerModule();
      this.register(this.autoClicker);
      this.fastUse = new FastUseModule();
      this.register(this.fastUse);
      this.nameTags = new NameTagsModule();
      this.register(this.nameTags);
      this.weatherNotifier = new WeatherNotifierModule();
      this.register(this.weatherNotifier);
      this.noFlame = new NoFlameModule();
      this.register(this.noFlame);
      this.noExplosions = new NoExplosionsModule();
      this.register(this.noExplosions);
      this.timeChanger = new TimeChangerModule();
      this.register(this.timeChanger);
      this.weatherChanger = new WeatherChangerModule();
      this.register(this.weatherChanger);
      this.autoTool = new AutoToolModule();
      this.register(this.autoTool);
      this.autoPot = new AutoPotModule();
      this.register(this.autoPot);
      this.potionRefill = new PotionRefillModule();
      this.register(this.potionRefill);
      this.fakeCoordinates = new FakeCoordinatesModule();
      this.register(this.fakeCoordinates);
      this.heatColor = new HeatColorModule();
      this.register(this.heatColor);
      this.totemCounter = new TotemCounterModule();
      this.register(this.totemCounter);
      this.waypoints = new WaypointsModule();
      this.register(this.waypoints);
      this.targetEsp = new TargetESPModule();
      this.register(this.targetEsp);
      this.lootEsp = new LootESPModule();
      this.register(this.lootEsp);
      this.blockHighlight = new BlockHighlightModule();
      this.register(this.blockHighlight);
      this.blockOverlay = new BlockOverlayModule();
      this.register(this.blockOverlay);
      this.fastPlace = new FastPlaceModule();
      this.register(this.fastPlace);
      this.armorTrimHider = new ArmorTrimHiderModule();
      this.register(this.armorTrimHider);
      this.customCrosshair = new CustomCrosshairModule();
      this.register(this.customCrosshair);
      this.staffList = new StaffListModule();
      this.register(this.staffList);
      this.customGlint = new CustomGlintModule();
      this.register(this.customGlint);
      this.customFov = new CustomFovModule();
      this.register(this.customFov);
      this.coordSnapper = new CoordSnapperModule();
      this.register(this.coordSnapper);
      this.autoWalk = new AutoWalkModule();
      this.register(this.autoWalk);
      this.zoom = new ZoomModule();
      this.register(this.zoom);
      this.freeLook = new FreeLookModule();
      this.register(this.freeLook);
      this.spawnerProtect = new SpawnerProtectModule();
      this.register(this.spawnerProtect);
      this.skyShader = new SkyShaderModule();
      this.register(this.skyShader);
      this.accessories = new AccessoriesModule();
      this.register(this.accessories);
      this.speedMine = new SpeedMineModule();
      this.register(this.speedMine);
      this.homeMethod = new HomeMethodModule();
      this.register(this.homeMethod);
      this.panorama = new PanoramaModule();
      this.register(this.panorama);
      this.fakePay = new FakePayModule();
      this.register(this.fakePay);
      this.fakeMedia = new FakeMediaModule();
      this.register(this.fakeMedia);
      this.blockEsp = new BlockEspModule();
      this.register(this.blockEsp);
      this.clusterEsp = new ClusterEspModule();
      this.register(this.clusterEsp);
      this.storageEsp = new StorageEspModule();
      this.register(this.storageEsp);
      this.airParticles = new AirParticlesModule();
      this.register(this.airParticles);
      this.fullbright = new FullbrightModule();
      this.register(this.fullbright);
      this.playerEsp = new PlayerEspModule();
      this.register(this.playerEsp);
      this.mobEsp = new MobEspModule();
      this.register(this.mobEsp);
      this.playerTrail = new PlayerTrailModule();
      this.register(this.playerTrail);
      this.netheriteFinder = new NetheriteFinderModule();
      this.register(this.netheriteFinder);
      this.spawnerNametags = new SpawnerNametagsModule();
      this.register(this.spawnerNametags);
      this.register(this.susChunkFinder);
      this.susFinder = new SusFinderModule();
      this.register(this.susFinder);
      this.baseChunkFinder = new BaseChunkFinderModule();
      this.register(this.baseChunkFinder);
      this.activityInspector = new ActivityInspectorModule();
      this.register(this.activityInspector);
      this.fly = new FlyModule();
      this.register(this.fly);
      this.chunkFinder = new ChunkFinderModule();
      this.register(this.chunkFinder);
      this.chunkBypass = new ChunkBypassModule();
      this.register(this.chunkBypass);
      this.regionMap = new RegionMapModule();
      this.register(this.regionMap);
   }

   public void register(Module module) {
      this.modules.add(module);
      this.byCategory.get(module.getCategory()).add(module);
      module.setToggleCallback(this::notifyToggle);
   }

   public List<Module> all() {
      return this.modules;
   }

   public List<Module> inCategory(Category category) {
      return this.byCategory.get(category);
   }

   public void setOpenGuiAction(Runnable runnable) {
      this.openGuiAction = runnable;
   }

   public void setToggleListener(BiConsumer<Module, Boolean> biConsumer) {
      this.toggleListener = biConsumer;
   }

   public void notifyToggle(Module module, boolean bl) {
      this.toggleListener.accept(module, bl);
   }

   public boolean onKeyPressed(int i) {
      if (this.clickGui.getKeybind().matches(i)) {
         this.openGuiAction.run();
         return true;
      } else {
         boolean var2 = false;

         for (Module var4 : this.modules) {
            if (var4 != this.clickGui && var4.getKeybind().matches(i)) {
               var4.toggle();
               var2 = true;
            }
         }

         for (Module var6 : this.modules) {
            if (var6.isEnabled() && var6.onKeyPress(i)) {
               var2 = true;
            }
         }

         return var2;
      }
   }

   public void onTick() {
      for (Module var2 : this.modules) {
         if (var2.isEnabled() && var2.getCategory() != Category.COMBAT) {
            var2.onTick();
         }
      }
   }

   public void onCombatTick() {
      for (Module var2 : this.modules) {
         if (var2.isEnabled() && var2.getCategory() == Category.COMBAT) {
            var2.onTick();
         }
      }
   }
}
