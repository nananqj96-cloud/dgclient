#version 330

// Haze's colour picker: a green valley on a clear afternoon, ridge behind ridge of hills and woods out to the far
// distance, the sun low over the hills on the right. The haze lies over it as it does in the world: close by the land keeps
// its own colours, the further away the more it takes on the haze's, thickest low in the valleys, with soft pockets of its
// second colour drifting by and a glow towards the sun. Live in your Strength, Height and Sun glow, and in the palette
// chosen (or the one under the pointer). Drawn into an image of its own and shown in the picker.

layout(std140) uniform PurxiyHazePreview {
    // x, y = size (px), z = seconds, w = how far the drift has gone
    vec4 PreviewSize;
    // rgb = the haze close by and low, w = extinction low over the ground (per block, as in the world)
    vec4 PreviewLow;
    // rgb = the haze far away and higher up, w = height of the haze (blocks)
    vec4 PreviewHigh;
    // rgb = the pockets drifting through it, w = how much they show
    vec4 PreviewPocket;
    // rgb = its glow towards the sun (strength included), w = how far around you the air is clear (blocks)
    vec4 PreviewSun;
};

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x), mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0, amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += amp * noise(p);
        p = p * 2.03 + 17.1;
        amp *= 0.5;
    }
    return sum;
}

// trees along a ridge: round crowns and a few firs, in cells of 1 / n of the width
float trees(float x, float n, float height, float seed) {
    float cell = floor(x * n);
    float best = 0.0;
    for (int k = -1; k <= 1; k++) {
        float c = cell + float(k);
        float r = hash(vec2(c, seed));
        if (r < 0.35) {
            continue;
        }
        float centre = (c + 0.25 + 0.5 * hash(vec2(c, seed + 3.1))) / n;
        float h = height * (0.55 + 0.45 * hash(vec2(c, seed + 7.7)));
        float d = abs(x - centre) * n;
        float crown = r > 0.84 ? max(0.0, 1.0 - d * 2.2) * 1.3 : sqrt(max(0.0, 1.0 - d * d * 3.2));
        best = max(best, h * crown);
    }
    return best;
}

const int RIDGES = 4;
// how far away each ridge is (blocks), the farthest first
const float DISTANCE[RIDGES] = float[](600.0, 300.0, 150.0, 40.0);
// the meadow in front: where it ends (the picture's height) and how far away it is there and at the bottom
const float MEADOW = 0.14, MEADOW_FAR = 34.0, MEADOW_NEAR = 5.0;

// the foot of ridge k in the picture's height (0 bottom, 1 top)
float foot(int k) {
    return 0.5 - 0.095 * float(k);
}

// the top of ridge k at x (u: across the picture, 0..1): soft hills, the nearer ones rising at the sides so the valley opens
// in the middle, and woods along them (the nearer, the bigger the trees)
float ridge(float x, float u, int k) {
    float fk = float(k);
    float valley = (1.0 - 4.0 * u * (1.0 - u)) * fk * fk * 0.06;
    float hills = fbm(vec2(x * (0.8 + 0.45 * fk) + fk * 7.3, fk * 1.7)) * (0.05 + 0.04 * fk) + valley;
    float forest = trees(x + fk * 0.31, 30.0 - 6.0 * fk, 0.014 + 0.013 * fk, fk + 3.0);
    return foot(k) + 0.015 + hills + forest;
}

// The mean of exp(-k * max(h, 0)) while h goes evenly from a to b (as include/haze.glsl).
float band(float a, float b, float k) {
    float lo = min(a, b), hi = max(a, b);
    float span = hi - lo;
    if (span < 1.0e-3) {
        return exp(-k * max(lo, 0.0));
    }
    float below = clamp(-lo, 0.0, span);
    float above = hi > 0.0 ? (exp(-k * max(lo, 0.0)) - exp(-k * hi)) / k : 0.0;
    return (below + above) / span;
}

