#version 330

#moj_import <purxiy:fire.glsl>

// Low Fire (see dev.purxiy.fire.PurxiyFire): the fire you see while you burn, in one pass over the bottom of the
// screen, then its embers.
//
//   flames   tongues of fire along the bottom edge, taller towards the sides so the middle stays clear: a wave runs up
//            each one and curls its tip, now and then a wisp breaks off it; a deeper layer of taller tongues burns
//            behind them, and they all rise from one body of fire. Crisp, anti-aliased edges, a white-hot heart low
//            down, a soft light around them (Flames)
//   pixel    Minecraft's own fire, recoloured into the palette, tiled low along the edge (Pixel)
//   glow     only the light of the flames, as if they burned just below the screen (Glow)
//   haze     the air above the flames shimmers
//   events   a burst of light that rises from the bottom edge as you catch fire, a flare each time the fire hurts you,
//            steam that billows up when it is put out
//   embers   sparks that drift up and cool into the palette (little squares in the Pixel style)

uniform sampler2D PixelSampler;
uniform sampler2D SceneSampler;

flat in int ember;
in vec2 local;

out vec4 fragColor;

const float TAU = 6.28318530718;
const vec3 WHITE = vec3(1.0);

// ---------------------------------------------------------------------------------------------------------- noise

// Hash by Dave Hoskins (MIT).
float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = p - i;
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x), mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

// Octaves of noise, each turned against the last so no grid shows. 0..1.
float fbm(vec2 p, int octaves) {
    const mat2 TURN = mat2(1.6, 1.2, -1.2, 1.6);
    float value = 0.0;
    float amplitude = 0.5;
    float total = 0.0;
    for (int i = 0; i < octaves; i++) {
        value += amplitude * noise(p);
        total += amplitude;
        p = TURN * p;
        amplitude *= 0.5;
    }
    return value / total;
}

// ---------------------------------------------------------------------------------------------------------- helpers

// Premultiplied "src over dst".
vec4 over(vec4 dst, vec4 src) {
    return src + dst * (1.0 - src.a);
}

// Premultiplied, from a colour and its coverage.
vec4 paint(vec3 c, float alpha) {
    return vec4(c * alpha, alpha);
}

// Light: premultiplied, but it mostly adds to what is behind it.
vec4 light(vec3 c, float amount, float cover) {
    return vec4(c * amount, amount * cover);
}

// Smooth minimum: tongues merge into each other with soft valleys.
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// Under Fire Resistance the fire calms down: paler, towards the first colour.
vec3 calmed(vec3 c) {
    return mix(c, mix(FireFirst.rgb, WHITE, 0.4), 0.45 * FireStyle.w);
}

// How high the flames reach across the screen, as a part of their height: lower in the middle, where you look.
float frameProfile(float x, float aspect) {
    float side = abs(x / aspect - 0.5) * 2.0;
    return mix(0.55, 1.0, smoothstep(0.1, 0.95, side));
}

// ---------------------------------------------------------------------------------------------------------- flames

// Tongue i of layer l (both in the table) at p: the signed distance to its edge (screen heights, negative inside) and
// how far up it p lies (u, 1 at its tip). A tongue is widest low down and narrows to a tip that a wave, running up it,
// curls from side to side.
float tongue(int l, int i, vec2 p, out float u) {
    int at = (l * MAX_TONGUES + i) * 2;
    vec4 a = FireTongues[at];
    vec4 b = FireTongues[at + 1];
    float h = max(a.y, 1.0e-4);
    float w = a.z;
    u = p.y / h;
    if (u > 1.6) {
        // far above it (beyond any wisp): the distance to its tip is close enough
        return length(vec2(p.x - a.x - a.w, p.y - h));
    }
    float along = clamp(u, 0.0, 1.0);
    // the sway and the wave grow towards the tip (u^1.5), the width narrows to it ((1 - u)^0.75, a little fuller low down)
    float up = clamp(u, 0.0, 1.3);
    float centre = a.x + (a.w + sin(TAU * 1.25 * u - b.x) * b.y) * up * sqrt(up);
    float rest = sqrt(1.0 - along);
    float width = w * rest * sqrt(rest) * (1.0 + 0.72 * along * (1.0 - along));
    float d = abs(p.x - centre) - width;
    if (u > 1.0) {
        d = max(d, length(vec2(p.x - centre, p.y - h)));
    }
    if (b.z >= 0.0) {
        // a wisp breaks off the tip: a small drop, round below and pointed above, that rises and burns away
        float q = b.z;
        float wy = h * (1.0 + 0.3 * q);
        float dy = (p.y - wy) / (p.y > wy ? 2.2 : 1.0);
        d = min(d, length(vec2(p.x - b.w, dy)) - w * 0.42 * (1.0 - q));
    }
    return d;
}

