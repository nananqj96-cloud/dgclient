package com.dg.client.module.modules.esp;

import com.dg.client.DGClient;
import com.dg.client.event.EventHandler;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws bobbing-safe batched 3D tracer lines from screen crosshair/bottom/top to filtered entities.
 */
public final class TracersModule extends Module {
    public enum Anchor {
        CROSSHAIR(0.0f),
        BOTTOM(-0.90f),
        TOP(0.90f);

        private final float ndcY;

        Anchor(float ndcY) {
            this.ndcY = ndcY;
        }

        public float getNdcY() {
            return ndcY;
        }
    }

    private final SettingGroup targets = createGroup("Targets");
    private final SettingGroup style = createGroup("Style");

    public final BooleanSetting players = targets.add(new BooleanSetting("Players", "Trace players", true));
    public final BooleanSetting hostiles = targets.add(new BooleanSetting("Hostiles", "Trace hostile mobs", false));
    public final BooleanSetting passives = targets.add(new BooleanSetting("Passives", "Trace passive animals", false));
    public final BooleanSetting items = targets.add(new BooleanSetting("Items", "Trace item drops", false));
    public final IntSetting maxRange = targets.add(new IntSetting("Max Range", "Maximum tracer distance", 160, 16, 384, 8));

    public final EnumSetting<Anchor> anchor = style.add(new EnumSetting<>("Anchor", "Screen origin point", Anchor.CROSSHAIR));
    public final DoubleSetting lineWidth = style.add(new DoubleSetting("Width", "Tracer line width", 1.5, 0.8, 3.5, 0.1));
    public final ColorSetting nearColor = style.add(new ColorSetting("Near Color", "Color for close targets", ColorUtil.PURE_WHITE));
    public final ColorSetting farColor = style.add(new ColorSetting("Far Color", "Color for distant targets", ColorUtil.NEON_GREEN));

    public TracersModule() {
        super("Tracers", "Draws distance-graded 3D tracer lines to selected entity types", ModuleCategory.ESP);
    }

    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || mc.player == null) {
            return;
        }
        float tickDelta = event.getTickDelta();
        double maxDist = maxRange.getValue();
        double maxDistSq = maxDist * maxDist;

        Vec3d startWorld = Render3DUtil.computeTracerOrigin(
                event.getCamera(),
                DGClient.getInstance().getLastViewMatrix(),
                DGClient.getInstance().getLastProjectionMatrix(),
                0.0f,
                anchor.getValue().getNdcY()
        );

        List<Render3DUtil.LineSegment> batch = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !e.isAlive()) {
                continue;
            }
            if (!matches(e)) {
                continue;
            }
            double dSq = mc.player.squaredDistanceTo(e);
            if (dSq > maxDistSq) {
                continue;
            }
            float t = (float) MathHelper.clamp(Math.sqrt(dSq) / maxDist, 0.0, 1.0);
            int col = (e instanceof PlayerEntity p && DGClient.getInstance().getFriendManager().isFriend(p.getName().getString()))
                    ? ColorUtil.MINT_GREEN
                    : ColorUtil.lerp(nearColor.getColor(), farColor.getColor(), t);

            Vec3d targetPos = EspRenderHelper.interpolatePos(e, tickDelta).add(0.0, e.getHeight() * 0.5, 0.0);
            batch.add(new Render3DUtil.LineSegment(
                    startWorld,
                    targetPos,
                    ColorUtil.withAlpha(col, 195),
                    ColorUtil.withAlpha(col, 240),
                    lineWidth.getFloatValue()
            ));
        }

        Render3DUtil.drawLinesBatch(event.getMatrices(), event.getCamera(), batch, true);
    }

    private boolean matches(Entity e) {
        if (e instanceof PlayerEntity) return players.getValue();
        if (e instanceof HostileEntity) return hostiles.getValue();
        if (e instanceof PassiveEntity) return passives.getValue();
        if (e instanceof ItemEntity) return items.getValue();
        return false;
    }
}
