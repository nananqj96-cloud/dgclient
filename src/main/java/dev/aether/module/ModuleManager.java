package dev.aether.module;

import dev.aether.module.impl.AimAssistModule;
import dev.aether.module.impl.AnchorMacroModule;
import dev.aether.module.impl.ArmorTrimHiderModule;
import dev.aether.module.impl.AutoClickerModule;
import dev.aether.module.impl.AutoCrystalModule;
import dev.aether.module.impl.AutoInventoryTotemModule;
import dev.aether.module.impl.AutoTotemModule;
import dev.aether.module.impl.AutoTpaModule;
import dev.aether.module.impl.AutoWalkModule;
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
import dev.aether.module.impl.DebugHoleEspModule;
import dev.aether.module.impl.DoubleAnchorModule;
import dev.aether.module.impl.ElytraSwapModule;
import dev.aether.module.impl.FakeMediaModule;
import dev.aether.module.impl.FakePayModule;
import dev.aether.module.impl.FakeRolesModule;
import dev.aether.module.impl.FakeStatsModule;
import dev.aether.module.impl.FastUseModule;
import dev.aether.module.impl.FreeLookModule;
import dev.aether.module.impl.FreecamModule;
import dev.aether.module.impl.FullbrightModule;
import dev.aether.module.impl.HitBoxModule;
import dev.aether.module.impl.HitParticlesModule;
import dev.aether.module.impl.HomeMethodModule;
import dev.aether.module.impl.HoverTotemModule;
import dev.aether.module.impl.JumpCirclesModule;
import dev.aether.module.impl.MaceBomberModule;
import dev.aether.module.impl.MaceSwapModule;
import dev.aether.module.impl.MobEspModule;
import dev.aether.module.impl.MotionBlurModule;
import dev.aether.module.impl.NameProtectModule;
import dev.aether.module.impl.NameTagsModule;
import dev.aether.module.impl.NetheriteFinderModule;
import dev.aether.module.impl.PanoramaModule;
import dev.aether.module.impl.PlayerEspModule;
import dev.aether.module.impl.RegionMapModule;
import dev.aether.module.impl.ShieldBreakerModule;
import dev.aether.module.impl.SkinProtectModule;
import dev.aether.module.impl.SpawnerNametagsModule;
import dev.aether.module.impl.SpawnerProtectModule;
import dev.aether.module.impl.SpeedMineModule;
import dev.aether.module.impl.SpotifyHudModule;
import dev.aether.module.impl.StaffListModule;
import dev.aether.module.impl.StorageEspModule;
import dev.aether.module.impl.SwingSpeedModule;
import dev.aether.module.impl.TriggerbotModule;
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
   public FullbrightModule fullbright;
   public AutoWalkModule autoWalk;
   public WeatherNotifierModule weatherNotifier;
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
   public SpeedMineModule speedMine;
   public HomeMethodModule homeMethod;
   public PanoramaModule panorama;
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
      this.fullbright = new FullbrightModule();
      this.register(this.fullbright);
      this.playerEsp = new PlayerEspModule();
      this.register(this.playerEsp);
      this.mobEsp = new MobEspModule();
      this.register(this.mobEsp);
      this.netheriteFinder = new NetheriteFinderModule();
      this.register(this.netheriteFinder);
      this.spawnerNametags = new SpawnerNametagsModule();
      this.register(this.spawnerNametags);
      this.register(this.susChunkFinder);
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
