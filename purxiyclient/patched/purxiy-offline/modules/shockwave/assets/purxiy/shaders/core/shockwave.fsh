#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:shockwave.glsl>

// The moving ground of Shockwave (see dev.purxiy.shockwave.ShockRenderer), redrawn from the scene.
//
// Where this pixel shows the top of a known column, its view ray is marched over the tiles as they are now (their tops
// moved by the waves), one cell at a time, from a little above the ground to a little below it:
//
//   top    the ray meets a tile from above: the picture of that tile where it rested (read from a copy of the scene),
//          in the shadow of higher neighbours, lit by the cracks along its edges
//   side   the ray meets the side of a tile: if that side was inside the ground a moment ago, it is the crack between
//          two tiles, dark, with light welling up from its foot; if it was in view before, its picture is moved along
//
// Columns that are not known (nothing that moves) count as level with the ground being drawn.

uniform sampler2D DepthSampler;
uniform sampler2D SceneSampler;
uniform sampler2D GroundSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

const float TAU = 6.28318530718;

struct Cell {
    ivec2 at;     // where it is in the grid
    bool known;   // its top is known (a column that moves, or could)
    int near;     // bit 1: a tile moves within one cell of it; bit 2: within two
    float rest;   // where its top rests (camera-relative height)
    float sink;   // how far it has sunk (blocks; negative: risen)
    float top;    // where its top is now
};

int byteOf(float v) {
    return int(v * 255.0 + 0.5);
}

// The cell in column c of the grid; one that is not known is level with the height given. One texel: all the march needs.
Cell cellAt(ivec2 c, float level) {
    Cell cell = Cell(c, false, 0, level, 0.0, level);
    int n = int(ShockGrid.w + 0.5);
    if (c.x < 0 || c.y < 0 || c.x >= n || c.y >= n) {
        return cell;
    }
    vec4 a = texelFetch(GroundSampler, ivec2(c.x * 3, c.y), 0);
    int high = byteOf(a.r), low = byteOf(a.g);
    int rest = high * 64 + (low >> 2);
    if (rest == 0) {
        return cell;
    }
    cell.known = true;
    cell.near = low & 3;
    cell.rest = ShockGrid.z + float(rest - 8192) / 256.0;
    cell.sink = float(byteOf(a.b) * 256 + byteOf(a.a) - 32768) / 16384.0;
    cell.top = cell.rest - cell.sink;
    return cell;
}

// The light of a cell: how much it shines (0..1) and which way around its wave it lies (turns).
vec2 lightOf(Cell cell) {
    if (!cell.known) {
        return vec2(0.0);
    }
    return texelFetch(GroundSampler, ivec2(cell.at.x * 3 + 1, cell.at.y), 0).rg;
}

// The edges of a cell (west, east, north, south): how far its tile lies below the neighbour there beyond where they rest
// (a crack at its foot), negative for how far above it.
vec4 edgesOf(Cell cell) {
    if (!cell.known) {
        return vec4(0.0);
    }
    return (floor(texelFetch(GroundSampler, ivec2(cell.at.x * 3 + 2, cell.at.y), 0) * 255.0 + 0.5) - 128.0) / 512.0;
}

vec3 scenePoint(vec2 px, float depth) {
    vec2 ndc = px / ShockScreen.xy * 2.0 - 1.0;
    vec4 p = ShockInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// Where a camera-relative point is on screen (pixels).
vec2 toScreen(vec3 p) {
    vec4 clip = ShockViewProj * vec4(p, 1.0);
    return (clip.xy / clip.w * 0.5 + 0.5) * ShockScreen.xy;
}

// The picture of the ground at a point where it rested, as the scene showed it; if the scene did not show that point
// (something stands in front of it, or it is off screen), the fallback.
vec3 pictureAt(vec3 q, vec3 fallback) {
    vec2 s = toScreen(q);
    if (s.x < 0.5 || s.y < 0.5 || s.x > ShockScreen.x - 0.5 || s.y > ShockScreen.y - 0.5) {
        return fallback;
    }
    ivec2 texel = ivec2(s);
#ifdef MASKED
    // your hand is in the picture but not in the depth: never take it for ground
    if (texelFetch(HandMaskSampler, texel, 0).r > 0.5) {
        return fallback;
    }
#endif
    vec3 seen = scenePoint(vec2(texel) + 0.5, texelFetch(DepthSampler, texel, 0).r);
    if (abs(seen.y - q.y) > 0.03 + 0.002 * length(q)) {
        return fallback;
    }
    // the nearest pixel: blocks keep their crisp pixels as they move
    return texelFetch(SceneSampler, texel, 0).rgb;
}

// The palette at a point around a wave (turns): three soft lobes, the accent a little quieter.
vec3 paletteAt(float turns) {
    float a = turns * TAU;
    vec3 w = max(vec3(cos(a), cos(a - TAU / 3.0), cos(a + TAU / 3.0)), 0.0);
    w *= w * vec3(1.0, 1.0, 0.75);
    return (ShockFirst.rgb * w.x + ShockSecond.rgb * w.y + ShockAccent.rgb * w.z) / max(w.x + w.y + w.z, 1.0e-4);
}

// Light that has to read on bright ground too: a little more saturated than the palette.
vec3 vivid(vec3 c) {
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(grey), c, 1.45), 0.0, 1.0);
}

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
#ifdef MASKED
    if (texelFetch(HandMaskSampler, px, 0).r > 0.5) {
        discard;
    }
