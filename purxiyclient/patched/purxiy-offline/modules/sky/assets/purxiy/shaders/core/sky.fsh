#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:sky.glsl>
#moj_import <purxiy:sky_styles.glsl>
#moj_import <purxiy:sky_weather.glsl>

// The Purxiy sky (see dev.purxiy.sky.PurxiySky): a living sky from another dimension, painted per pixel from the
// view direction, so it is sharp at any resolution and field of view.
//
//   dome      gradient from the zenith to the horizon, which is exactly the colour of the world's fog; dawn and dusk
//   space     the river of nebula and the curtains of aurora (drawn at half resolution by sky_space.fsh), three layers
//             of stars, clusters and galaxies, all seen through the singularity's lens; meteors, showers and comets
//   bodies    the sun and the moon (the planet and the singularity are drawn on top by sky_body.fsh)
//   air       the glowing horizon ring, lightning

uniform sampler2D SpaceSampler; // the soft layers: rgb = light, a = dust

in vec3 vRay;

out vec4 fragColor;

const float SUN_RADIUS = 0.040;
const float MOON_RADIUS = 0.034;

// ---------------------------------------------------------------------------------------------------------- dome

vec3 dome(vec3 d, vec3 horizon) {
    float up = max(d.y, 0.0);
    vec3 sky = mix(ColMid.rgb, ColZenith.rgb, smoothstep(0.08, 1.0, up));
    sky = mix(sky, horizon, exp(-up * 6.5));
    if (d.y < 0.0) {
        // below the horizon: a quiet abyss
        vec3 abyss = ColZenith.rgb * 0.75 + ColMid.rgb * 0.15;
        sky = mix(horizon, abyss, 1.0 - exp(d.y * 5.0));
    }
    return sky;
}

// Dawn and dusk: a glow around the sun, and opposite it a pink belt above the darker shadow of the world rising (the
// belt of Venus). It leaves the horizon itself alone, so the fog still matches.
vec3 twilight(vec3 d, vec3 col) {
    float tw = SkyState.y;
    if (tw <= 0.002) {
        return col;
    }
    vec2 toSun = normalize(SkySun.xz + vec2(1.0e-5));
    float facing = dot(normalize(d.xz + vec2(1.0e-5)), toSun);
    float up = max(d.y, 0.0);
    float toward = max(facing, 0.0);
    col += mix(ColGlow.rgb, ColSun.rgb, 0.35) * tw * toward * toward * toward * exp(-up * 4.0) * 0.55;
    float away = max(-facing, 0.0);
    away *= away * tw;
    float belt = exp(-(up - 0.10) * (up - 0.10) / 0.0036);
    float shadow = smoothstep(0.004, 0.02, up) * exp(-up / 0.05);
    return (col + ColGlow.rgb * belt * away * 0.22) * (1.0 - shadow * away * 0.25);
}

// ---------------------------------------------------------------------------------------------------------- stars

// Face of the cube map a direction falls on, and its coordinates on that face (-1..1).
vec2 cubeFace(vec3 d, out int face) {
    vec3 a = abs(d);
    if (a.x >= a.y && a.x >= a.z) {
        face = d.x > 0.0 ? 0 : 1;
        return d.zy / a.x;
    }
    if (a.y >= a.z) {
        face = d.y > 0.0 ? 2 : 3;
        return d.xz / a.y;
    }
    face = d.z > 0.0 ? 4 : 5;
    return d.xy / a.z;
}

vec3 cubeDir(int face, vec2 uv) {
    vec3 d;
    if (face == 0) {
        d = vec3(1.0, uv.y, uv.x);
    } else if (face == 1) {
        d = vec3(-1.0, uv.y, uv.x);
    } else if (face == 2) {
        d = vec3(uv.x, 1.0, uv.y);
    } else if (face == 3) {
        d = vec3(uv.x, -1.0, uv.y);
    } else if (face == 4) {
        d = vec3(uv.x, uv.y, 1.0);
    } else {
        d = vec3(uv.x, uv.y, -1.0);
    }
    return normalize(d);
}

// Mostly white and lavender, some blue-white and pink, and now and then an old gold giant.
vec3 starTint(float h) {
    if (h < 0.45) {
        return vec3(1.0);
    }
    if (h < 0.65) {
        return vec3(0.84, 0.78, 1.0);
    }
    if (h < 0.80) {
        return vec3(0.76, 0.84, 1.0);
    }
    if (h < 0.93) {
        return vec3(1.0, 0.78, 0.92);
    }
    return vec3(1.0, 0.88, 0.72);
}

