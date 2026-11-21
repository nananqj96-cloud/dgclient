#version 330

#moj_import <purxiy:sky.glsl>

// The soft layers of the Purxiy sky, drawn at half resolution: the river of nebula and the curtains of aurora.
// rgb = their light (the nebula already veiled by the horizon's haze), a = the dust between the clouds, which dims the
// stars drawn on top.

in vec3 vRay;

out vec4 fragColor;

// Two axes in the plane of the river's great circle: where along the river a point lies.
const vec3 RIVER_A = vec3(-0.1993, 0.0, 0.9800);
const vec3 RIVER_B = vec3(0.4312, -0.8981, 0.0877);
// The heart of the river, where it is widest and brightest, like the centre of a galaxy.
const vec3 HEART = vec3(0.0792, -0.5074, 0.8581);

// Now and then a cloud lights up from within, like lightning in a far-away storm: a quick flash, then flickers that
// die away. Two channels, each somewhere along the river.
float flashes(vec3 c, float flow) {
    float sum = 0.0;
    for (int k = 0; k < 2; k++) {
        float period = 7.0 + float(k) * 4.3;
        float t = flow + float(k) * 3.7;
        float slot = floor(t / period);
        float age = t - slot * period;
        if (age > 2.4) {
            continue;
        }
        vec3 h = hash3(vec3(slot, float(k) * 5.0, 71.0));
        if (h.z > 0.75) {
            continue;
        }
        float a = h.x * TAU;
        vec3 at = normalize(RIVER_A * cos(a) + RIVER_B * sin(a) + RIVER * (h.y - 0.5) * 0.6);
        float glow = exp(-(1.0 - dot(c, at)) / (0.004 + 0.008 * h.y));
        float flicker = exp(-age * 2.2) * (0.55 + 0.45 * sin(age * (31.0 + 17.0 * h.z) + h.y * 6.0)) + exp(-age * 9.0);
        sum += glow * flicker;
    }
    return sum;
}

vec3 nebula(vec3 c, float life, float breath, float morph, float flow, out float dust) {
    const int octaves = QUALITY == 2 ? 6 : 3 + QUALITY;
    vec3 p = c * 1.7;
    float m = morph * 0.075;
    p += vec3(sin(c.y * 3.0 + m), cos(c.z * 2.7 - m * 0.8), sin(c.x * 3.4 + m * 0.6)) * life * 0.16;

    // domain warp: the clouds fold slowly into themselves (the warp only needs the large shapes)
    vec2 q = fbm2(p * 0.8 + vec3(0.0, m, 1.7), QUALITY == 0 ? 2 : 3);
    vec3 r = p + vec3(q.x, q.y, (q.x + q.y) * 0.5 - 0.5) * (1.2 + 1.3 * life) - 0.6;
    // the clouds, and in the same fetches the dust between them
    vec2 clouds = fbm2(r + vec3(m * 0.6, 0.0, -m * 0.4), octaves);
    float n = clouds.x;

    // Where there is gas at all: a luminous river around a great circle, widest at its heart, and a few islands of
    // cloud far from it. Between them deep, dark space.
    float off = dot(c, RIVER);
    vec2 along = vec2(dot(c, RIVER_A), dot(c, RIVER_B));
    float heart = exp(-(1.0 - dot(c, HEART)) * 5.0);
    float width = (0.20 + 0.16 * heart) * (0.8 + 0.5 * q.x);
    float river = exp(-off * off / (width * width));
    float islands = smoothstep(0.56, 0.72, q.y) * smoothstep(0.1, 0.35, abs(off));
    float mask = max(river * (0.7 + 0.3 * heart), islands * 0.85);

    // thin where there is little of it, only the densest clouds showing; brightest in the cores
    float gas = smoothstep(0.62 - 0.22 * mask, 0.86, n) * mask;
    gas *= gas * (2.0 - gas);

    // deep indigo in the veils, violet, magenta in the dense clouds, a pale rose at the heart; the thinnest veils
    // scatter starlight, cooler and bluer
    float hot = smoothstep(0.45, 0.85, n + (q.y - 0.5) * 0.5);
    vec3 col = mix(ColNebulaA.rgb * 1.6, ColNebulaB.rgb, smoothstep(0.0, 0.35, gas));
    col = mix(col, ColNebulaC.rgb, hot * 0.9);
    col = mix(col, mix(ColStar.rgb, ColNebulaB.rgb, 0.5), (1.0 - smoothstep(0.05, 0.3, gas)) * 0.45);
    col = mix(col, mix(ColNebulaC.rgb, ColSun.rgb, 0.55), heart * smoothstep(0.3, 0.9, gas) * 0.6);

    // currents of light that flow along the river, and slow breathing waves through the clouds
    float current = noise3(vec3(rotate2(along, morph * 0.085) * 3.2, off * 6.0 + 3.1));
    float pulse = (1.0 + life * 0.30 * sin(breath + n * 9.0 - off * 4.0)) * (0.8 + 0.45 * smoothstep(0.3, 0.8, current) * life);
    vec3 light = col * gas * pulse * 1.15;

    if (QUALITY > 0) {
        // filaments: thin bright wisps where a finer field crosses its middle, drifting a little faster than the clouds
        vec2 fine = fbm2(r * 2.7 + vec3(3.1, -m * 1.4, 7.7), QUALITY + 1);
        float filament = pow(max(1.0 - abs(fine.x * 2.0 - 1.0), 0.0), 8.0) * smoothstep(0.3, 0.6, n) * mask;
        light += mix(ColNebulaC.rgb, ColSun.rgb, 0.3) * filament * 0.35 * (0.75 + 0.5 * current);
        // bright knots where stars are being born
        float knots = smoothstep(0.66, 0.9, fine.y) * smoothstep(0.58, 0.78, n) * mask;
        light += mix(ColNebulaC.rgb, vec3(1.0), 0.4) * knots * knots * 0.5;
    }

    // the unresolved light of countless stars along the river, strongest at its heart
    light += mix(ColNebulaB.rgb, ColStar.rgb, 0.45) * exp(-off * off / 0.006) * (0.035 + 0.1 * heart);

    // Dust: blotches in the clouds, and on high quality dark lanes along the river's heart, like a great rift.
    dust = QUALITY > 0 ? smoothstep(0.56, 0.72, clouds.y) * river : 0.0;
    if (QUALITY > 1) {
        vec3 lp = vec3(along * 3.4, off * 7.0) + vec3(q, q.x) * 0.9;
        float lane = smoothstep(0.47, 0.68, fbm2(lp + vec3(0.0, 0.0, m * 0.2), 3).x) * exp(-off * off / 0.012);
        dust = max(dust, lane * 0.9);
    }

    // now and then the clouds light up from within
    float flash = flashes(c, flow) * life;
    light *= 1.0 + flash * 3.0;
    light += mix(ColNebulaC.rgb, vec3(1.0), 0.5) * flash * gas * 0.25;
    return light * (1.0 - dust * 0.8);
}

