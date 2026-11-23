package dev.aether.module.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import dev.aether.module.Module;
import dev.aether.module.Category;
import dev.aether.settings.BooleanSetting;
import dev.aether.settings.ColorSetting;
import dev.aether.settings.ModeSetting;
import dev.aether.settings.SliderSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

public class TracersModule extends Module {
    private static final double DISTANCE_COLOR_RANGE = 60.0;
    private static final float ARROW_TIP = 0.73205F;
    private static final int WHITE = 16777215;
    private final BooleanSetting players = this.addSetting(new BooleanSetting("Players", true));
    private final BooleanSetting animals = this.addSetting(new BooleanSetting("Animals", false));
    private final BooleanSetting waterAnimals = this.addSetting(new BooleanSetting("Water Animals", false));
    private final BooleanSetting monsters = this.addSetting(new BooleanSetting("Monsters", false));
    private final BooleanSetting ambient = this.addSetting(new BooleanSetting("Ambient", false));
    private final BooleanSetting misc = this.addSetting(new BooleanSetting("Misc", false));
    private final BooleanSetting ignoreSelf = this.addSetting(new BooleanSetting("Ignore Self", false));
    private final BooleanSetting ignoreFriends = this.addSetting(new BooleanSetting("Ignore Friends", false));
    private final BooleanSetting showInvisible = this.addSetting(new BooleanSetting("Show Invisible", true));
    private final ModeSetting style = this.addSetting(new ModeSetting("Style", "Lines", "Lines", "Offscreen"));
    private final ModeSetting target = this.addSetting(new ModeSetting("Target", "Body", "Head", "Body", "Feet"));
    private final BooleanSetting stem = this.addSetting(new BooleanSetting("Stem", true));
    private final SliderSetting maxDistance = this.addSetting(new SliderSetting("Max Distance", 256.0, 0.0, 256.0, 1.0F));
    private final SliderSetting distanceOffscreen = this.addSetting(
        "distance-offscreen", new SliderSetting("Distance Offscreen", 200.0, 0.0, 500.0, 1.0F)
    );
    private final SliderSetting sizeOffscreen = this.addSetting(new SliderSetting("Size Offscreen", 10.0, 2.0, 50.0, 1.0F));
    private final BooleanSetting blinkOffscreen = this.addSetting(new BooleanSetting("Blink Offscreen", true));
    private final SliderSetting blinkOffscreenSpeed = this.addSetting(
        "blink-offscreen-speed", new SliderSetting("Blink Offscreen Speed", 4.0, 1.0, 15.0, 1.0F)
    );
    private final BooleanSetting distanceColors = this.addSetting(new BooleanSetting("Distance Colors", false));
    private final BooleanSetting friendOverride = this.addSetting(new BooleanSetting("Show Friend Colors", true));
    private final BooleanSetting teamColors = this.addSetting(new BooleanSetting("Team Colors", true));
    private final ColorSetting friendColor = this.addSetting(new ColorSetting("Friend Color", -16711756));
    private final ColorSetting playersColor = this.addSetting(new ColorSetting("Players Color", 2144193997));
    private final ColorSetting animalsColor = this.addSetting(new ColorSetting("Animals Color", 2140274577));
    private final ColorSetting waterAnimalsColor = this.addSetting(new ColorSetting("Water Animals Color", 2140246527));
    private final ColorSetting monstersColor = this.addSetting(new ColorSetting("Monsters Color", 2147455377));
    private final ColorSetting ambientColor = this.addSetting(new ColorSetting("Ambient Color", 2135640907));
    private final ColorSetting miscColor = this.addSetting(new ColorSetting("Misc Color", 2140246417));