// All tongues of a layer that can reach p, merged; u receives how far up the nearest one p lies. The nearest cells go
// first, and a tongue that stands too far aside to change anything is skipped: no part of it, wisps included, lies
// more than 1.8 half widths from where it stands (see PurxiyFire#tongues), and smin leaves the result as it is for
// one at least `round` further away than the result so far.
float tongues(int l, vec2 p, float spacing, int range, float round, out float u) {
    float shift = 0.5 * spacing * float(l);
    // the table starts two cells left of the screen
    int cell = int(floor((p.x - shift) / spacing)) + 2;
    float d = 10.0;
    u = 2.0;
    for (int j = 0; j <= 2 * range; j++) {
        int i = cell + ((j & 1) == 0 ? j / 2 : -(j + 1) / 2);
        if (i < 0 || i >= MAX_TONGUES) {
            continue;
        }
        vec4 a = FireTongues[(l * MAX_TONGUES + i) * 2];
        if (abs(p.x - a.x) - 1.8 * a.z >= d + round) {
            continue;
        }
        float ui;
        float di = tongue(l, i, p, ui);
        if (di < d) {
            u = ui;
        }
        d = smin(d, di, round);
    }
    return d;
}

vec4 softFlames(vec2 uv, float aspect, float t, float px) {
    float height = FireShape.x;
    float size = FireLook.z;
    float glowStyle = FireStyle.z;
    // how far the light around the flames reaches: it is gone at three times this (keep in sync with PurxiyFire#draw)
    float reach = (0.028 + 0.04 * glowStyle) * size;
    // the tongues are lower in the middle of the screen (the bound follows their profile, with some room for the ones
    // that lean in from the side)
    float bound = FireShape.w * min(1.0, frameProfile(uv.x, aspect) + 0.3) + 3.0 * reach + 0.01;
    if (height < 1.0e-4 || uv.y > bound) {
        return vec4(0.0);
    }
    // they lean with you
    vec2 p = vec2(uv.x - FireShape.z * uv.y * 0.6, uv.y);
    float local = height * frameProfile(p.x, aspect);
    // one body of fire along the bottom, the tongues rise from it; their edges wobble a little as the fire rises
    float body = local * 0.3 * (0.9 + 0.2 * noise(vec2(p.x * 4.0 / size, t * 0.7)));
    float round = 0.012 * size;

    float ub, um;
    float db = tongues(1, p, FireLayers.y, int(FireLayers.w), round, ub);
    db = smin(db, p.y - body * 1.15, 0.03 * size);
    float dm = tongues(0, p, FireLayers.x, int(FireLayers.z), round, um);
    dm = smin(dm, p.y - body, 0.03 * size);
    // the edges wobble a little as the fire rises; only near an edge (the heart's too), fading out before it stops
    float near = 1.0 - smoothstep(0.03 * size, 0.045 * size, min(abs(db), abs(dm)));
    if (near > 0.0) {
        float edge = (fbm(vec2(p.x * 10.0, p.y * 7.0 - t * 2.8) / size, 3) - 0.5) * 0.008 * size * near;
        db += edge * (0.4 + clamp(ub, 0.0, 1.0));
        dm += edge * (0.4 + clamp(um, 0.0, 1.0));
    }

    float aa = 1.2 * px / FireScreen.y;
    float flare = FireLife.x;
    vec3 vivid = FireRim.rgb;
    vec4 result = vec4(0.0);

    // the layer behind: taller tongues in the deeper colour, a little softer. Fire gives light, so its layers add a
    // little to what is behind them instead of only covering it (half-covered violet over green would turn grey).
    float cb = smoothstep(aa * 1.5, -aa * 1.5, db);
    float backCover = cb * (1.0 - 0.35 * smoothstep(0.7, 1.1, ub)) * (1.0 - glowStyle);
    vec3 back = calmed(mix(mix(vivid, FireSecond.rgb, 0.25), WHITE, 0.2 * flare));
    result = over(result, vec4(back * backCover * 0.85, backCover * 0.72));

    // the front layer: lilac, lighter inside, the tips blushing towards the accent
    float cm = smoothstep(aa, -aa, dm);
    float inside = clamp(-dm / (0.03 * size), 0.0, 1.0);
    vec3 c = mix(mix(vivid, FireSecond.rgb, 0.5), FireFirst.rgb, smoothstep(0.2, 1.0, inside) * 0.42);
    c = mix(c, mix(FireAccent.rgb, vivid, 0.3), smoothstep(0.55, 1.05, um) * 0.4);
    c = mix(c, FireHeart.rgb, 0.3 * flare);
    float frontCover = cm * (1.0 - 0.3 * smoothstep(0.7, 1.1, um)) * (1.0 - glowStyle);
    result = over(result, vec4(calmed(c) * frontCover * 0.95, frontCover * 0.86));

    // the heart: low in the flames, inset from their edges
    float dc = dm + 0.026 * size * (1.0 - 0.35 * flare) + max(p.y - local * (0.36 + 0.12 * flare), 0.0) * 1.2;
    float cc = smoothstep(aa, -aa, dc);
    vec3 heart = mix(FireFirst.rgb, FireHeart.rgb, smoothstep(0.0, 1.0, clamp(-dc / (0.035 * size), 0.0, 1.0)) * 0.65);
    result = over(result, paint(calmed(heart), cc * (0.5 + 0.3 * flare) * (1.0 - glowStyle)));

    // the light around them; in the Glow style, all there is
    float nearest = max(min(db, dm), 0.0);
    float cover = max(cb, cm) * (1.0 - glowStyle);
    float glow = exp(-nearest / reach) * (1.0 - smoothstep(1.6 * reach, 3.0 * reach, nearest)) * (1.0 - cover)
        * (0.42 + 0.3 * flare + 0.25 * glowStyle);
    glow += glowStyle * 0.6 * smoothstep(0.02 * size, -0.03 * size, min(db, dm));
    vec3 glowColour = calmed(mix(mix(FireSecond.rgb, FireAccent.rgb, 0.5), vivid, 0.4 * glowStyle));
    return over(light(glowColour, glow, 0.12 + 0.3 * glowStyle), result);
}

