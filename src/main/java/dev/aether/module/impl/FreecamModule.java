package dev.aether.module.impl;

import dev.aether.AetherClient;
import dev.aether.mixin.ClientInputAccessor;
import dev.aether.module.Category;
import dev.aether.module.Module;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class FreecamModule extends Module {
   private static FreecamModule instance;
   private static final double SCROLL_SPEED_STEP = 0.1;
   public final SliderSetting speed = this.addSetting(
      new SliderSetting("Speed", "Camera fly speed", 1.0, 0.1, 50.0, 0.1, "x")
   );
   public final SliderSetting verticalMultiplier = this.addSetting(
      new SliderSetting(
         "Vertical speed",
         "Up/down speed multiplier",
         1.0,
         0.2,
         5.0,
         0.05,
         "x"
      )
   );
   public final BooleanSetting smoothing = this.addSetting(
      new BooleanSetting("Smoothing", "Ease camera movement", true)
   );
   public final BooleanSetting showPlayerModel = this.addSetting(
      new BooleanSetting("Show own body", "Render your body while detached", true)
   );
   public final BooleanSetting showHands = this.addSetting(
      new BooleanSetting("Show hands", "Keep first-person hands visible", true)
   );
   public final SliderSetting lookSensitivity = this.addSetting(
      new SliderSetting("Look Sensitivity", "Camera look sensitivity", 0.5, 0.1, 2.0, 0.05)
   );
   public final SliderSetting freecamChunkDistance = this.addSetting(
      new SliderSetting("Chunk distance", "Chunks to load around the camera", 12.0, 2.0, 32.0, 1.0)
   );
   private double currentX;
   private double currentY;
   private double currentZ;
   private double prevX;
   private double prevY;
   private double prevZ;
   private float currentYaw;
   private float currentPitch;
   private float prevYaw;
   private float prevPitch;
   private double savedX;
   private double savedY;
   private double savedZ;
   private float savedYaw;
   private float savedPitch;
   private boolean savedAbilitiesFlying;
   private boolean savedSmartCull;
   private CameraType perspectiveBeforeFreecam;
   private boolean switchedPerspectiveForBody;
   private ChunkPos lastSyncedCamChunk;
   private int lastSyncedLoadDistance = Integer.MIN_VALUE;
   private boolean activationPending;
   private boolean active = false;
   private boolean latchedForward;
   private boolean latchedBack;
   private boolean latchedLeft;
   private boolean latchedRight;
   private boolean latchedJump;
   private boolean latchedSneak;
   private boolean latchedSprint;

   public FreecamModule() {
      super(
         "Freecam",
         "Detached camera (WASD = fly). Body can keep walking; mining uses real aim.",
         Category.MISC
      );
      instance = this;
   }

   public static FreecamModule get() {
      return instance;
   }

   public boolean adjustSpeedFromScroll(double d) {
      if (this.isActive() && d != 0.0) {
         this.speed.set(scrolledSpeed(this.speed.get(), d));
         return true;
      } else {
         return false;
      }
   }

   static double scrolledSpeed(double d, double e) {
      return e == 0.0 ? d : d + Math.copySign(0.1, e);
   }

   @Override
   protected void onEnable() {
      this.clearMovementLatches();
      Minecraft var1 = Minecraft.getInstance();
      if (var1.player != null) {
         this.captureMovementLatches(var1);
      }

      this.activationPending = true;
      this.active = false;
   }

   @Override
   protected void onDisable() {
      Minecraft var1 = Minecraft.getInstance();
      this.clearMovementLatches();
      this.activationPending = false;
      this.active = false;
      if (this.switchedPerspectiveForBody) {
         var1.options.setCameraType(this.perspectiveBeforeFreecam);
         this.switchedPerspectiveForBody = false;
      }

      this.restoreViewOnlyClientState(var1);
      this.restoreVanillaChunkLoading(var1);
      if (var1.player != null) {
         var1.player.getAbilities().flying = this.savedAbilitiesFlying;
      }
   }

   public void tryCompleteActivation(Minecraft minecraft) {
      if (this.isEnabled() && this.activationPending && minecraft.player != null && minecraft.level != null) {
         this.savedX = minecraft.player.getX();
         this.savedY = minecraft.player.getY();
         this.savedZ = minecraft.player.getZ();
         this.savedYaw = minecraft.player.getYRot();
         this.savedPitch = minecraft.player.getXRot();
         this.currentX = this.prevX = this.savedX;
         this.currentY = this.prevY = this.savedY + (double)minecraft.player.getEyeHeight();
         this.currentZ = this.prevZ = this.savedZ;
         this.currentYaw = this.prevYaw = this.savedYaw;
         this.currentPitch = this.prevPitch = this.savedPitch;
         this.savedAbilitiesFlying = minecraft.player.getAbilities().flying;
         this.switchedPerspectiveForBody = false;
         if (this.showPlayerModel.get()) {
            this.perspectiveBeforeFreecam = minecraft.options.getCameraType();
            if (this.perspectiveBeforeFreecam.isFirstPerson()) {
               minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
               this.switchedPerspectiveForBody = true;
            }
         }

         this.activationPending = false;
         this.active = true;
         this.lastSyncedCamChunk = null;
         this.lastSyncedLoadDistance = Integer.MIN_VALUE;
         this.applyViewOnlyClientState(minecraft);
         this.syncFreecamChunkLoading(minecraft);
         reapplyBodyInput(minecraft);
      }
   }

   private void captureMovementLatches(Minecraft minecraft) {
      Options var2 = minecraft.options;
      LocalPlayer var3 = minecraft.player;
      Input var4 = var3 != null ? var3.input.keyPresses : Input.EMPTY;
      this.latchedForward = var2.keyUp.isDown() || var4.forward();
      this.latchedBack = var2.keyDown.isDown() || var4.backward();
      this.latchedLeft = var2.keyLeft.isDown() || var4.left();
      this.latchedRight = var2.keyRight.isDown() || var4.right();
      this.latchedJump = var2.keyJump.isDown() || var4.jump();
      this.latchedSneak = var2.keyShift.isDown() || var4.shift() || var3 != null && var3.isShiftKeyDown();
      this.latchedSprint = var2.keySprint.isDown() || var4.sprint();
      this.mergeLatchFromAutoWalk();
   }

   private void mergeLatchFromAutoWalk() {
      AutoWalkModule var1 = AetherClient.modules() != null ? AetherClient.modules().autoWalk : null;
      if (var1 != null && var1.isEnabled()) {
         this.latchedForward = true;
      }
   }

   private void clearMovementLatches() {
      this.latchedRight = false;
      this.latchedLeft = false;
      this.latchedBack = false;
      this.latchedForward = false;
      this.latchedSprint = false;
      this.latchedSneak = false;
      this.latchedJump = false;
   }

   @Override
   public void onTick() {
      Minecraft var1 = Minecraft.getInstance();
      if (this.isEnabled()) {
         this.tryCompleteActivation(var1);
         if (this.active && var1.player != null) {
            this.prevX = this.currentX;
            this.prevY = this.currentY;
            this.prevZ = this.currentZ;
            this.prevYaw = this.currentYaw;
            this.prevPitch = this.currentPitch;
            float var2 = this.speed.getFloat();
            float var3 = this.verticalMultiplier.getFloat();
            float var4 = this.smoothing.get() ? 0.5F : 1.0F;
            Options var5 = var1.options;
            double var6 = 0.0;
            double var8 = 0.0;
            double var10 = 0.0;
            if (var5.keyUp.isDown()) {
               var6++;
            }

            if (var5.keyDown.isDown()) {
               var6--;
            }

            if (var5.keyLeft.isDown()) {
               var8++;
            }

            if (var5.keyRight.isDown()) {
               var8--;
            }

            if (var5.keyJump.isDown()) {
               var10++;
            }

            if (var5.keyShift.isDown()) {
               var10--;
            }

            LocalPlayer var12 = var1.player;
            if (var12.getAbilities().instabuild) {
               var12.getAbilities().flying = false;
            }

            double var13 = Math.toRadians((double)this.currentYaw);
            double var15 = -Math.sin(var13) * var6 * (double)var2 + Math.cos(var13) * var8 * (double)var2;
            double var17 = Math.cos(var13) * var6 * (double)var2 + Math.sin(var13) * var8 * (double)var2;
            double var19 = var10 * (double)var2 * (double)var3;
            this.currentX += var15 * (double)var4;
            this.currentY += var19 * (double)var4;
            this.currentZ += var17 * (double)var4;
            this.syncFreecamChunkLoading(var1);
         }
      }
   }

   public static void reapplyBodyInput(Minecraft minecraft) {
      FreecamModule var1 = instance;
      if (var1 != null && var1.isActive() && minecraft.player != null) {
         LocalPlayer var2 = minecraft.player;
         boolean var3 = var1.latchedForward;
         boolean var4 = var1.latchedBack;
         boolean var5 = var1.latchedLeft;
         boolean var6 = var1.latchedRight;
         boolean var7 = var1.latchedJump;
         boolean var8 = var1.latchedSneak;
         boolean var9 = var1.latchedSprint;
         var2.input.keyPresses = new Input(var3, var4, var5, var6, var7, var8, var9);
         float var10 = (var5 ? 1.0F : 0.0F) - (var6 ? 1.0F : 0.0F);
         float var11 = (var3 ? 1.0F : 0.0F) - (var4 ? 1.0F : 0.0F);
         Vec2 var12 = new Vec2(var10, var11).normalized();
         ((ClientInputAccessor)var2.input).aetherclient$setMoveVector(var12);
         var2.setShiftKeyDown(var8);
      }
   }

   public boolean hasLatchedLocomotion() {
      return this.latchedForward || this.latchedBack || this.latchedLeft || this.latchedRight || this.latchedJump || this.latchedSneak;
   }

   private void syncFreecamChunkLoading(Minecraft minecraft) {
      if (this.active && minecraft.level != null && minecraft.player != null) {
         ClientChunkCache var2 = minecraft.level.getChunkSource();
         ChunkPos var3 = minecraft.player.chunkPosition();
         int var4 = Math.min(32, Math.max(2, Math.round(this.freecamChunkDistance.getFloat())));
         int var5 = Math.min(32, Math.max(var4, minecraft.options.getEffectiveRenderDistance()));
         if (var5 != this.lastSyncedLoadDistance) {
            var2.updateViewRadius(var5);
            this.lastSyncedLoadDistance = var5;
         }

         if (this.lastSyncedCamChunk == null || var3.x != this.lastSyncedCamChunk.x || var3.z != this.lastSyncedCamChunk.z) {
            var2.updateViewCenter(var3.x, var3.z);
            this.lastSyncedCamChunk = var3;
            if (minecraft.levelRenderer != null) {
               minecraft.levelRenderer.needsUpdate();
            }
         }
      }
   }

   private void restoreVanillaChunkLoading(Minecraft minecraft) {
      this.lastSyncedCamChunk = null;
      this.lastSyncedLoadDistance = Integer.MIN_VALUE;
      if (minecraft.level != null) {
         ClientChunkCache var2 = minecraft.level.getChunkSource();
         if (minecraft.player != null) {
            ChunkPos var3 = minecraft.player.chunkPosition();
            var2.updateViewCenter(var3.x, var3.z);
         }

         var2.updateViewRadius(minecraft.options.getEffectiveRenderDistance());
         if (minecraft.levelRenderer != null) {
            minecraft.levelRenderer.needsUpdate();
         }
      }
   }

   private void applyViewOnlyClientState(Minecraft minecraft) {
      this.savedSmartCull = minecraft.smartCull;
      minecraft.smartCull = false;
   }

   private void restoreViewOnlyClientState(Minecraft minecraft) {
      minecraft.smartCull = this.savedSmartCull;
      if (minecraft.gameMode != null) {
         minecraft.gameMode.stopDestroyBlock();
      }
   }

   public boolean isActive() {
      return this.isEnabled() && this.active;
   }

   public boolean isShowPlayerModel() {
      return this.showPlayerModel.get();
   }

   public boolean isShowHands() {
      return this.showHands.get();
   }

   public boolean renderHands() {
      return !this.isActive() || this.isShowHands();
   }

   public boolean wasHoldingSneak() {
      return this.latchedSneak;
   }

   public double getInterpolatedX(float f) {
      return Mth.lerp((double)f, this.prevX, this.currentX);
   }

   public double getInterpolatedY(float f) {
      return Mth.lerp((double)f, this.prevY, this.currentY);
   }

   public double getInterpolatedZ(float f) {
      return Mth.lerp((double)f, this.prevZ, this.currentZ);
   }

   public float getInterpolatedYaw(float f) {
      return Mth.lerp(f, this.prevYaw, this.currentYaw);
   }

   public float getInterpolatedPitch(float f) {
      return Mth.lerp(f, this.prevPitch, this.currentPitch);
   }

   public Vec3 getInterpolatedPos(float f) {
      return new Vec3(this.getInterpolatedX(f), this.getInterpolatedY(f), this.getInterpolatedZ(f));
   }

   public void setRotation(float f, float g) {
      this.currentYaw = f;
      this.currentPitch = Mth.clamp(g, -90.0F, 90.0F);
   }

   public float getCurrentYaw() {
      return this.currentYaw;
   }

   public float getCurrentPitch() {
      return this.currentPitch;
   }

   public float getLookSensitivity() {
      return this.lookSensitivity.getFloat();
   }
}