void main() {
    vec2 size = PreviewSize.xy;
    vec2 uv = gl_FragCoord.xy / size;
    float aspect = size.x / size.y;
    float x = uv.x * aspect;
    float y = uv.y;
    float drift = PreviewSize.w;
    float sigma = PreviewLow.w;
    float k = 1.0 / max(PreviewHigh.w, 1.0);
    // the camera stands a few blocks above the valley floor; one unit of the picture's height spans about this many blocks
    // per block of distance
    const float EYE = 6.0, SPAN = 0.9;

    // the sky: clear blue high up, pale at the horizon, the sun low on the right
    vec2 sunAt = vec2(0.76 * aspect, 0.64);
    float toSun = length(vec2(x, y) - sunAt);
    vec3 colour = mix(vec3(0.80, 0.87, 0.96), vec3(0.40, 0.58, 0.89), smoothstep(0.42, 1.05, y));
    colour += vec3(1.0, 0.95, 0.86) * (smoothstep(0.032, 0.022, toSun) * 0.55 + exp(-toSun * 8.0) * 0.16);
    float distance = 900.0;
    // how high above the valley floor the view goes at the far end, and whether it is land
    float rise = (y - 0.47) * SPAN * distance;

    float tops[RIDGES];
    for (int r = 0; r < RIDGES; r++) {
        tops[r] = ridge(x, uv.x, r);
    }
    int layer = -1;
    for (int r = 0; r < RIDGES; r++) {
        if (y <= tops[r]) {
            layer = r;
        }
    }
    if (y < MEADOW) {
        // the meadow in front, a fresh green, lighter where the sun reaches
        float m = y / MEADOW;
        distance = mix(MEADOW_NEAR, MEADOW_FAR, m * m);
        rise = 0.0;
        vec3 grass = mix(vec3(0.30, 0.52, 0.18), vec3(0.38, 0.58, 0.22), noise(vec2(x * 22.0, y * 60.0)));
        colour = grass * (0.92 + 0.12 * m) + vec3(0.10, 0.08, 0.03) * exp(-toSun * 1.8);
        layer = RIDGES;
    } else if (layer >= 0) {
        float fk = float(layer) / float(RIDGES - 1);
        distance = DISTANCE[layer];
        rise = (y - foot(layer)) * SPAN * distance;
        // far woods cool and dark, nearer ones a deeper green, their crowns catching the light towards the sun
        vec3 land = mix(vec3(0.16, 0.26, 0.24), vec3(0.17, 0.38, 0.13), fk);
        float cell = hash(vec2(floor(x * (30.0 - 6.0 * float(layer))), float(layer)));
        land = mix(land, land * vec3(0.8, 0.95, 0.72), 0.5 * cell);
        land *= 0.8 + 0.32 * smoothstep(foot(layer), tops[layer] + 0.02, y);
        land += vec3(0.14, 0.12, 0.05) * exp(-toSun * 2.6) * (0.3 + 0.7 * fk);
        colour = land;
    }

    // the haze along the view: thickest low over the land, thinner above it, clear right around you
    float through = band(EYE, EYE + rise, k) * distance;
    float veil = 0.2 * band(EYE, EYE + rise, k * 0.25) * distance;
    // clear around you, the haze coming in gently beyond (as include/haze.glsl, over the whole view)
    float clear = PreviewSun.w, ramp = clear + 20.0;
    float near = distance > clear ? ((distance - clear) - ramp * (1.0 - exp(-(distance - clear) / ramp))) / distance : 0.0;
    // soft banks and the pockets of the second colour, drifting with the wind along the valleys
    float banks = fbm(vec2(x * 1.6 + drift * 0.035, y * 3.0 + 0.4 * float(layer)));
    float pockets = fbm(vec2(x * 0.9 - drift * 0.022 + 4.1, y * 1.6 + 2.3)) * (1.0 - smoothstep(0.52, 0.66, y));
    float tau = sigma * (through * (0.7 + 0.6 * banks) + veil) * near;
    float shown = exp(-tau);
    float hidden = 1.0 - shown;

    float far = smoothstep(0.0, 180.0, distance);
    float up = smoothstep(0.0, 2.5 / k, max(rise, 0.0) * 0.5);
    vec3 haze = mix(PreviewLow.rgb, PreviewHigh.rgb, clamp(0.85 * far + 0.42 * up, 0.0, 1.0));
    haze = mix(haze, PreviewPocket.rgb, smoothstep(0.45, 0.78, pockets) * PreviewPocket.w);
    vec3 glow = PreviewSun.rgb * (0.65 * exp(-toSun * toSun * 34.0) + 0.35 * exp(-toSun * 2.4)) * hidden;

    // mixed as light mixes, as in the world (core/haze.fsh)
    colour = sqrt(colour * colour * shown + haze * haze * hidden) + glow;

    // a whisper of a vignette
    colour *= 1.0 - 0.16 * dot(uv - 0.5, uv - 0.5);
    fragColor = vec4(clamp(colour, 0.0, 1.0), 1.0);
}