// ---------------------------------------------------------------------------------------------------------- pixel

// Minecraft's fire, recoloured, in tiles along the bottom edge (every other one mirrored, each on a frame of its own),
// lowered until their tips reach the flames' height.
vec4 pixelFlames(vec2 uv, float aspect) {
    float width = FirePixel.y;
    int tiles = int(FirePixel.z);
    vec4 result = vec4(0.0);
    for (int k = 0; k < tiles; k++) {
        float cx = float(k) * aspect / float(tiles - 1);
        float top = FireShape.x * frameProfile(cx, aspect) + width / 16.0;
        vec2 at = vec2((uv.x - cx) / width + 0.5, (top - uv.y) / width);
        if (at.x < 0.0 || at.x >= 1.0 || at.y < 0.0 || at.y >= 1.0) {
            continue;
        }
        if (k % 2 == 1) {
            at.x = 1.0 - at.x;
        }
        float frame = mod(FirePixel.x + float(k * 7), FireSprite.w);
        vec2 st = vec2(mix(FireSprite.x, FireSprite.y, at.x), (frame + at.y) * FireSprite.z);
        vec4 texel = textureLod(PixelSampler, st, 0.0);
        vec3 c = mix(texel.rgb, FireHeart.rgb, 0.3 * FireLife.x);
        result = over(result, paint(calmed(c), texel.a * 0.92));
    }
    return result;
}

// ---------------------------------------------------------------------------------------------------------- glow

// A soft band of light along the bottom edge that flickers like fire, brighter towards the corners.
vec4 edgeGlow(vec2 uv, float aspect, float t) {
    float side = abs(uv.x / aspect - 0.5) * 2.0;
    float corner = smoothstep(0.35, 1.0, side);
    float glowStyle = FireStyle.z;
    // gone at seven times its base reach (keep in sync with PurxiyFire#draw)
    float base = (0.022 + 0.05 * max(FireLook.w, 0.1)) * (1.0 + 0.8 * glowStyle);
    float reach = base * (0.8 + 0.4 * noise(vec2(uv.x * 3.0, t * 0.8)));
    reach *= mix(0.75, 1.35, corner) * (1.0 + 0.5 * FireLife.x + 0.8 * FireLife.y);
    float g = exp(-uv.y / reach) * (1.0 - smoothstep(5.0 * base, 7.0 * base, uv.y)) * (0.3 + 0.35 * glowStyle) * FireLife.w;
    vec3 c = calmed(mix(FireSecond.rgb, FireAccent.rgb, 0.3 + 0.35 * corner));
    return light(c, g, 0.4);
}

// ---------------------------------------------------------------------------------------------------------- events

// The air above the flames shimmers: the scene behind them, wavering. Its alpha is how much of it replaces the scene.
vec4 haze(vec2 frag, vec2 uv, float t, float px) {
    float strength = FireLook.x;
    if (strength <= 0.001) {
        return vec4(0.0);
    }
    float top = FireLook.y;
    float band = smoothstep(0.0, max(FireShape.x * 0.5, 0.02), uv.y) * (1.0 - smoothstep(top * 0.45, top, uv.y));
    if (band <= 0.001) {
        return vec4(0.0);
    }
    vec2 np = vec2(uv.x * 9.0, uv.y * 7.0 - t * 2.6);
    vec2 wave = vec2(noise(np), noise(np + vec2(31.7, 11.3))) - 0.5;
    vec2 offset = wave * band * strength * 2.6 * px;
    vec3 scene = textureLod(SceneSampler, (frag + offset) / FireScreen.xy, 0.0).rgb;
    return vec4(scene * band, band);
}