    public TracersModule() {
        super("tracers", "Displays tracer lines to specified entities.", Category.RENDER);
        this.target.visibleWhen(() -> this.style.is("Lines"));
        this.stem.visibleWhen(() -> this.style.is("Lines"));
        this.distanceOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.sizeOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.blinkOffscreen.visibleWhen(() -> this.style.is("Offscreen"));
        this.blinkOffscreenSpeed.visibleWhen(() -> this.style.is("Offscreen") && this.blinkOffscreen.get());
        this.friendOverride.visibleWhen(() -> this.distanceColors.get() && !this.ignoreFriends.get());
        this.teamColors.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
        this.friendColor.visibleWhen(() -> !this.ignoreFriends.get() && this.players.get() && (!this.distanceColors.get() || this.friendOverride.get()));
        this.playersColor.visibleWhen(() -> !this.distanceColors.get() && this.players.get());
        this.animalsColor.visibleWhen(() -> !this.distanceColors.get() && this.animals.get());
        this.waterAnimalsColor.visibleWhen(() -> !this.distanceColors.get() && this.waterAnimals.get());
        this.monstersColor.visibleWhen(() -> !this.distanceColors.get() && this.monsters.get());
        this.ambientColor.visibleWhen(() -> !this.distanceColors.get() && this.ambient.get());
        this.miscColor.visibleWhen(() -> !this.distanceColors.get() && this.misc.get());
    }

//     @EventHandler
//     public void onRender3D(Render3DEvent event) {
        // Minecraft mc = Minecraft.getInstance();
        // if (!mc.options.hideGui && this.style.is("Lines")) {
            // for (Entity entity : this.targets(mc)) {
                // int color = this.colorOf(mc, entity);
                // Vec3 feet = entity.getPosition(event.partialTick);
                // double height = entity.getBoundingBox().getYsize();

                // double up = switch (this.target.get()) {
                    // case "Head" -> height;
                    // case "Body" -> height / 2.0;
                    // default -> 0.0;
                // };
                // TracerEngine.draw(feet.add(0.0, up, 0.0), color);
                // if (this.stem.get()) {
                    // TracerEngine.stem(feet, height, color);
                // }
            // }
        // }
    // }

    @Override
    public boolean isHudOverlayEnabled() {
        return this.isEnabled() && this.style.is("Offscreen") && !Minecraft.getInstance().options.hideGui;
    }

    @Override
    // public void renderHudOverlay(ImDrawList dl, float screenW, float screenH) {
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float blink = this.blinkOffscreen.get() ? this.blinkAlpha() : 1.0F;
        float reach = this.distanceOffscreen.get();
        float size = this.sizeOffscreen.get();