#endif
    float depth = texelFetch(DepthSampler, px, 0).r;
    if (depth >= 1.0) {
        discard;
    }
    vec3 p = scenePoint(gl_FragCoord.xy, depth);
    Cell own = cellAt(ivec2(floor(p.xz - ShockGrid.xy)), p.y);
    if (!own.known || own.near == 0) {
        discard;
    }
    float dist = length(p);
    // only where the scene shows the top of the ground
    if (abs(p.y - own.rest) > 0.02 + dist * 0.002) {
        discard;
    }
    vec3 v = p / dist;
    float down = -v.y;
    // a ray that looks down steeply cannot reach a tile two cells away before its own
    if (down < 0.004 || ((own.near & 1) == 0 && down > 0.3)) {
        discard;
    }
    vec3 scene = texelFetch(SceneSampler, px, 0).rgb;
    float level = own.rest;

    // march the ray over the tiles as they are now, from above the highest rise to below the deepest sink
    float t0 = max(dist - (ShockLimits.x + 0.03) / down, 0.02);
    float t1 = dist + (ShockLimits.y + 0.03) / down;
    vec2 o = v.xz * t0 - ShockGrid.xy;
    ivec2 cell = ivec2(floor(o));
    ivec2 stride = ivec2(v.x >= 0.0 ? 1 : -1, v.z >= 0.0 ? 1 : -1);
    vec2 inv = 1.0 / max(abs(v.xz), vec2(1.0e-5));
    vec2 edge = vec2(stride.x > 0 ? floor(o.x) + 1.0 - o.x : o.x - floor(o.x), stride.y > 0 ? floor(o.y) + 1.0 - o.y : o.y - floor(o.y));
    vec2 tMax = t0 + edge * inv;
    float tin = t0;
    Cell prev = own;
    Cell struck = own;
    bool started = false;
    int hit = 0;
    float tHit = dist;
    for (int i = 0; i < 12; i++) {
        Cell k = cellAt(cell, level);
        float tout = min(min(tMax.x, tMax.y), t1);
        if (v.y * tin <= k.top) {
            // the ray enters this tile below its top: its side (or, where the march starts, its top)
            hit = started ? 2 : 1;
            tHit = tin;
            struck = k;
            break;
        }
        if (v.y * tout <= k.top) {
            hit = 1;
            tHit = k.top / v.y;
            struck = k;
            break;
        }
        if (tout >= t1) {
            break;
        }
        started = true;
        prev = k;
        tin = tout;
        if (tMax.x < tMax.y) {
            cell.x += stride.x;
            tMax.x += inv.x;
        } else {
            cell.y += stride.y;
            tMax.y += inv.y;
        }
    }
    if (hit == 0) {
        discard;
    }

    vec3 at = v * tHit;
    float fogAmount = total_fog_value(fog_spherical_distance(at), fog_cylindrical_distance(at), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    // the light of the tile hit, and for the side of a tile, of whichever of the two by the crack is livelier
    vec2 light = lightOf(struck);
    if (hit == 2) {
        vec2 other = lightOf(prev);
        if (other.x > light.x) {
            light = other;
        }
    }
    float shine = light.x;
    // how strongly the light shows here: the Glow setting, the fog, and how lively the tile is (brightest as the front strikes)
    float glow = ShockScreen.z * (1.0 - fogAmount) * (0.45 + 0.55 * shine);
    // blocks per pixel here, looking along the ray
    float pixel = tHit / ShockLimits.z;
    // the palette of the wave around here, deeply coloured so it reads on grass and sand as well as at night, whiter at heart
    vec3 tint = vivid(paletteAt(light.y));
    vec3 body = vivid(tint);
    vec3 core = mix(tint, vec3(1.0), 0.6);
    vec3 deep = tint * tint * 0.3;
    // how dark the ground is here: the glow around the seams spreads further on dark ground (on bright ground it would only
    // turn the ground grey)
    float dark = 1.0 - smoothstep(0.08, 0.5, dot(scene, vec3(0.299, 0.587, 0.114)));

    vec3 color = scene;
    // light from the cracks: the seam right at a crack, which covers what is under it, and the glow it casts around, which
    // only adds; and the shadow of higher neighbours
    float seam = 0.0;
    float spread = 0.0;
    float shade = 0.0;
    if (hit == 1) {
        // a tile that barely moved shows as the scene showed it
        if (abs(struck.sink) > 0.0005) {
            color = pictureAt(at + vec3(0.0, struck.sink, 0.0), scene);
        }
        // its four edges: how far it lies below the neighbour there (a crack at its foot), or above it
        vec2 g = at.xz - ShockGrid.xy;
        vec2 f = clamp(g - floor(g), 0.0, 1.0);
        vec4 edges = edgesOf(struck);
        vec4 away = vec4(f.x, 1.0 - f.x, f.y, 1.0 - f.y);
        // lower than a neighbour: in its shadow near that edge, and lit by the crack at the foot of its side
        vec4 deeper = max(edges, 0.0);
        vec4 open = smoothstep(0.008, 0.1, deeper);
        vec4 shadow = smoothstep(0.0, 0.2, deeper) * exp(-away / (0.04 + 0.7 * deeper));
        // at least a pixel and a half wide on screen, so a ring far away still reads
        vec4 near = away / max(0.012 + 0.04 * deeper, vec4(pixel * 1.5));
        vec4 line = open / (1.0 + near * near);
        vec4 glowing = open * exp(-away / ((0.05 + 0.35 * deeper) * (1.0 + 1.6 * dark)));
        // higher than a neighbour: the light rising from the crack below it catches its edge
        vec4 higher = max(-edges, 0.0);
        vec4 rim = away / max(0.01, pixel * 0.8);
        vec4 catching = smoothstep(0.008, 0.1, higher) / (1.0 + rim * rim);
        shade = 1.0 - exp(-(shadow.x + shadow.y + shadow.z + shadow.w));
        color *= 1.0 - 0.35 * shade;
        seam = dot(line, vec4(1.0)) + 0.45 * dot(catching, vec4(1.0));
        spread = dot(glowing, vec4(1.0));
    } else {
        vec3 rested = at + vec3(0.0, struck.sink, 0.0);
        if (rested.y > prev.rest + 0.002) {
            // a side that was in view already (a step in the ground): it moves along with its tile
            color = pictureAt(rested, scene);
        } else if (at.y >= prev.rest) {
            // the side of a tile that rose above the ground: like the side of a block, its top's colours a little darker,
            // lit from the crack at its foot
            vec3 edgeTop = vec3(at.x, struck.rest, at.z) + vec3(v.x, 0.0, v.z) * 0.02;
            color = pictureAt(edgeTop, scene) * 0.86;
            float height = max(struck.top - prev.rest, 1.0e-4);
            float up = clamp((at.y - prev.top) / max(struck.top - prev.top, 1.0e-4), 0.0, 1.0);
            spread = smoothstep(0.004, 0.06, height) * (1.0 - up) * 0.5;
        } else {
            // the crack: from the top of the lower tile in front (its foot) up to the ground at rest (or the edge of the
            // higher tile). Its walls are dark, with light welling up in it: almost white at its foot, the palette above,
            // dimmer towards the top. A crack thinner than a pixel on screen only shows as much of its light as it covers
            float foot = prev.top;
            float gap = max(min(struck.top, min(prev.rest, prev.rest - struck.sink)) - foot, 1.0e-4);
            float up = clamp((at.y - foot) / gap, 0.0, 1.0);
            float thick = smoothstep(0.03, 0.09, gap) * smoothstep(0.8, 3.5, gap / pixel);
            vec3 hole = scene * (0.2 + 0.2 * up) + deep * (1.0 - up) * 0.6;
            color = mix(scene, hole, thick);
            float open = smoothstep(0.004, 0.06, gap) * min(gap / pixel, 1.0);
            vec3 lamp = mix(core, body, smoothstep(0.0, 0.5, up)) * (1.0 - 0.6 * smoothstep(0.35, 1.0, up));
            color = mix(color, lamp, clamp(open * glow * 1.7, 0.0, 0.94));
            spread = open * (1.0 - up) * 0.8;
        }
    }

    // the seams cover what is under them, the glow around them adds; a tile the front just struck lights up for a moment,
    // keeping its own colours. All of it is brightest while the tiles move and fades as they settle
    float gs = glow * seam;
    float gg = glow * spread * (0.1 + 0.9 * dark);
    float flash = hit == 1 ? ShockScreen.z * (1.0 - fogAmount) * shine : 0.0;
    color *= 1.0 + 0.22 * flash;
    color = mix(color, mix(body, core, smoothstep(0.5, 1.4, gs)), 1.0 - exp(-gs * 1.6)) + body * gg * 0.3;
    float g = gs + gg + flash;
    if (hit == 1 && g < 0.002 && abs(struck.sink) <= 0.0005 && shade < 0.002) {
        // nothing changed here
        discard;
    }

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    color = max(color + (dither - 0.5) / 255.0 * step(0.001, g), 0.0);
    fragColor = vec4(min(color, vec3(1.0)), 1.0);
}