// Steam that billows up when the fire is put out, drifting up and thinning as it goes.
vec4 steam(vec2 uv, float t) {
    float s = FireLife.z;
    if (s >= 1.0) {
        return vec4(0.0);
    }
    float h = max(FireLook.w, 0.12);
    float rise = s * (0.2 + 0.6 * h);
    float size = mix(0.3, h, 0.5);
    vec2 p = vec2(uv.x * 2.4, (uv.y - rise) * 2.2 - t * 0.15) / size;
    float n = fbm(p + (fbm(p * 0.8 + 3.1, 3) - 0.5) * 1.1, 4);
    float plume = 1.0 - smoothstep(h * (0.5 + 1.2 * s), h * (1.1 + 1.9 * s), uv.y);
    float life = smoothstep(0.0, 0.06, s) * pow(1.0 - s, 1.4);
    float density = smoothstep(0.34, 0.74, n) * plume * life;
    vec3 c = mix(WHITE, FireFirst.rgb, 0.45);
    return light(c, density * 0.66, 0.8);
}

// A burst of light from the bottom edge as you catch fire: bright and low at first, rising up the screen as it fades.
vec4 ignition(vec2 uv, float aspect) {
    float flash = FireLife.y;
    if (flash <= 0.001) {
        return vec4(0.0);
    }
    // gone at 1.35 times its reach (keep in sync with PurxiyFire#draw)
    float reach = max(FireLook.w, 0.1) * 1.6 + 0.05;
    float rise = reach * (0.18 + 0.82 * (1.0 - flash));
    float side = abs(uv.x / aspect - 0.5) * 2.0;
    float band = exp(-uv.y / (rise * mix(0.8, 1.2, side))) * (1.0 - smoothstep(0.9 * reach, 1.35 * reach, uv.y));
    return light(mix(FireFirst.rgb, WHITE, 0.5), band * flash * 0.6, 0.3);
}

// ---------------------------------------------------------------------------------------------------------- embers

vec4 emberLight(int e) {
    vec4 a = FireEmbers[e * 2];
    vec4 b = FireEmbers[e * 2 + 1];
    float px = FireScreen.z;
    float radius = a.z;
    vec2 q = local;
    // a capsule along its motion; a little square in the Pixel style
    q.x = max(abs(q.x) - fireStreak(length(b.xy)), 0.0);
    bool square = FireStyle.y > 0.5;
    float d = square ? max(abs(q.x), abs(q.y)) : length(q);
    float core = clamp(radius + 0.5 - d, 0.0, 1.0);
    float glow = exp(-d * d / (radius * radius * 5.0 + px * px * 3.0)) * (square ? 0.35 : 1.0);
    vec3 c = calmed(mix(FireRim.rgb, FireHeart.rgb, b.z));
    float bright = a.w * FireShape.y;
    vec4 result = light(mix(c, FireAccent.rgb, 0.3), glow * 0.9 * bright, 0.25);
    return over(result, paint(mix(c, WHITE, 0.35 + 0.3 * b.z), core * bright));
}

// ---------------------------------------------------------------------------------------------------------- main

vec4 overlay(vec2 frag) {
    float px = FireScreen.z;
    vec2 uv = frag / FireScreen.y;
    float aspect = FireScreen.x / FireScreen.y;
    float t = FireScreen.w;

    vec4 result = haze(frag, uv, t, px);
    vec4 fire = edgeGlow(uv, aspect, t);
    if (FireStyle.x + FireStyle.z > 0.001) {
        fire = over(fire, softFlames(uv, aspect, t, px) * (FireStyle.x + FireStyle.z));
    }
    if (FireStyle.y > 0.001) {
        fire = over(fire, pixelFlames(uv, aspect) * FireStyle.y);
    }
    // fading out as the fire dies down
    fire *= smoothstep(0.0, 0.3, FireLife.w);
    result = over(result, fire * FireShape.y);
    result = over(result, steam(uv, t));
    return over(result, ignition(uv, aspect) * FireShape.y);
}

void main() {
    vec4 result = ember >= 0 ? emberLight(ember) : overlay(gl_FragCoord.xy);
    if (result.a <= 0.002 && max(max(result.r, result.g), result.b) <= 0.002) {
        discard;
    }
    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