        for (Entity entity : this.targets(mc)) {
            Vector4f clip = TracerEngine.clip(entity.getPosition(partialTick));
            if (clip == null) {
                return;
            }

            if (!(clip.w > 0.0F) || !(Math.abs(clip.x) < clip.w) || !(Math.abs(clip.y) < clip.w)) {
                double angle = Math.atan2(-clip.y * screenH, clip.x * screenW);
                int color = this.colorOf(mc, entity);
                int alpha = (int)((color >>> 24) * blink);
                int tracerColor = (alpha << 24) | (color & 16777215);
                arrow(
                    dl,
                    screenW * 0.5F + reach * (float)Math.cos(angle),
                    screenH * 0.5F + reach * (float)Math.sin(angle),
                    size,
                    (float)angle,
                    tracerColor
                );
            }
        }
    }

    private List<Entity> targets(Minecraft mc) {
        if (mc.level != null && mc.player != null) {
//             FreecamModule freecam = ModuleRegistry.get(FreecamModule.class);
            boolean skipSelf = this.ignoreSelf.get() || freecam == null || !freecam.isEnabled();
            double maxSq = (double)this.maxDistance.get() * this.maxDistance.get();
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            double view = (mc.options.renderDistance().get() + 1) * 16.0;
            List<Entity> result = new ArrayList<>();

            for (Entity entity : mc.level.entitiesForRendering()) {
                if ((entity != mc.player || !skipSelf)
                    && this.groupColor(entity) != null
                    && !(entity.distanceToSqr(mc.player) > maxSq)
                    && (!this.ignoreFriends.get() || !isFriend(entity))
                    && (this.showInvisible.get() || !entity.isInvisible())
                    && !(Math.abs(camera.x - entity.getX()) >= view)
                    && !(Math.abs(camera.z - entity.getZ()) >= view)) {
                    result.add(entity);
                }
            }

            return result;
        } else {
            return List.of();
        }
    }

    private ColorSetting groupColor(Entity entity) {
        if (entity instanceof Player) {
            return this.players.get() ? this.playersColor : null;
        }

        return switch (entity.getType().getCategory()) {
            case CREATURE -> this.animals.get() ? this.animalsColor : null;
            case WATER_AMBIENT, WATER_CREATURE, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> this.waterAnimals.get() ? this.waterAnimalsColor : null;
            case MONSTER -> this.monsters.get() ? this.monstersColor : null;
            case AMBIENT -> this.ambient.get() ? this.ambientColor : null;
            default -> this.misc.get() ? this.miscColor : null;
        };
    }

    private static boolean isFriend(Entity entity) {
//         return entity instanceof Player player && FriendStore.getInstance().isFriend(player.getGameProfile().name());
    }

    private int colorOf(Minecraft mc, Entity entity) {
        boolean friend = isFriend(entity);
        if (!this.distanceColors.get()) {
            if (entity instanceof Player player) {
                int base = this.playersColor.argb();
                int alpha = base & 0xFF000000;
                if (friend) {
                    return alpha | this.friendColor.rgb();
                }

                int team = this.teamColors.get() ? teamColor(player) : 16777215;
                return team == 16777215 ? base : alpha | team;
            } else {
                return this.groupColor(entity).argb();
            }
        } else {
            return friend && this.friendOverride.get() ? this.friendColor.argb() : distanceColor(mc, entity);
        }
    }

    private static int distanceColor(Minecraft mc, Entity entity) {
        double percent = mc.gameRenderer.getMainCamera().position().distanceTo(entity.getEyePosition()) / 60.0;
        if (percent > 1.0) {
            return -16711936;
        }

        int r = percent < 0.5 ? 255 : 255 - (int)(255.0 * (percent - 0.5) / 0.5);
        int g = percent < 0.5 ? (int)(255.0 * percent / 0.5) : 255;
        return 0xFF000000 | r << 16 | g << 8;
    }

    private static int teamColor(Player player) {
        Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
        player.getDisplayName().visit((segment, text) -> {
            TextColor color = segment.getColor();
            counts.addTo(color == null ? 16777215 : color.getValue(), text.length());
            return Optional.empty();
        }, Style.EMPTY);
        int best = 16777215;
        int bestCount = 0;

        for (Entry entry : counts.int2IntEntrySet()) {
            if (entry.getIntValue() > bestCount) {
                best = entry.getIntKey();
                bestCount = entry.getIntValue();
            }
        }

        return best;
    }

    private float blinkAlpha() {
        double time = Util.getMillis() * (this.blinkOffscreenSpeed.get() / 4.0);
        return (float)Math.abs(time % 1000.0 - 500.0) / 500.0F;
    }

    // private static void arrow(ImDrawList dl, float x, float y, float size, float angle, int abgr) {
        float cos = (float)Math.cos(angle);
        float sin = (float)Math.sin(angle);
        float cx = x - size * 1.26795F / 3.0F;
        float[] px = new float[]{x - size, x + size * 0.73205F, x - size};
        float[] py = new float[]{y - size, y, y + size};

        for (int i = 0; i < 3; i++) {
            float dx = px[i] - cx;
            float dy = py[i] - y;
            px[i] = cx + dx * cos - dy * sin;
            py[i] = y + dx * sin + dy * cos;
        }

        dl.addTriangleFilled(px[0], py[0], px[1], py[1], px[2], py[2], abgr);
    }
}
