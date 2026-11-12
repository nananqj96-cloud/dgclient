package com.dg.client.module.modules.esp;

import com.dg.client.event.EventHandler;
import com.dg.client.event.events.Render2DEvent;
import com.dg.client.event.events.Render3DEvent;
import com.dg.client.module.Module;
import com.dg.client.module.ModuleCategory;
import com.dg.client.render.Render3DUtil;
import com.dg.client.setting.BooleanSetting;
import com.dg.client.setting.ColorSetting;
import com.dg.client.setting.DoubleSetting;
import com.dg.client.setting.EnumSetting;
import com.dg.client.setting.IntSetting;
import com.dg.client.setting.SettingGroup;
import com.dg.client.util.ColorUtil;
import com.dg.client.util.MathUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Categorized Entity ESP (Hostile, Passive, Item, Projectile/Crystal, Vehicle)
 * with all 6 render modes, frustum culling, invisible entity support, and target cap.
 */
public final class EntityEspModule extends Module {
    private final SettingGroup catGroup = createGroup("Categories");

    public final EnumSetting<EspRenderHelper.RenderMode> mode = addSetting(new EnumSetting<>(
            "Mode", "ESP render mode", EspRenderHelper.RenderMode.BOX_BOTH));
    public final DoubleSetting range = addSetting(new DoubleSetting(
            "Range", "Maximum render distance in blocks", 96.0, 16.0, 256.0, 8.0));
    public final BooleanSetting throughWalls = addSetting(new BooleanSetting(
            "Through Walls", "Draws ESP through solid terrain", true));
    public final BooleanSetting showInvisible = addSetting(new BooleanSetting(
            "Invisibles", "Includes invisible mobs and entities", true));
    public final BooleanSetting showLabels = addSetting(new BooleanSetting(
            "Labels", "Displays entity name, HP, and distance", false));
    public final IntSetting maxTargets = addSetting(new IntSetting(
            "Max Targets", "Performance cap on rendered entities", 80, 10, 250, 10));

    public final BooleanSetting hostiles = catGroup.add(new BooleanSetting("Hostile", "Monsters & hostile mobs", true));
    public final BooleanSetting passives = catGroup.add(new BooleanSetting("Passive", "Animals, villagers, ambient mobs", true));
    public final BooleanSetting items = catGroup.add(new BooleanSetting("Items", "Dropped item stacks", false));
    public final BooleanSetting projectiles = catGroup.add(new BooleanSetting("Projectiles & Crystals", "Arrows, pearls, tridents, end crystals", true));
    public final BooleanSetting vehicles = catGroup.add(new BooleanSetting("Vehicles", "Boats and minecarts", true));

    public final ColorSetting hostileColor = addSetting(new ColorSetting("Hostile Color", "Hostile accent", ColorUtil.NEON_GREEN));
    public final ColorSetting passiveColor = addSetting(new ColorSetting("Passive Color", "Passive accent", ColorUtil.MINT_GREEN));
    public final ColorSetting miscColor = addSetting(new ColorSetting("Misc Color", "Item/Projectile/Vehicle accent", ColorUtil.PURE_WHITE));

    private final List<EspRenderHelper.ProjectedRect> rects2d = new ArrayList<>();

    public EntityEspModule() {
        super("EntityESP", "Highlights hostile, passive, item, projectile, and vehicle entities", ModuleCategory.RENDER);
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue().name();
    }

    public boolean shouldShaderOutline(Entity entity) {
        if (!isEnabled() || mode.getValue() != EspRenderHelper.RenderMode.SHADER_GLOW) {
            return false;
        }
        if (entity == null || entity instanceof PlayerEntity || mc.player == null) {
            return false;
        }
        if (entity.isInvisible() && !showInvisible.getValue()) {
            return false;
        }
        if (mc.player.squaredDistanceTo(entity) > range.getValue() * range.getValue()) {
            return false;
        }
        return classifyColor(entity) != 0;
    }

    public int classifyColor(Entity e) {
        if (e instanceof Monster) {
            return hostiles.getValue() ? hostileColor.getColor() : 0;
        }
        if (e instanceof PassiveEntity || (e instanceof LivingEntity && !(e instanceof PlayerEntity))) {
            return passives.getValue() ? passiveColor.getColor() : 0;
        }
        if (e instanceof ItemEntity) {
            return items.getValue() ? miscColor.getColor() : 0;
        }
        if (e instanceof ProjectileEntity || e instanceof EndCrystalEntity) {
            return projectiles.getValue() ? ColorUtil.NEON_GREEN : 0;
        }
        if (e instanceof BoatEntity || e instanceof AbstractMinecartEntity) {
            return vehicles.getValue() ? miscColor.getColor() : 0;
        }
        return 0;
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        rects2d.clear();
        if (mc.world == null || mc.player == null) {
            return;
        }
        double maxSq = range.getValue() * range.getValue();
        List<Entity> matched = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof PlayerEntity || !e.isAlive()) continue;
            if (e.isInvisible() && !showInvisible.getValue()) continue;
            if (mc.player.squaredDistanceTo(e) > maxSq) continue;
            if (classifyColor(e) == 0) continue;
            matched.add(e);
        }
        matched.sort(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)));

        float delta = event.getTickDelta();
        int count = 0;
        for (Entity e : matched) {
            if (count >= maxTargets.getValue()) {
                break;
            }
            double ix = MathUtil.lerp(e.prevX, e.getX(), delta) - e.getX();
            double iy = MathUtil.lerp(e.prevY, e.getY(), delta) - e.getY();
            double iz = MathUtil.lerp(e.prevZ, e.getZ(), delta) - e.getZ();
            Box box = e.getBoundingBox().offset(ix, iy, iz).expand(0.03);
            if (event.getFrustum() != null && !event.getFrustum().isVisible(box.expand(0.25))) {
                continue;
            }
            int col = classifyColor(e);
            float hp = (e instanceof LivingEntity le)
                    ? MathUtil.clamp(le.getHealth() / Math.max(1.0f, le.getMaxHealth()), 0.0f, 1.0f)
                    : -1.0f;

            EspRenderHelper.renderTargetBox(event, box, mode.getValue(), col, 32, 1.5f,
                    throughWalls.getValue(), rects2d, hp);

            if (showLabels.getValue()) {
                double dist = Math.sqrt(mc.player.squaredDistanceTo(e));
                String label = e.getName().getString()
                        + (e instanceof LivingEntity le ? String.format(Locale.ROOT, " %.0fHP", le.getHealth()) : "")
                        + String.format(Locale.ROOT, " %.0fm", dist);
                Render3DUtil.drawBillboardLabel(event.getMatrices(), event.getCamera(),
                        new Vec3d(box.getCenter().x, box.maxY + 0.25, box.getCenter().z),
                        label, ColorUtil.PURE_WHITE, col, 0.85f, throughWalls.getValue());
            }
            count++;
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (!rects2d.isEmpty() && !mc.options.hudHidden) {
            EspRenderHelper.draw2DProjectedRects(event.getContext(), rects2d);
        }
    }
}
