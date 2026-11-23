package dev.aether.module.impl;

import java.util.Optional;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SpearLungeModule extends Module {
    private static final int BUFFER_TICKS = 10;
    private static final double POKE_RANGE = 4.4;
    private static final double POKE_MARGIN = 0.125;
    private final KeybindSetting key = this.addSetting(new KeySetting("Lunge Key", -1));
    private final BooleanSetting reachPoke = this.addSetting(new BooleanSetting("Reach Poke", false));
    private final BooleanSetting switchBack = this.addSetting(new BooleanSetting("Switch Back", true));
    private final SlotReturn slots = new SlotReturn(this);
    private boolean pending;
    private int pendingTicks;
    private LocalPlayer owner;

    public SpearLungeModule() {
        super("spear-swap", "Swaps to your Lunge spear and lunges. Reach Poke stabs with a spear past sword reach.", Category.COMBAT);
    }

    @Override
    protected void onEnable() {
        this.pending = false;
        this.owner = Minecraft.getInstance().player;
    }

    @Override
    protected void onDisable() {
        this.pending = false;
        this.owner = null;
        this.slots.drop(Minecraft.getInstance(), this.switchBack.get());
    }

//     private void onKey(KeyInputEvent event) {
        if (event.action == 1) {
            this.onPress(event.key);
        }
    }

//     private void onMouse(MouseInputEvent event) {
        if (event.action == 1) {
            this.onPress(1000 + event.button);
        }
    }

    private void onPress(int pressed) {
        int bound = this.key.value();
//         if (ModuleKeybinds.isBindable(bound) && pressed == bound) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null && mc.player != null && mc.level != null) {
                if (bestSpear(mc.player.getInventory(), true) < 0) {
//                     ModuleChat.actionBar(this, ModuleChat.body().warn("No Lunge spear").text(" in your hotbar"));
                } else {
                    this.pending = true;
                    this.pendingTicks = 0;
                }
            }
        }
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        this.slots.tick(mc, this.switchBack.get());
        LocalPlayer player = mc.player;
        if (player != this.owner) {
            this.owner = player;
            this.pending = false;
        }

        if (player != null && mc.level != null && mc.gameMode != null) {
            if (this.pending) {
                this.tryLunge(mc, player);
            }

            if (this.reachPoke.get() && MeleeAttacks.clickPending(mc)) {
                this.tryPoke(mc, player);
            }
        }
    }

    private void tryLunge(Minecraft mc, LocalPlayer player) {
        if (++this.pendingTicks > 10) {
            this.pending = false;
        } else if (this.canStab(mc, player) && !player.isInWater() && !player.isFallFlying() && !player.isPassenger()) {
            if (player.getFoodData().hasEnoughFood() || player.getAbilities().mayfly) {
                Inventory inventory = player.getInventory();
                int slot = bestSpear(inventory, true);
                if (slot < 0) {
                    this.pending = false;
                } else {
                    ItemStack spear = inventory.getItem(slot);
                    if (canJab(mc, player, spear) && this.slots.take(player, slot)) {
                        this.pending = false;
                        stab(mc, player, spear.get(DataComponents.PIERCING_WEAPON));
                    }
                }
            }
        }
    }

    private void tryPoke(Minecraft mc, LocalPlayer player) {
        if (this.canStab(mc, player) && !player.getMainHandItem().has(DataComponents.PIERCING_WEAPON)) {
            if (MeleeAttacks.crosshairHit(mc) == null) {
                Inventory inventory = player.getInventory();
                int slot = bestSpear(inventory, false);
                if (slot >= 0) {
                    ItemStack spear = inventory.getItem(slot);
                    if (canJab(mc, player, spear) && enemyPastReach(player) && this.slots.take(player, slot)) {
                        stab(mc, player, spear.get(DataComponents.PIERCING_WEAPON));
                    }
                }
            }
        }
    }

    private static void stab(Minecraft mc, LocalPlayer player, PiercingWeapon weapon) {
        mc.gameMode.piercingAttack(weapon);
        if (MeleeAttacks.actedThisTick(player)) {
            player.swing(InteractionHand.MAIN_HAND);
            MeleeAttacks.claimTick(mc);
        }
    }

    private boolean canStab(Minecraft mc, LocalPlayer player) {
        return MeleeAttacks.canAct(mc) && MeleeAttacks.handsFree(mc, player) && !MeleeAttacks.actedThisTick(player)
            ? !CombatMacroLock.heldByOther(this) && !AutoInvTotemModule.wantsHotbar()
            : false;
    }

    private static boolean canJab(Minecraft mc, LocalPlayer player, ItemStack spear) {
        return mc.missTime <= 0 && !mc.gameMode.isSpectator()
            ? spear.isItemEnabled(mc.level.enabledFeatures()) && !player.cannotAttackWithItem(spear, 0)
            : false;
    }

    private static boolean enemyPastReach(LocalPlayer player) {
        Rotation sent = ServerRotation.sent();
        Vec3 look = sent != null ? Vec3.directionFromRotation(sent.pitch(), sent.yaw()) : player.getViewVector(1.0F);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(look.scale(4.4));
        double reach = player.entityInteractionRange();
        AABB area = player.getBoundingBox().expandTowards(look.scale(4.4)).inflate(1.0);

        for (Entity entity : player.level().getEntities(player, area)) {
            if (MeleeAttacks.isEnemy(player, entity)) {
                Optional<Vec3> at = entity.getBoundingBox().inflate(0.125).clip(eye, end);
                if (at.isPresent() && eye.distanceTo(at.get()) > reach && MeleeAttacks.lineIsClear(player, eye, at.get())) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int bestSpear(Inventory inventory, boolean lunge) {
        int selected = inventory.getSelectedSlot();
        int best = -1;
        int bestLevel = 0;

        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.has(DataComponents.PIERCING_WEAPON) && !stack.nextDamageWillBreak()) {
                int level = lungeLevel(stack);
                boolean better = !lunge ? best < 0 || level < bestLevel : level > bestLevel || level > 0 && level == bestLevel && slot == selected;
                if (better) {
                    best = slot;
                    bestLevel = level;
                }
            }
        }

        return best;
    }

    private static int lungeLevel(ItemStack stack) {
        for (Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(Enchantments.LUNGE)) {
                return entry.getIntValue();
            }
        }

        return 0;
    }
}
