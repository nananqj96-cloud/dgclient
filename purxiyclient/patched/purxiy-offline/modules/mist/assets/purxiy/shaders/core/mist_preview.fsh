#version 330

// Mist's colour picker: a little landscape at sunrise, ridge behind ridge of hills and trees fading into the mist, banks
// of it lying low in the valleys between them and drifting by, the low sun glowing through it, all in one palette and in
// your Density, Height and Sun rays. Drawn into an image of its own and shown in the picker's tile.

layout(std140) uniform PurxiyMistPreview {
    // x, y = size (px), z = which tile (each has its own hills), w = how lit (chosen or hovered: 0..1)
    vec4 PreviewSize;
    // rgb = the mist in the shade, the sun high and the sun low (the palette)
    vec4 PreviewShade;
    vec4 PreviewSun;
    vec4 PreviewLow;
    // x = density (0..1), y = height (0..1), z = sun rays (0..1), w = seconds
    vec4 PreviewLook;
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
        if (r < 0.3) {
            continue;
        }
        float centre = (c + 0.25 + 0.5 * hash(vec2(c, seed + 3.1))) / n;
        float h = height * (0.55 + 0.45 * hash(vec2(c, seed + 7.7)));
        float d = abs(x - centre) * n;
        float crown = r > 0.82 ? max(0.0, 1.0 - d * 2.2) * 1.25 : sqrt(max(0.0, 1.0 - d * d * 3.2));
        best = max(best, h * crown);
    }
    return best;
}

// the top of hill layer k (0 the farthest) at x, in the picture's height (0 bottom, 1 top)
float ridge(float x, int k, float seed) {
    float fk = float(k);
    float base = 0.56 - 0.13 * fk;
    float hills = (fbm(vec2(x * (1.6 + 0.7 * fk) + seed * 3.0 + fk * 11.0, fk)) - 0.5) * (0.16 + 0.05 * fk);
    float forest = trees(x + fk * 0.37, 9.0 + 5.0 * fk, 0.025 + 0.02 * fk, seed + fk);
    return base + hills + forest;
}

void main() {
    vec2 size = PreviewSize.xy;
    vec2 uv = gl_FragCoord.xy / size;
    float aspect = size.x / size.y;
    float x = uv.x * aspect;
    float y = uv.y;
    float time = PreviewLook.w;
    float seed = PreviewSize.z * 1.37 + 2.0;
    float density = PreviewLook.x;
    float height = PreviewLook.y;
    float rays = PreviewLook.z;
    vec3 shade = PreviewShade.rgb, sun = PreviewSun.rgb, low = PreviewLow.rgb;

    // the sun just risen over the far hills, right of the middle, and its glow through the mist
    vec2 sunAt = vec2(0.64 * aspect, 0.6);
    vec2 toSun = vec2(x, y) - sunAt;
    float near = length(toSun);
    float halo = exp(-near * near * 60.0);
    float wide = exp(-near * 3.6);
    vec3 glow = mix(sun, low, 0.75);
    vec3 mist = shade * (0.84 + 0.12 * y) + glow * (0.12 + 0.42 * wide) * (0.35 + 0.65 * rays);

    // the sky: the mist's own colour low down, a soft muted blue high up, the sun's disc
    vec3 sky = mix(mist, mix(vec3(0.62, 0.70, 0.86), shade, 0.55), smoothstep(0.55, 1.05, y) * (1.0 - 0.6 * density));
    sky += glow * halo * 0.4 + mix(vec3(1.0, 0.96, 0.88), low, 0.3) * smoothstep(0.034, 0.024, near) * 0.7;
    vec3 colour = sky;

    // ridge behind ridge: the nearest one a pixel lies in shows; the farther it is, the more mist lies in front of it, and
    // banks of mist lie low in the valley in front of it (just above the next ridge), thicker the higher the mist rises
    float tops[4];
    for (int k = 0; k < 4; k++) {
        tops[k] = ridge(x, k, seed);
    }
    int layer = -1;
    for (int k = 0; k < 4; k++) {
        if (y <= tops[k]) {
            layer = k;
        }
    }
    if (layer >= 0) {
        float fk = float(layer);
        float depth = (3.6 - fk) / 3.6;
        // far hills cool and dark; the nearest woods in their autumn colours, rust and amber
        vec3 land = mix(vec3(0.10, 0.10, 0.13), vec3(0.30, 0.14, 0.07), fk / 3.0);
        land = mix(land, vec3(0.42, 0.24, 0.08), 0.35 * hash(vec2(floor(x * (9.0 + 5.0 * fk)), fk + seed)) * fk / 3.0);
        land = mix(land, shade * 0.35, 0.2);
        // the side of the hills towards the sun catches a little light
        land += glow * 0.06 * smoothstep(0.0, 0.5, 1.0 - near);
        float through = exp(-(0.6 + 2.8 * density) * depth);
        float valley = layer < 3 ? tops[layer + 1] : 0.0;
        float bank = fbm(vec2(x * 3.0 + time * (0.025 + 0.01 * fk) + fk * 5.0, y * 9.0 + fk));
        float lying = exp(-max(y - valley, 0.0) / (0.025 + 0.08 * height)) * (0.35 + 0.65 * smoothstep(0.25, 0.8, bank));
        float hide = 1.0 - through * (1.0 - lying * (0.45 + 0.5 * density));
        colour = mix(land, mist, clamp(hide, 0.0, 1.0));
    }

    // beams from the sun through the mist, faint
    float angle = atan(toSun.y, toSun.x);
    float beam = smoothstep(0.55, 0.95, noise(vec2(angle * 9.0, time * 0.05))) * exp(-near * 2.2) * step(y, sunAt.y + 0.08);
    colour += glow * beam * 0.12 * rays;

    // a whisper of the vignette, and less of everything when not chosen
    float vignette = 1.0 - 0.18 * dot(uv - 0.5, uv - 0.5);
    colour *= vignette * (0.86 + 0.14 * PreviewSize.w);
    fragColor = vec4(clamp(colour, 0.0, 1.0), 1.0);
}