// Curtains of light low around the sky: each sheet hangs from a lower edge that meanders with the azimuth (slow bends,
// smaller folds and waves that run along it), is brightest just above it and fades upwards in fine vertical rays. Each
// sheet only covers part of the sky, and wanders. Now and then the aurora surges: brighter, taller and faster.
vec3 aurora(vec3 d, float flow, float life, float surge) {
    if (d.y <= 0.0) {
        return vec3(0.0);
    }
    vec2 around = normalize(d.xz + vec2(1.0e-5));
    float az = atan(around.y, around.x);
    float pace = 1.0 + surge * 1.5;
    vec3 sum = vec3(0.0);
    const int sheets = QUALITY == 0 ? 1 : 2;
    for (int i = 0; i < sheets; i++) {
        float fi = float(i);
        // A surge changes height and brightness, never the accumulated phase of the moving curtains.
        float t = flow * (0.042 + 0.018 * fi) + fi * 7.3;
        vec2 sheetAround = rotate2(around, flow * (0.009 + fi * 0.004));
        // where along the sky this sheet hangs at all
        float presence = smoothstep(0.42 - 0.12 * surge, 0.64, noise3(vec3(around * 0.9, t * 0.5 + 40.0 + fi * 5.0)));
        if (presence <= 0.0) {
            continue;
        }
        vec2 bends = noise3x2(vec3(sheetAround * (1.3 + 0.5 * fi), t + fi * 3.1));
        float folds = noise3(vec3(sheetAround * 4.2, t * 2.0 + fi * 5.7 + 20.0));
        // fine rays that drift along the curtain; each starts a little higher or lower, so the lower edge is ragged
        vec2 rays = noise3x2(vec3(sheetAround * 46.0, t * 5.0 + fi * 3.1));
        float edge = 0.10 + 0.07 * fi - 0.03 * surge + (bends.x - 0.5) * 0.12 + (folds - 0.5) * (0.03 + 0.03 * life)
            + sin(az * 9.0 - flow * 0.48 + fi * 2.0) * 0.014 * life * pace + (rays.y - 0.5) * 0.018;
        float above = d.y - edge;
        if (above < -0.04) {
            continue;
        }
        float fade = 7.0 - fi * 1.5 - surge * 2.5;
        float profile = smoothstep(-0.025, 0.008, above) * (exp(-max(above, 0.0) * fade) + 0.18 * exp(-max(above, 0.0) * 2.2));
        float ray = 0.35 + 0.65 * rays.x * rays.x;
        float patch = 0.55 + 0.45 * smoothstep(0.3, 0.8, bends.y);
        // pink along the lower edge, violet higher up; the colour also wanders along the curtain
        vec3 low = mix(ColAuroraA.rgb, mix(ColAuroraA.rgb, ColAuroraB.rgb, 0.5), smoothstep(0.35, 0.75, rays.y) * 0.7);
        vec3 col = mix(low, ColAuroraB.rgb, smoothstep(0.0, 0.16, above));
        sum += col * profile * ray * patch * presence;
    }
    return sum * 0.85 * (1.0 + surge * 0.9);
}

void main() {
    vec3 d = normalize(vRay);
    float life = SkyAmount.w;
    float night = 1.0 - SkyState.x;

    vec3 light = vec3(0.0);
    float dust = 0.0;
    float nebulaVis = SkyAmount.x * mix(0.32, 1.0, night) * smoothstep(-0.04, 0.10, d.y);
    if (nebulaVis > 0.002) {
        // deep space is seen through the singularity's lens
        vec3 c = mat3(SkyCelestial) * lensed(d);
        light += nebula(c, life, SkyTime.z, SkyTime.y, SkyTime.x, dust) * nebulaVis * (1.0 - skyHaze(d));
    }
    float auroraVis = SkyAmount.z * mix(0.12, 1.0, night * night);
    if (auroraVis > 0.002) {
        light += aurora(d, SkyTime.x, life, SkyLife.y) * auroraVis;
    }
    // this is stored in 8 bits: dither it, or faint clouds would band
    light += (hash12(gl_FragCoord.xy) - 0.5) * (1.0 / 255.0);
    fragColor = vec4(max(light, 0.0), dust);
}