// Normal cumulative distribution (logistic approximation, plenty for pixels).
float cdf(float x) {
    return 1.0 / (1.0 + exp(-1.702 * x));
}

// The light of a star of `sigma` pixels, integrated over this pixel's square and scaled to the total of a unit-peak
// Gaussian: a star moving across the screen keeps its brightness instead of flickering from pixel to pixel.
float splat(vec2 o, float sigma) {
    vec2 hi = (o + 0.5) / sigma;
    vec2 lo = (o - 0.5) / sigma;
    return (cdf(hi.x) - cdf(lo.x)) * (cdf(hi.y) - cdf(lo.y)) * TAU * sigma * sigma;
}

// The screen: sx and sy turn a small step of the view direction into pixels along x and y; px is a pixel's angle.
struct Screen {
    vec3 sx;
    vec3 sy;
    float px;
};

// One layer of stars: at most one per cell of a cube-map grid, each an anti-aliased point measured in pixels, so stars
// stay crisp and round at every resolution. Stars keep a quarter cell away from the cell's edges, so only the cell the
// pixel falls in has to be looked at.
vec3 starLayer(vec3 c, int face, vec2 uv, float scale, Screen screen, float cells, float density, float gain, float core, float halo,
    float reach, float seed, float flow, float life, bool flare) {
    vec2 g = uv * cells;
    vec2 cell = min(floor(g), cells - 1.0);
    vec3 h = hash3(vec3(cell, float(face) + seed * 8.0));
    if (h.z > density) {
        return vec3(0.0);
    }
    // cheap test first, on the face: most pixels are too far from their cell's star to see its light
    vec2 centre = cell + 0.25 + 0.5 * h.xy;
    if (length(g - centre) * scale / cells > reach * screen.px) {
        return vec3(0.0);
    }
    vec3 s = cubeDir(face, centre / cells * 2.0 - 1.0);
    vec2 o = vec2(dot(s - c, screen.sx), dot(s - c, screen.sy));
    float r = length(o);
    if (r > reach) {
        return vec3(0.0);
    }
    vec3 k = hash3(vec3(cell.yx + 311.0, float(face) + seed * 8.0 + 4.0));
    float bright = gain * (0.18 + 0.82 * k.x * k.x * k.x);
    float twinkle = 1.0 + life * 0.6 * sin(flow * (1.3 + 2.4 * k.y) + fract(k.z * 7.31) * TAU);
    float shape = splat(o, core) + halo * 0.1 * exp(-r / (core * 2.5));
    if (flare) {
        // a soft four-pointed sparkle on the brightest stars
        vec3 t1 = normalize(cross(s, vec3(0.28, 0.91, 0.30)));
        vec2 f = abs(vec2(dot(s - c, t1), dot(s - c, cross(s, t1)))) / screen.px;
        shape += 0.3 * (exp(-f.x * f.x * 0.8 - f.y / 7.0) + exp(-f.y * f.y * 0.8 - f.x / 7.0));
    }
    return starTint(k.z) * bright * twinkle * shape;
}

// A young open cluster: a knot of bright blue-white stars in a veil of blue haze.
vec3 cluster(vec3 c, vec3 centre, float radius, float seed, Screen screen, float flow, float life) {
    float cd = dot(c, centre);
    if (cd < cos(radius * 2.0)) {
        return vec3(0.0);
    }
    vec3 right = normalize(cross(centre, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, centre);
    vec2 q = vec2(dot(c, right), dot(c, up)) / cd;
    vec3 light = mix(ColStar.rgb, vec3(0.62, 0.72, 1.0), 0.6) * exp(-dot(q, q) / (radius * radius) * 1.6) * 0.07;
    float cell = radius / 3.5;
    vec2 id = floor(q / cell);
    vec3 h = hash3(vec3(id, seed));
    if (h.z < 0.85 * exp(-dot(id + 0.5, id + 0.5) / 14.0)) {
        vec2 sq = (id + 0.25 + 0.5 * h.xy) * cell - q;
        vec3 off = right * sq.x + up * sq.y;
        vec2 o = vec2(dot(off, screen.sx), dot(off, screen.sy));
        float bright = 0.6 + 1.4 * h.x * h.y;
        float twinkle = 1.0 + life * 0.5 * sin(flow * (1.7 + 2.0 * h.y) + h.x * TAU);
        light += vec3(0.8, 0.87, 1.0) * bright * twinkle * (splat(o, 0.85) + 0.1 * exp(-length(o) / 2.2));
    }
    return light;
}

// A distant galaxy: a disc of light seen at a slant, with spiral arms that turn very slowly around a bright core.
vec3 galaxy(vec3 c, vec3 centre, float size, float slant, float angle, vec3 tint, float flow) {
    float cd = dot(c, centre);
    if (cd < cos(size * 1.8)) {
        return vec3(0.0);
    }
    vec3 right = normalize(cross(centre, vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, centre);
    vec2 q = rotate2(vec2(dot(c, right), dot(c, up)) / (cd * size), angle);
    q.y /= slant;
    float r = length(q);
    float arms = 0.5 + 0.5 * cos(2.0 * atan(q.y, q.x) - log(r + 0.02) * 4.0 - flow * 0.004);
    float disc = exp(-r * 3.5) * mix(0.25, 1.0, arms * smoothstep(0.02, 0.25, r));
    float core = exp(-r * r * 50.0);
    return (tint * disc * 0.5 + mix(tint, vec3(1.0), 0.6) * core * 0.7) * smoothstep(1.8, 1.2, r);
}

vec3 starField(vec3 c, Screen screen, float flow, float life) {
    int face;
    vec2 t = cubeFace(c, face);
    vec2 uv = t * 0.5 + 0.5;
    // angle per unit of face coordinate is at least 1 / (1 + |t|^2) (gnomonic projection), times 2 per cell unit, with
    // some margin for the star sitting a little further out
    float scale = 1.8 / (1.0 + dot(t, t));
    // the faintest stars crowd along the river of the nebula, like a milky way
    float off = dot(c, RIVER);
    float crowd = 0.35 + 0.6 * exp(-off * off / 0.06);
    vec3 stars = starLayer(c, face, uv, scale, screen, 76.0, crowd, 0.28, 0.75, 0.0, 4.5, 1.0, flow, life, false);
    stars += starLayer(c, face, uv, scale, screen, 30.0, 0.55, 0.8, 0.85, 1.0, 8.0, 2.0, flow, life, false);
    if (QUALITY > 1) {
        // a haze of countless faint stars along the river, like the milky way
        float band = exp(-off * off / 0.02);
        if (band > 0.08) {
            stars += starLayer(c, face, uv, scale, screen, 150.0, band * 0.85, 0.13, 0.7, 0.0, 3.5, 4.0, flow, life, false);
        }
    }
    if (QUALITY > 0) {
        stars += starLayer(c, face, uv, scale, screen, 10.0, 0.32, 1.5, 1.0, 2.2, QUALITY > 1 ? 30.0 : 10.0, 3.0, flow, life, QUALITY > 1);
        stars += cluster(c, vec3(0.30, 0.80, -0.52), 0.045, 5.0, screen, flow, life);
        stars += cluster(c, vec3(-0.62, 0.36, 0.70), 0.035, 9.0, screen, flow, life);
        stars += galaxy(c, vec3(0.735, 0.608, -0.300), 0.035, 0.55, 0.6, mix(ColNebulaC.rgb, ColStar.rgb, 0.5), flow);
        stars += galaxy(c, vec3(-0.705, 0.353, 0.615), 0.022, 0.18, -0.4, mix(ColNebulaB.rgb, ColStar.rgb, 0.5), flow);
        stars += galaxy(c, vec3(0.152, -0.658, 0.737), 0.028, 0.8, 1.9, mix(ColGlow.rgb, ColStar.rgb, 0.4), flow);
    }
    return stars * ColStar.rgb;
}

// ---------------------------------------------------------------------------------------------------------- meteors

// One streak of light: starting at `start`, flying along `dir` for `len` radians, `p` of the way through its life.
vec3 streak(vec3 d, float px, vec3 start, vec3 dir, float len, float p) {
    float head = len * (1.0 - (1.0 - p) * (1.0 - p));
    vec3 a = normalize(start + dir * max(head - 0.10 - 0.10 * p, 0.0));
    vec3 b = normalize(start + dir * head);
    vec3 ab = b - a;
    float along = clamp(dot(d - a, ab) / max(dot(ab, ab), 1.0e-8), 0.0, 1.0);
    float dist = length(d - (a + ab * along)) / px;
    float fade = smoothstep(0.0, 0.12, p) * (1.0 - smoothstep(0.65, 1.0, p));
    float light = exp(-dist * dist / 1.4) * along * along + exp(-dist / 3.5) * 0.22 * along;
    return mix(ColNebulaC.rgb, ColSun.rgb, along) * light * fade * 1.6;
}

// Now and then a streak of light: three independent channels, each with a chance of a meteor every few seconds. During
// a shower, five more channels send meteors flying away from its radiant.
vec3 shootingStars(vec3 d, float px, float time) {
    vec3 sum = vec3(0.0);
    for (int k = 0; k < 3; k++) {
        float period = 7.0 + float(k) * 3.3;
        float t = time + float(k) * 13.1;
        float slot = floor(t / period);
        float age = t - slot * period;
        if (age > 0.9) {
            continue;
        }
        vec3 h = hash3(vec3(slot, float(k) * 7.0, 91.0));
        if (h.z > 0.55) {
            continue;
        }
        vec3 h2 = hash3(vec3(slot, float(k) * 7.0 + 3.0, 57.0));
        float az = h.x * TAU;
        float el = mix(0.45, 1.15, h.y);
        vec3 start = vec3(cos(az) * cos(el), sin(el), sin(az) * cos(el));
        vec3 side = normalize(cross(start, vec3(0.0, 1.0, 0.0)));
        vec3 rise = cross(side, start);
        vec3 dir = normalize(side * (h2.x * 2.0 - 1.0) - rise * (0.6 + 0.4 * h2.y));
        sum += streak(d, px, start, dir, mix(0.25, 0.45, h2.z), age / 0.9);
    }
    if (SkyShower.w > 0.002) {
        vec3 radiant = SkyShower.xyz;
        vec3 side = normalize(cross(radiant, vec3(0.0, 1.0, 0.0)));
        vec3 rise = cross(side, radiant);
        for (int k = 0; k < 5; k++) {
            float period = 1.3 + float(k) * 0.37;
            float t = time + float(k) * 0.71;
            float slot = floor(t / period);
            float age = t - slot * period;
            if (age > 0.8) {
                continue;
            }
            vec3 h = hash3(vec3(slot, float(k) + 20.0, 13.0));
            if (h.z > 0.8) {
                continue;
            }
            float a = h.x * TAU;
            float dist = mix(0.08, 0.5, h.y);
            vec3 outward = side * cos(a) + rise * sin(a);
            vec3 start = radiant * cos(dist) + outward * sin(dist);
            vec3 dir = outward * cos(dist) - radiant * sin(dist);
            sum += streak(d, px, start, dir, mix(0.12, 0.3, h.x), age / 0.8) * SkyShower.w;
        }
    }
    return sum;
}

// A comet: a sharp nucleus in a cyan coma, a broad curved dust tail and a straight blue ion tail, both streaming away
// from the sun, rippling as they flow and fading out towards their ends.
vec3 comet(vec3 d, float px, float flow) {
    vec3 head = SkyComet.xyz;
    float len = SkyCometTail.w;
    float ch = dot(d, head);
    if (ch < cos(len * 1.1 + 0.05)) {
        return vec3(0.0);
    }
    vec3 tail = SkyCometTail.xyz;
    vec3 across = cross(head, tail);
    vec3 v = d / ch - head;
    float x = dot(v, tail);
    float y = dot(v, across);
    float r = length(vec2(x, y));
    vec3 coma = mix(ColStar.rgb, vec3(0.62, 0.95, 1.0), 0.55);
    vec3 col = vec3(1.0) * exp(-r * r / (px * px * 2.5)) * 1.2 + coma * (exp(-r / 0.004) * 0.5 + exp(-r / 0.015) * 0.12);
    if (x > 0.0) {
        float t = x / len;
        float end = 1.0 - smoothstep(0.35, 1.0, t);
        // the dust tail curves away from the path and widens
        float bend = 0.2 * x * t;
        float w = 0.003 + 0.035 * t;
        float dy = (y - bend) / w;
        float dust = exp(-dy * dy) * exp(-t * 1.5) * (0.65 + 0.35 * noise2(vec2(x * 50.0 - flow * 0.6, dy * 2.5)));
        // the ion tail is straight, narrow and blue, with knots drifting along it
        float wi = 0.0015 + 0.006 * t;
        float dyi = y / wi;
        float ion = exp(-dyi * dyi) * exp(-t * 1.1) * (0.5 + 0.5 * noise2(vec2(x * 22.0 - flow * 1.4, dyi * 1.5 + 3.0)));
        col += (mix(ColGlow.rgb, ColSun.rgb, 0.55) * dust * 0.5 + mix(ColNebulaB.rgb, vec3(0.55, 0.8, 1.0), 0.6) * ion * 0.5) * end;
    }
    return col;
}

// ---------------------------------------------------------------------------------------------------------- sun, moon

vec3 sunAndMoon(vec3 d, float px, float flow, vec3 col, float vis) {
    float up = smoothstep(-0.01, 0.02, d.y);

    // sun: a white-hot star, darker and warmer at its edge, in a corona of slowly turning streamers; warmer as it sinks
    vec3 S = SkySun.xyz;
    float as = length(d - S);
    if (as < 1.0) {
        float sinking = smoothstep(0.28, 0.0, S.y);
        vec3 around = normalize(d - S * dot(d, S) + vec3(1.0e-6));
        float streamers = 0.7 + 0.6 * noise3(around * 3.0 + vec3(0.0, 0.0, flow * 0.02));
        vec3 corona = ColGlow.rgb * (exp(-as * 70.0) * 0.7 + exp(-as * 16.0) * 0.28 * streamers + exp(-as * 4.5) * 0.08);
        col += corona * vis * smoothstep(-0.15, 0.05, S.y);
        float disk = clamp((SUN_RADIUS - as) / px + 0.5, 0.0, 1.0);
        float mu = sqrt(max(1.0 - as * as / (SUN_RADIUS * SUN_RADIUS), 0.0));
        vec3 sun = mix(ColGlow.rgb * 1.2, ColSun.rgb, 0.45 + 0.55 * mu);
        col = mix(col, mix(sun, ColGlow.rgb * 1.3, sinking * 0.55), disk * vis * up);
    }

    // moon: a pale world with soft craters that shows its phase
    vec3 M = SkyMoon.xyz;
    float am = length(d - M);
    if (am > 0.9) {
        return col;
    }
    float cm = dot(d, M);
    float phaseLight = 0.5 + 0.5 * cos(SkyMoon.w);
    if (cm > 0.995) {
        vec3 right = normalize(cross(M, vec3(0.0, 0.0, 1.0)));
        vec3 v = d / cm - M;
        vec2 q = vec2(dot(v, right), dot(v, cross(right, M))) / MOON_RADIUS;
        float r2 = dot(q, q);
        float cover = clamp((1.0 - sqrt(r2)) / (px / MOON_RADIUS) + 0.5, 0.0, 1.0);
        if (cover > 0.0) {
            vec3 n = vec3(q, sqrt(max(1.0 - r2, 0.0)));
            vec3 L = vec3(sin(SkyMoon.w), 0.0, cos(SkyMoon.w));
            float lit = smoothstep(-0.05, 0.12, dot(n, L));
            float craters = noise3(n * 2.8 + 4.0) * 0.6 + noise3(n * 7.5 + 9.0) * 0.4;
            vec3 surface = ColMoon.rgb * (0.7 + 0.38 * craters) * (0.72 + 0.28 * n.z);
            vec3 moon = surface * (0.03 + 0.97 * lit) + ColMoon.rgb * 0.04 * (1.0 - lit);
            col = mix(col, moon, cover * vis * up);
        }
    }
    col += ColMoon.rgb * (exp(-am * 24.0) * 0.22 + exp(-am * 6.0) * 0.05) * phaseLight * vis * smoothstep(-0.15, 0.05, M.y);
    return col;
}

// ---------------------------------------------------------------------------------------------------------- air

// A thin ring of light around the horizon, like the ring of the Purxiy mark, with light travelling slowly along it.
vec3 horizonRing(vec3 d, float flow, float breath, float life) {
    float y = d.y - 0.004;
    float line = exp(-(y * y) / (0.003 * 0.003));
    float bloom = exp(-abs(y) * 38.0) * 0.26 + exp(-max(d.y, 0.0) * 8.0) * 0.07;
    vec2 around = normalize(d.xz + vec2(1.0e-5));
    float travel = smoothstep(0.25, 0.85, noise3(vec3(around * 2.6, flow * 0.05)));
    float pulse = 1.0 + life * 0.35 * sin(breath);
    return ColGlow.rgb * (line * 0.34 + bloom) * (0.3 + 1.1 * travel) * pulse;
}

// ---------------------------------------------------------------------------------------------------------- main

vec3 galaxySky(vec3 d, float px, vec3 horizon) {
    float flow = SkyTime.x;
    float life = SkyAmount.w;
    float night = 1.0 - SkyState.x;
    float above = smoothstep(-0.04, 0.10, d.y);
    float haze = skyHaze(d);

    // The horizon is the colour of the world's fog, so the land melts into the sky.
    vec3 col = twilight(d, mix(dome(d, horizon), horizon, haze));

    // deep space: the soft layers, then the stars (dimmed by the dust between the clouds, bent by the singularity)
    vec4 space = SkyMisc.z > 0.5 ? textureLod(SpaceSampler, gl_FragCoord.xy * SkyMisc.xy, 0.0) : vec4(0.0);
    col += space.rgb;
    vec3 c = mat3(SkyCelestial) * lensed(d);
    vec3 cx = dFdx(c);
    vec3 cy = dFdy(c);
    Screen screen = Screen(cx / max(dot(cx, cx), 1.0e-12), cy / max(dot(cy, cy), 1.0e-12), px);
    float starVis = SkyAmount.y * mix(0.12, 1.0, night * night) * above * (1.0 - haze);
    if (starVis > 0.002) {
        vec3 stars = starField(c, screen, flow, life) * (1.0 - space.a * 0.65);
        if (SkyTime.w > 0.002) {
            stars += shootingStars(d, px, flow) * SkyTime.w;
        }
        col += stars * starVis;
    }
    if (SkyComet.w > 0.002) {
        col += comet(d, px, flow) * SkyComet.w * mix(0.25, 1.0, night) * above * (1.0 - haze);
    }

    float bodiesVis = SkySun.w;
    if (bodiesVis > 0.002) {
        col = sunAndMoon(d, px, flow, col, bodiesVis);
    }

    // the glowing horizon (nothing left of it half way up)
    if (d.y < 0.5) {
        col += horizonRing(d, flow, SkyTime.z, life) * mix(1.0, 0.45, SkyState.x);
    }

    // lightning lights up the clouds of space
    col += (ColGlow.rgb * 0.25 + ColNebulaB.rgb * 0.35) * SkyState.w * above;
    return softClip(col);
}

void main() {
    vec3 d = normalize(vRay);
    float px = max(length(dFdx(d)), length(dFdy(d))); // angular size of a pixel
    vec3 horizon = FogColor.a > 0.0 ? FogColor.rgb : ColHorizon.rgb;
    vec3 col = vec3(0.0);
    float galaxyWeight = max(0.0, 1.0 - dot(SkyStyles, vec4(1.0)) - dot(SkyWeatherStyles.xyz, vec3(1.0)));

    // These branches are uniform over the entire draw. A settled preset evaluates only its own material, and avoids
    // all Galaxy stars, lensing, celestial bodies, atmosphere and the half-resolution sampler. During a transition,
    // complete images blend together, so highlights and silhouettes fade with the rest of their sky.
    if (galaxyWeight > 0.002) {
        col += galaxySky(d, px, horizon) * galaxyWeight;
    }
    if (SkyStyles.x > 0.002) {
        col += aetherSky(d, horizon) * SkyStyles.x;
    }
    if (SkyStyles.y > 0.002) {
        col += nocturneSky(d, horizon) * SkyStyles.y;
    }
    if (SkyStyles.z > 0.002) {
        col += amethystSky(d, horizon) * SkyStyles.z;
    }
    if (SkyStyles.w > 0.002) {
        col += opalineSky(d, horizon) * SkyStyles.w;
    }
    if (SkyWeatherStyles.x > 0.002) {
        col += rainSky(d, horizon) * SkyWeatherStyles.x;
    }
    if (SkyWeatherStyles.y > 0.002) {
        col += snowSky(d, horizon) * SkyWeatherStyles.y;
    }
    if (SkyWeatherStyles.z > 0.002) {
        col += fireSky(d, horizon) * SkyWeatherStyles.z;
    }

    // under water the sky fades into the water, as vanilla's does
    if (SkyLife.x > 0.0) {
        float sphere = d.y > 0.0 ? 16.0 / d.y : 1.0e6;
        col = mix(col, FogColor.rgb, linear_fog_value(sphere, 0.0, FogSkyEnd) * SkyLife.x);
    }

    // dither away 8-bit banding in the dark gradients
    col += (hash12(gl_FragCoord.xy) - 0.5) * (1.5 / 255.0);
    fragColor = vec4(col, SkyMisc.w);
}
