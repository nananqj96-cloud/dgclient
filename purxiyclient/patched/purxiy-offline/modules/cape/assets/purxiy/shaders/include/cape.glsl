#version 330

// Purxiy capes (see dev.purxiy.cape.CapeTextures). Every design is drawn here, straight into a cape texture in
// Minecraft's 64 by 32 cape layout (the elytra's part of it too), once per mip level, and again for the light the cape
// gives off in the dark. Positions on the cape are in cape pixels ("px"): the outside is 10 by 16 of them, x from the
// left as you look at the wearer's back, y down from the shoulders. Keep the design order in sync with CapeDesign.

layout(std140) uniform PurxiyCape {
    vec4 CapeFrame;   // x = seconds, y = design of the live cape, z = 1 when drawing the glow, w = one target texel in px
    vec4 CapeLayout;  // x = swatches (0 = the live cape), y = swatch columns, z = 1 to leave the elytra out, w = 0
    vec4 CapeLook;    // x = glow strength, yzw = spare
};

uniform sampler2D MarkSampler;
uniform sampler2D ElytraSampler;

// Loops start at ZERO (always 0, but unknown to the compiler), so they are not unrolled: the shader compiles in a
// fraction of the time.
#define ZERO min(int(CapeLayout.w), 0)

#define DESIGN_PURXIY 0
#define DESIGN_MIDNIGHT 1
#define DESIGN_PEARL 2
#define DESIGN_NEBULA 3
#define DESIGN_AURORA 4
#define DESIGN_ECLIPSE 5
#define DESIGN_AMETHYST 6
#define DESIGN_END 7
#define DESIGN_CHERRY 8

const float TAU = 6.28318530718;

// The mark and the wordmark in MarkSampler (printed by tools/cape/build_mark_sdf.py): where each sits in the stored
// image, its aspect, and how far (in widths of it) its distance fields reach from 0.5 to 0 or 1.
const vec2 MARK_TEXELS = vec2(480.0, 572.0);
const vec2 MARK_ORIGIN = vec2(32.0 / 480.0, 32.0 / 572.0);
const vec2 MARK_SIZE = vec2(396.8 / 480.0, 429.6 / 572.0);
const float MARK_ASPECT = 992.0 / 1074.0;
const float MARK_SPREAD = 72.0 / 992.0;
// the last row of the mark's part of the image (the wordmark is below it)
const float MARK_BOTTOM = 493.0 / 572.0;
// where the glasses catch the light (the rivet on the far lens), from the centre of the mark, in widths of the mark
const vec2 MARK_GLINT = vec2(0.4244, -0.1996);
const vec2 WORD_ORIGIN = vec2(6.4 / 480.0, 500.4 / 572.0);
const vec2 WORD_SIZE = vec2(467.2 / 480.0, 64.8 / 572.0);
const float WORD_ASPECT = 1168.0 / 162.0;
const float WORD_SPREAD = 14.0 / 1168.0;

// ------------------------------------------------------------------------------------------------ noise

float vc_hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 vc_hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float vc_noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
    float a = vc_hash(i);
    float b = vc_hash(i + vec2(1.0, 0.0));
    float c = vc_hash(i + vec2(0.0, 1.0));
    float d = vc_hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

// Fractal noise, 0..1, of octaves octaves.
float vc_fbm(vec2 p, int octaves) {
    float sum = 0.0;
    float amp = 0.5;
    float norm = 0.0;
    for (int i = ZERO; i < octaves; i++) {
        sum += amp * vc_noise(p);
        norm += amp;
        p = mat2(1.6, 1.2, -1.2, 1.6) * p;
        amp *= 0.5;
    }
    return sum / norm;
}

mat2 vc_rotate(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, s, -s, c);
}

// Coverage of the inside of a distance field (positive inside, px) on a target texel of aa px.
float vc_cover(float dist, float aa) {
    return clamp(dist / aa + 0.5, 0.0, 1.0);
}

// A point of light of radius r px: a gaussian that never gets thinner than the texel, keeping its energy, so small
// lights fade instead of flickering as the cape gets smaller.
float vc_dot(vec2 d, float r, float aa) {
    float s = max(r, aa * 0.75);
    return exp(-dot(d, d) / (2.0 * s * s)) * (r * r) / (s * s);
}

// A four pointed sparkle at the origin: a core and two thin rays of length len.
float vc_sparkle(vec2 d, float len, float aa) {
    float w = max(len * 0.05, aa * 0.6);
    vec2 a = abs(d);
    float rays = exp(-a.y / w - a.x / (len * 0.42)) + exp(-a.x / w - a.y / (len * 0.42));
    return rays * 0.7 + vc_dot(d, len * 0.12, aa);
}

// Stars on a grid of cells cell px wide: density of the cells hold one, sized about size px, twinkling.
float vc_stars(vec2 p, float cell, float density, float size, float t, float aa, float seed) {
    vec2 id = floor(p / cell);
    float sum = 0.0;
    for (int k = ZERO; k < 9; k++) {
        vec2 cellId = id + vec2(k % 3 - 1, k / 3 - 1);
        vec2 h = vc_hash2(cellId + seed);
        if (vc_hash(cellId + seed * 1.7 + 3.1) > density) {
            continue;
        }
        vec2 centre = (cellId + 0.15 + 0.7 * h) * cell;
        float twinkle = 0.55 + 0.45 * sin(t * (0.8 + 2.2 * h.y) + h.x * TAU);
        sum += vc_dot(p - centre, size * (0.45 + 0.9 * h.x * h.x), aa) * twinkle;
    }
    return sum;
}

// ------------------------------------------------------------------------------------------------ the Purxiy mark

struct Mark {
    float dist;     // px to its edge, positive inside
    float light;    // how light the artwork is here: 0 its darkest (the plaid, the glasses), 1 its lightest fur
    float broad;    // the same, blurred: the broad light and shade of the face
    float reach;    // how far (px) the distance field reaches
    vec2 normal;    // the direction its edge faces (outwards)
};

vec2 vc_markUv(vec2 p, vec2 centre, float width) {
    vec2 m = (p - centre) / width;
    // kept inside the mark's part of the image (its margin reads as far outside), clear of the wordmark below it
    return clamp(MARK_ORIGIN + (m * vec2(1.0, MARK_ASPECT) + 0.5) * MARK_SIZE, vec2(0.0), vec2(1.0, MARK_BOTTOM));
}

// The mark width px wide, centred on centre.
Mark vc_mark(vec2 p, vec2 centre, float width) {
    vec2 uv = vc_markUv(p, centre, width);
    vec4 s = texture(MarkSampler, uv);
    Mark m;
    m.reach = MARK_SPREAD * width;
    m.dist = (s.r - 0.5) * 2.0 * m.reach;
    m.light = s.g;
    m.broad = s.b;
    // the stored image has square texels, so the field's slope is the same in both directions
    vec2 e = vec2(1.0) / MARK_TEXELS;
    float dx = texture(MarkSampler, uv + vec2(e.x, 0.0)).r - texture(MarkSampler, uv - vec2(e.x, 0.0)).r;
    float dy = texture(MarkSampler, uv + vec2(0.0, e.y)).r - texture(MarkSampler, uv - vec2(0.0, e.y)).r;
    vec2 g = vec2(dx, dy);
    m.normal = length(g) > 1e-5 ? -normalize(g) : vec2(0.0, -1.0);
    return m;
}

// Only the distance to the mark's edge (px): for its shadow.
float vc_markDist(vec2 p, vec2 centre, float width) {
    return (texture(MarkSampler, vc_markUv(p, centre, width)).r - 0.5) * 2.0 * MARK_SPREAD * width;
}

// The wordmark ("PURXIY") width px wide, centred on centre: px to the edge of its letters, positive inside.
float vc_word(vec2 p, vec2 centre, float width) {
    vec2 m = (p - centre) / width;
    vec2 uv = WORD_ORIGIN + (m * vec2(1.0, WORD_ASPECT) + 0.5) * WORD_SIZE;
    uv = clamp(uv, WORD_ORIGIN - vec2(0.004), WORD_ORIGIN + WORD_SIZE + vec2(0.004));
    return (texture(MarkSampler, uv).r - 0.5) * 2.0 * WORD_SPREAD * width;
}

// ------------------------------------------------------------------------------------------------ designs

// What a design gives the cape at a point: the colour of the cloth, and the light it gives off in the dark (rgb, and
// how much in a).
struct Cloth {
    vec3 color;
    vec4 glow;
};

void vc_light(inout Cloth c, vec3 color, float amount) {
    amount = clamp(amount, 0.0, 1.0);
    c.glow.rgb = mix(c.glow.rgb, color, amount / max(c.glow.a + amount, 1e-4));
    c.glow.a = max(c.glow.a, amount);
}

// A soft band of light crossing the cape along dir, once every period seconds (offset shifts when).
float vc_sweep(vec2 p, vec2 dir, float t, float period, float offset, float width) {
    float phase = fract((t + offset) / period);
    float along = dot(p, normalize(dir));
    float at = -6.0 + phase * 43.0;
    float d = (along - at) / width;
    return exp(-d * d);
}

// The mark on the cloth. style 0: the dog in pale lilac on violet; 1: its outline in light on black, the dog only a
// faint print inside; 2: violet on pearl. The light and shade inside are the artwork's own, in the cape's colours.
void vc_markLayer(inout Cloth c, vec2 p, vec2 centre, float width, float t, float aa, int style) {
    Mark m = vc_mark(p, centre, width);
    float inside = vc_cover(m.dist, aa);
    // the details where the cape is drawn large enough to hold them, the broad shading where it is not (they would
    // flicker on the small mip levels)
    float fine = smoothstep(25.0, 70.0, width / aa);
    float ink = mix(m.broad, m.light, fine);
    // the glasses catch the light: a sparkle on the rivet that breathes, and flares now and then
    float beat = 0.5 + 0.5 * sin(t * TAU / 3.2);
    float flare = pow(max(0.0, sin(t * TAU / 7.0 + 1.3)), 24.0);
    vec2 glintAt = centre + MARK_GLINT * width;
    // a glint that crosses the mark now and then
    float glint = vc_sweep(p, vec2(1.0, 0.55), t, 7.5, 2.0, 0.55);

    if (style == 1) {
        float line = max(width * 0.018, 0.1);
        float stroke = vc_cover(line - abs(m.dist), aa);
        vec3 lineInk = vec3(0.74, 0.60, 1.0);
        vec3 print = vec3(0.03, 0.018, 0.06) + vec3(0.11, 0.075, 0.20) * ink * ink;
        c.color = mix(c.color, c.color + print * (1.0 + glint * 0.8), inside);
        vec3 lit = mix(lineInk, vec3(1.0, 0.96, 1.0), clamp(glint * 0.8, 0.0, 1.0));
        c.color = mix(c.color, lit, stroke);
        // a faint halo around the line
        float halo = exp(-abs(m.dist) / 0.35) * (1.0 - stroke) * (1.0 - smoothstep(0.5, 0.9, abs(m.dist) / m.reach));
        c.color += lineInk * halo * 0.10;
        vc_light(c, lit, stroke * 0.8);
        vc_light(c, lineInk, inside * ink * ink * 0.2);
        float spark = vc_sparkle(p - glintAt, width * (0.10 + 0.12 * flare + 0.03 * beat), aa);
        c.color += vec3(0.85, 0.78, 1.0) * spark * (0.25 + 0.75 * flare);
        vc_light(c, vec3(1.0), spark * (0.3 + 0.7 * flare));
        return;
    }

    vec3 dark, light, edge, shadowTint;
    if (style == 2) {
        dark = vec3(0.21, 0.08, 0.52);
        light = vec3(0.76, 0.60, 1.0);
        edge = vec3(0.86, 0.78, 1.0);
        shadowTint = vec3(0.55, 0.45, 0.75);
    } else {
        dark = vec3(0.12, 0.045, 0.29);
        light = vec3(0.99, 0.92, 1.0);
        edge = vec3(1.0);
        shadowTint = vec3(0.02, 0.0, 0.06);
    }
    // soft shadow below the mark, and a lilac glow around it
    float below = vc_markDist(p - vec2(0.10, 0.24) * width / 8.0, centre, width);
    float shadow = smoothstep(-0.9, 0.25, below) * (1.0 - inside);
    c.color = mix(c.color, shadowTint, shadow * (style == 2 ? 0.28 : 0.45));
    if (style == 0) {
        float around = exp(min(m.dist, 0.0) / 0.55) * (1.0 - smoothstep(0.5, 0.9, -m.dist / m.reach));
        c.color += vec3(0.42, 0.28, 0.85) * around * (1.0 - inside) * 0.35;
    }

    // the fill: the artwork's light and shade between the two colours, lit along the edges that face the light
    vec3 fill = mix(dark, light, pow(ink, 0.9));
    float bevel = exp(-max(m.dist, 0.0) / 0.16);
    float facing = dot(m.normal, normalize(vec2(-0.6, -1.0)));
    fill = mix(fill, edge, bevel * max(facing, 0.0) * 0.45);
    fill *= 1.0 - bevel * max(-facing, 0.0) * (style == 2 ? 0.22 : 0.16);
    // the glint lights the lighter parts most
    fill = mix(fill, vec3(1.0), glint * (0.2 + 0.4 * ink));

    c.color = mix(c.color, fill, inside);
    float spark = vc_sparkle(p - glintAt, width * (0.09 + 0.14 * flare + 0.03 * beat), aa);
    c.color += (style == 2 ? vec3(0.55, 0.40, 0.95) : vec3(0.95, 0.88, 1.0)) * spark * (0.20 + 0.8 * flare) * (1.0 - inside * 0.5);

    vc_light(c, fill, inside * (style == 2 ? 0.3 : 0.5) * (0.35 + 0.65 * ink));
    vc_light(c, vec3(1.0), spark * (0.4 + 0.6 * flare));
}

// The wordmark under the mark, in the style of the mark (see vc_markLayer), with the same glint crossing it.
void vc_wordLayer(inout Cloth c, vec2 p, vec2 centre, float width, float t, float aa, int style) {
    float d = vc_word(p, centre, width);
    float inside = vc_cover(d, aa);
    float glint = vc_sweep(p, vec2(1.0, 0.55), t, 7.5, 2.0, 0.55);
    vec3 ink = style == 2 ? vec3(0.50, 0.32, 0.94) : (style == 1 ? vec3(0.72, 0.58, 1.0) : vec3(0.88, 0.80, 1.0));
    ink = mix(ink, vec3(1.0), glint * (style == 2 ? 0.35 : 0.6));
    c.color = mix(c.color, ink, inside * (style == 2 ? 0.9 : 0.85));
    vc_light(c, ink, inside * 0.5);
}

// Soft folds of cloth hanging from the shoulders.
float vc_folds(vec2 p, float t) {
    return vc_fbm(vec2(p.x * 0.42 + 0.3 * sin(t * 0.2), p.y * 0.05 + 1.7), 3);
}

Cloth purxiy(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    float y = p.y / 16.0;
    c.color = mix(vec3(0.225, 0.130, 0.440), vec3(0.085, 0.045, 0.180), smoothstep(0.0, 1.0, y));
    c.color *= 0.86 + 0.26 * vc_folds(p, t);
    vec2 centre = vec2(5.0, 6.5);
    vec2 d = (p - centre) * vec2(1.0, 0.75);
    float breath = 0.5 + 0.5 * sin(t * TAU / 5.0);
    c.color += vec3(0.46, 0.30, 0.95) * exp(-dot(d, d) / 9.0) * (0.16 + 0.08 * breath);
    return c;
}

Cloth midnight(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    c.color = mix(vec3(0.052, 0.048, 0.074), vec3(0.030, 0.028, 0.044), p.y / 16.0);
    // a faint violet light rising from the hem
    c.color += vec3(0.10, 0.05, 0.22) * exp(-(16.0 - p.y) / 3.5) * (0.8 + 0.2 * sin(t * TAU / 7.0));
    // satin: long soft highlights down the cloth
    float satin = vc_fbm(vec2(p.x * 0.55, p.y * 0.04 + 5.0), 3);
    c.color += vec3(0.05, 0.045, 0.08) * smoothstep(0.45, 0.9, satin);
    // a glint of light that crosses the whole cape now and then
    float glint = vc_sweep(p, vec2(1.0, 0.6), t, 6.0, 0.0, 0.9);
    c.color += vec3(0.10, 0.09, 0.14) * glint;
    // a thin frame of light, inset from the edges
    vec2 q = abs(p - vec2(5.0, 8.0)) - vec2(5.0 - 0.62, 8.0 - 0.62);
    float frame = abs(max(q.x, q.y));
    float line = vc_cover(0.055 - frame, aa);
    vec3 ink = vec3(0.70, 0.56, 1.0);
    float shimmer = 0.75 + 0.25 * sin((p.x + p.y) * 0.9 - t * 1.4);
    c.color = mix(c.color, mix(ink, vec3(1.0), glint * 0.7) * shimmer, line);
    vc_light(c, ink, line * 0.8);
    return c;
}

Cloth pearl(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    // pearl: white with soft colour that flows through it
    vec2 q = p * 0.2;
    float n1 = vc_fbm(q + vec2(t * 0.025, -t * 0.018), 4);
    float n2 = vc_fbm(q * 1.3 + vec2(3.1 - t * 0.02, 1.7 + t * 0.015) + n1, 4);
    vec3 lilac = vec3(0.925, 0.885, 1.0);
    vec3 blush = vec3(1.0, 0.905, 0.945);
    vec3 ice = vec3(0.885, 0.955, 1.0);
    vec3 pearlColor = mix(mix(lilac, blush, smoothstep(0.35, 0.7, n1)), ice, smoothstep(0.45, 0.8, n2) * 0.8);
    c.color = pearlColor * (0.92 + 0.10 * vc_folds(p, t));
    // a slow sheen
    c.color += vec3(0.06) * vc_sweep(p, vec2(1.0, 0.45), t, 9.0, 4.0, 1.6);
    return c;
}

Cloth nebula(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    vec2 q = p * 0.15 + vec2(0.0, t * 0.012);
    float w1 = vc_fbm(q + vec2(t * 0.02, 0.0), 4);
    float w2 = vc_fbm(q * 1.3 + 1.8 * w1 + vec2(1.7, 9.2) - t * 0.015, 4);
    float n = vc_fbm(q * 1.1 + 1.6 * w2 + vec2(8.3, 2.8), 5);
    vec3 deep = vec3(0.026, 0.016, 0.085);
    vec3 violet = vec3(0.44, 0.22, 0.80);
    vec3 magenta = vec3(0.86, 0.32, 0.78);
    vec3 blue = vec3(0.16, 0.28, 0.74);
    vec3 cloud = mix(blue, violet, smoothstep(0.3, 0.7, w2));
    cloud = mix(cloud, magenta, smoothstep(0.55, 0.85, n) * 0.75);
    float density = smoothstep(0.30, 0.82, n) * (0.55 + 0.45 * w1);
    // darker lanes of dust
    float lanes = smoothstep(0.58, 0.78, vc_fbm(q * 2.2 + 7.0 + w1, 4));
    density *= 1.0 - 0.5 * lanes;
    c.color = deep + cloud * density * 0.85;
    float core = density * density * density;
    c.color += vec3(0.95, 0.78, 1.0) * core * 0.4;
    // stars: dust, small, and a few bright ones with rays
    float s1 = vc_stars(p, 0.7, 0.55, 0.05, t, aa, 1.0);
    float s2 = vc_stars(p, 1.6, 0.45, 0.085, t * 1.3, aa, 7.0);
    vec2 cell = floor(p / 4.0);
    float bright = 0.0;
    for (int k = ZERO; k < 9; k++) {
        vec2 id = cell + vec2(k % 3 - 1, k / 3 - 1);
        vec2 h = vc_hash2(id * 3.7 + 11.0);
        vec2 at = (id + 0.2 + 0.6 * h) * 4.0;
        float pulse = 0.6 + 0.4 * sin(t * (0.7 + h.y) + h.x * TAU);
        bright += vc_sparkle(p - at, 0.55 + 0.35 * h.x, aa) * pulse * step(0.35, h.y);
    }
    vec3 starColor = vec3(0.92, 0.9, 1.0);
    c.color += starColor * (s1 * 0.7 + s2 + bright * 0.9);
    vc_light(c, cloud * 1.2 + 0.1, density * 0.45);
    vc_light(c, starColor, clamp(s2 + bright, 0.0, 1.0));
    return c;
}

Cloth aurora(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    float y = p.y / 16.0;
    c.color = mix(vec3(0.020, 0.040, 0.105), vec3(0.030, 0.085, 0.120), y);
    float stars = vc_stars(p, 0.9, 0.5, 0.05, t, aa, 3.0);
    c.color += vec3(0.85, 0.92, 1.0) * stars * 0.8 * (1.0 - smoothstep(6.0, 12.0, p.y));
    // two curtains, waving
    vec3 total = vec3(0.0);
    float light = 0.0;
    for (int i = ZERO; i < 2; i++) {
        float fi = float(i);
        float sway = sin(p.x * 0.55 + t * 0.45 + fi * 2.1) * 0.9 + (vc_fbm(vec2(p.x * 0.22 + fi * 5.0, t * 0.07), 4) - 0.5) * 4.0;
        float base = 8.6 + fi * 1.9 + sway;
        float rays = 0.45 + 0.55 * pow(vc_noise(vec2(p.x * 2.4 + t * 0.35 + fi * 13.0, fi)), 2.0);
        float above = base - p.y;
        float band = exp(-max(above, 0.0) / (3.2 - fi)) * smoothstep(-0.35, 0.25, above);
        float strength = band * rays * (0.85 - 0.3 * fi);
        vec3 tint = mix(vec3(0.30, 1.0, 0.64), vec3(0.62, 0.42, 1.0), smoothstep(0.0, 4.0, above));
        total += tint * strength;
        light = max(light, strength);
    }
    c.color += total;
    // a line of snowy spruce hills along the bottom
    float hills = 13.4 + 0.7 * sin(p.x * 0.7 + 1.0) + 0.4 * sin(p.x * 1.7);
    float spruce = 0.0;
    float cellX = floor(p.x / 1.05);
    for (int k = ZERO; k < 3; k++) {
        float id = cellX + float(k - 1);
        float h = vc_hash(vec2(id, 4.0));
        float cx = (id + 0.5) * 1.05 + (h - 0.5) * 0.4;
        float top = hills - 1.2 - 1.3 * h;
        float spread = (p.y - top) * 0.36;
        spruce = max(spruce, vc_cover(spread - abs(p.x - cx), aa) * step(top, p.y));
    }
    float ground = vc_cover(p.y - hills, aa);
    vec3 dark = vec3(0.012, 0.030, 0.050);
    c.color = mix(c.color, dark, max(spruce, ground));
    float snow = ground * smoothstep(hills + 0.02, hills + 0.25, p.y) * (1.0 - smoothstep(hills + 0.3, hills + 1.8, p.y));
    c.color = mix(c.color, vec3(0.62, 0.74, 0.86), snow * 0.35);
    vc_light(c, total * 1.3, light * 0.75 * (1.0 - max(spruce, ground)));
    return c;
}

Cloth eclipse(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    vec2 centre = vec2(5.0, 5.9);
    vec2 d = p - centre;
    float r = length(d);
    float R = 2.1;
    c.color = mix(vec3(0.060, 0.032, 0.110), vec3(0.012, 0.010, 0.022), clamp(r / 8.0, 0.0, 1.0));
    c.color += vc_stars(p, 1.1, 0.4, 0.05, t, aa, 5.0) * vec3(0.8, 0.8, 1.0) * smoothstep(3.5, 6.0, r);
    // the corona: long streamers that slowly turn and breathe, over a soft halo
    vec2 dir = r > 1e-4 ? d / r : vec2(0.0, -1.0);
    float n = vc_fbm(vc_rotate(t * 0.025) * dir * 2.4 + vec2(t * 0.05, -t * 0.03), 4);
    float fine = vc_fbm(vc_rotate(-t * 0.04) * dir * 7.0 + 9.0, 3);
    float reach = 0.6 + 3.4 * n * n + 0.6 * fine;
    float outside = max(r - R, 0.0);
    float streamers = exp(-outside / (reach * 0.7)) * (0.4 + 0.8 * smoothstep(0.25, 0.75, n));
    float halo = exp(-outside / 1.4) * 0.45;
    float rim = exp(-outside / 0.12);
    float corona = (streamers * 0.8 + halo + rim * 0.95) * step(R - 0.02, r);
    float breath = 0.9 + 0.1 * sin(t * TAU / 6.0);
    corona *= breath;
    vec3 hotColor = vec3(1.0, 0.97, 1.0);
    vec3 midColor = vec3(0.80, 0.64, 1.0);
    vec3 farColor = vec3(0.44, 0.24, 0.88);
    vec3 cc = mix(farColor, midColor, smoothstep(0.1, 0.55, corona));
    cc = mix(cc, hotColor, smoothstep(0.6, 1.2, corona));
    c.color += cc * corona;
    // the moon: black, lit only at its rim
    float disc = vc_cover(R - r, aa);
    c.color = mix(c.color, vec3(0.006, 0.005, 0.012), disc);
    // the diamond ring: a bead of light on the rim now and then
    float phase = fract(t / 8.0);
    float bead = smoothstep(0.0, 0.08, phase) * (1.0 - smoothstep(0.18, 0.4, phase));
    vec2 beadAt = centre + vec2(cos(-0.9), sin(-0.9)) * R;
    float spark = vc_sparkle(p - beadAt, 1.8 * bead + 0.01, aa) * bead;
    c.color += hotColor * spark;
    vc_light(c, cc, clamp(corona, 0.0, 1.0) * (1.0 - disc));
    vc_light(c, hotColor, spark);
    return c;
}

// The crystals of the amethyst cape, back to front: base (px), angle (radians from straight up), length, half width.
const vec4 CRYSTALS[9] = vec4[9](
    vec4(-0.3, 9.5, 1.12, 4.2), vec4(10.3, 7.0, -1.18, 4.6), vec4(2.4, 16.8, -0.42, 7.6), vec4(7.8, 16.9, 0.38, 8.4),
    vec4(-0.2, 15.0, 0.85, 3.3), vec4(10.2, 14.2, -0.80, 3.6), vec4(5.1, 17.2, 0.02, 11.2), vec4(3.4, 17.0, -0.20, 5.9),
    vec4(6.9, 17.1, 0.24, 5.2));
const vec2 CRYSTAL_SIZE[9] = vec2[9](
    vec2(0.50, 0.5), vec2(0.52, 0.1), vec2(0.72, 0.2), vec2(0.78, 0.7), vec2(0.46, 0.6), vec2(0.48, 0.8), vec2(1.05, 0.45),
    vec2(0.62, 0.9), vec2(0.58, 0.3));

Cloth amethyst(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    // the inside of a geode: dark, with a glow from deep inside
    float n = vc_fbm(p * 0.45 + 2.0, 4);
    c.color = mix(vec3(0.055, 0.025, 0.095), vec3(0.14, 0.06, 0.22), n);
    vec2 g = p - vec2(5.0, 10.5);
    c.color += vec3(0.30, 0.12, 0.55) * exp(-dot(g, g) / 22.0) * 0.45;
    for (int i = ZERO; i < 9; i++) {
        vec4 k = CRYSTALS[i];
        float halfWidth = CRYSTAL_SIZE[i].x;
        float tint = CRYSTAL_SIZE[i].y;
        vec2 axis = vec2(sin(k.z), -cos(k.z));
        vec2 across = vec2(-axis.y, axis.x);
        vec2 d = p - k.xy;
        float a = dot(d, axis);
        float b = dot(d, across);
        float len = k.w;
        float tip = halfWidth * 1.35;
        // the outline: straight sides, then a point
        float dist = min(min(halfWidth - abs(b), (len - a) * (halfWidth / tip) - abs(b)), a + 0.6);
        float inside = vc_cover(dist, aa);
        if (inside <= 0.0) {
            continue;
        }
        // three faces across it, a lit one, the middle and a shaded one; the tip's faces a shade lighter
        float face = b / halfWidth;
        vec3 dark = mix(vec3(0.26, 0.11, 0.48), vec3(0.34, 0.14, 0.56), tint);
        vec3 mid = mix(vec3(0.52, 0.30, 0.86), vec3(0.62, 0.34, 0.90), tint);
        vec3 lit = mix(vec3(0.80, 0.64, 1.0), vec3(0.90, 0.70, 1.0), tint);
        vec3 col = face < -0.33 ? lit : (face < 0.33 ? mid : dark);
        col = mix(col, col * 1.12 + 0.04, step(len - tip, a));
        // light running up the crystal
        float run = exp(-pow((a / len - fract(t / 5.0 + tint * 3.7) * 1.4 + 0.2) / 0.12, 2.0));
        col += vec3(0.30, 0.22, 0.45) * run;
        // the edges between faces catch the light
        float ridge = exp(-abs(abs(face) - 0.33) * halfWidth / max(aa, 0.03));
        col += vec3(0.20, 0.16, 0.28) * ridge * 0.6;
        c.color = mix(c.color, col, inside);
        vc_light(c, col, inside * (0.30 + 0.45 * run));
    }
    // glitter: sparkles that come and go
    vec2 cell = floor(p / 1.6);
    float glitter = 0.0;
    for (int k = ZERO; k < 9; k++) {
        vec2 id = cell + vec2(k % 3 - 1, k / 3 - 1);
        vec2 h = vc_hash2(id + 17.0);
        vec2 at = (id + 0.2 + 0.6 * h) * 1.6;
        float life = fract(t / (2.5 + 2.0 * h.y) + h.x);
        float on = smoothstep(0.0, 0.15, life) * (1.0 - smoothstep(0.15, 0.45, life));
        glitter += vc_sparkle(p - at, 0.55, aa) * on;
    }
    c.color += vec3(0.95, 0.85, 1.0) * glitter;
    vc_light(c, vec3(1.0, 0.92, 1.0), glitter);
    return c;
}

// The portal's colours, far to near: deep teal to pale mint, with a little violet.
const vec3 END_COLORS[4] = vec3[4](vec3(0.10, 0.42, 0.44), vec3(0.22, 0.66, 0.58), vec3(0.62, 0.95, 0.80), vec3(0.50, 0.36, 0.86));

Cloth theEnd(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    // deep, darker at the edges, with a slow mist of the portal's light
    vec2 centre = vec2(5.0, 8.0);
    float mist = vc_fbm(p * 0.2 + vec2(t * 0.03, t * 0.02), 4);
    vec2 v = (p - centre) / vec2(6.0, 9.0);
    c.color = mix(vec3(0.010, 0.026, 0.034), vec3(0.030, 0.100, 0.100), mist) * (1.0 - 0.45 * dot(v, v));
    // layers of flecks, each drifting its own way and all turning slowly round the middle, nearer ones larger,
    // brighter and paler, like the End portal
    float light = 0.0;
    vec3 sum = vec3(0.0);
    for (int i = ZERO; i < 7; i++) {
        float fi = float(i);
        float near = fi / 6.0;
        float scale = 2.3 - fi * 0.26;
        vec2 dir = vec2(cos(fi * 2.4 + 0.3), sin(fi * 2.4 + 0.3));
        vec2 turned = vc_rotate(t * (0.012 + 0.008 * near) + fi * 1.1) * (p - centre);
        vec2 q = turned * scale + dir * t * (0.18 + 0.05 * fi) + fi * 17.0;
        vec2 id = floor(q);
        vec2 f = fract(q);
        vec2 h = vc_hash2(id + fi * 31.0);
        float has = step(0.7 - near * 0.1, vc_hash(id * 1.3 + fi));
        vec2 at = 0.25 + 0.5 * h;
        float size = 0.05 + 0.07 * h.y;
        vec2 e = abs(f - at) - size;
        float square = vc_cover(-max(e.x, e.y) / scale, aa);
        float bloom = vc_dot((f - at) / scale, size * 2.2 / scale, aa) * 0.35;
        float twinkle = 0.6 + 0.4 * sin(t * (1.0 + h.x) + h.y * TAU);
        vec3 tint = h.x > 0.85 ? END_COLORS[3] : mix(END_COLORS[0], mix(END_COLORS[1], END_COLORS[2], near), near);
        float amount = has * (square + bloom) * twinkle * (0.35 + 0.65 * near);
        sum += tint * amount;
        light = max(light, has * square * twinkle * near);
    }
    c.color += sum;
    vc_light(c, sum + 0.05, light);
    return c;
}

// A cherry blossom of radius r at the origin: five round petals.
float vc_blossom(vec2 d, float r, float spin) {
    float a = atan(d.y, d.x) + spin;
    return r * (0.62 + 0.38 * abs(cos(a * 2.5))) - length(d);
}

// The branch: where it bends, from the left edge across the top.
const vec2 BRANCH[7] = vec2[7](vec2(-0.6, 3.6), vec2(1.5, 3.0), vec2(3.5, 2.1), vec2(5.4, 2.4), vec2(7.2, 1.6), vec2(9.0, 1.9), vec2(10.8, 1.2));
const vec3 BLOSSOMS[9] = vec3[9](vec3(1.3, 2.4, 0.70), vec3(2.9, 3.4, 0.62), vec3(4.3, 1.5, 0.74), vec3(6.2, 3.1, 0.66), vec3(8.4, 2.2, 0.76),
    vec3(1.9, 5.5, 0.55), vec3(9.5, 3.7, 0.58), vec3(3.6, 4.6, 0.5), vec3(7.0, 0.9, 0.52));

Cloth cherry(vec2 p, float t, float aa) {
    Cloth c;
    c.glow = vec4(0.0);
    float y = p.y / 16.0;
    c.color = mix(vec3(1.0, 0.915, 0.945), vec3(0.965, 0.760, 0.855), y);
    c.color *= 0.96 + 0.06 * vc_fbm(p * 0.3 + t * 0.01, 3);
    // soft, out of focus blossoms far behind
    for (int i = ZERO; i < 6; i++) {
        float fi = float(i);
        vec2 at = vec2(1.0 + fi * 1.7 + 0.6 * sin(fi * 3.1), 7.0 + 5.5 * fract(fi * 0.618) + 0.3 * sin(t * 0.3 + fi));
        float r = 0.9 + 0.5 * fract(fi * 0.37);
        float bokeh = smoothstep(r, r * 0.6, length(p - at));
        c.color = mix(c.color, vec3(1.0, 0.86, 0.92), bokeh * 0.45);
    }
    // the branch, with a twig down to the left
    vec3 bark = vec3(0.34, 0.18, 0.23);
    float branch = 1e3;
    for (int i = ZERO; i < 6; i++) {
        vec2 a = BRANCH[i];
        vec2 ab = BRANCH[i + 1] - a;
        float h = clamp(dot(p - a, ab) / dot(ab, ab), 0.0, 1.0);
        float width = mix(0.34, 0.12, (float(i) + h) / 6.0);
        branch = min(branch, length(p - a - ab * h) - width);
    }
    vec2 ta = vec2(3.5, 2.2);
    vec2 tb = vec2(1.9, 5.3);
    float th = clamp(dot(p - ta, tb - ta) / dot(tb - ta, tb - ta), 0.0, 1.0);
    branch = min(branch, length(p - ta - (tb - ta) * th) - mix(0.15, 0.06, th));
    c.color = mix(c.color, bark, vc_cover(-branch, aa));
    // blossoms along it, turning a little in the breeze
    for (int i = ZERO; i < 9; i++) {
        vec3 b = BLOSSOMS[i];
        float fi = float(i);
        float spin = fi * 0.9 + 0.15 * sin(t * 0.8 + fi);
        vec2 d = p - b.xy;
        float inside = vc_cover(vc_blossom(d, b.z, spin), aa);
        vec3 bloom = mix(vec3(1.0, 0.975, 0.985), vec3(0.985, 0.66, 0.79), smoothstep(0.0, b.z, length(d)));
        c.color = mix(c.color, bloom, inside);
        c.color = mix(c.color, vec3(0.88, 0.32, 0.55), vc_cover(b.z * 0.2 - length(d), aa));
    }
    // petals drifting down, turning as they fall
    for (int layer = ZERO; layer < 2; layer++) {
        float fl = float(layer);
        float size = 2.0 - fl * 0.55;
        vec2 q = p / size;
        float fall = t * (0.45 + 0.2 * fl);
        float column = floor(q.x);
        for (int k = ZERO; k < 3; k++) {
            float col = column + float(k - 1);
            float h = vc_hash(vec2(col, fl * 7.0));
            float cy = q.y - fall * (0.8 + 0.4 * h) - h * 8.0;
            float row = floor(cy);
            float h2 = vc_hash(vec2(col, row + fl * 13.0));
            if (h2 > 0.78) {
                continue;
            }
            vec2 at = vec2(col + 0.5 + 0.3 * sin(fall * 1.3 + h2 * TAU), row + 0.5);
            vec2 d = (vec2(q.x, cy) - at) * size;
            d = vc_rotate(fall * (1.2 + h2) + h * TAU) * d;
            float flip = 0.55 + 0.45 * abs(sin(fall * 2.0 + h * 5.0));
            float petal = 0.34 - length(d * vec2(1.0, 1.0 / flip) * vec2(1.0, 1.6));
            float inside = vc_cover(petal, aa) * (0.95 - 0.25 * fl);
            vec3 tint = mix(vec3(0.99, 0.64, 0.78), vec3(1.0, 0.86, 0.92), clamp(d.x * 3.0 + 0.5, 0.0, 1.0));
            c.color = mix(c.color, tint, inside);
        }
    }
    return c;
}

// ------------------------------------------------------------------------------------------------ the whole cape

// A design at p; on an elytra's wing (the game mirrors it for the other wing) the capes with the mark leave the
// wordmark out.
Cloth vc_design(int design, vec2 p, float t, float aa, bool wing) {
    if (design <= DESIGN_PEARL) {
        // the three capes with the mark share one copy of it: where it sits, how wide, and its style
        Cloth c;
        vec4 place;
        if (design == DESIGN_MIDNIGHT) {
            c = midnight(p, t, aa);
            place = vec4(5.0, 6.1, 6.2, 1.0);
        } else if (design == DESIGN_PEARL) {
            c = pearl(p, t, aa);
            place = vec4(5.0, 6.6, 7.0, 2.0);
        } else {
            c = purxiy(p, t, aa);
            place = vec4(5.0, 6.6, 7.2, 0.0);
        }
        int style = int(place.w + 0.5);
        vc_markLayer(c, p, place.xy, place.z, t, aa, style);
        if (!wing) {
            vc_wordLayer(c, p, vec2(5.0, 14.0), style == 1 ? 4.2 : 4.8, t, aa, style);
        }
        return c;
    }
    if (design == DESIGN_NEBULA) return nebula(p, t, aa);
    if (design == DESIGN_AURORA) return aurora(p, t, aa);
    if (design == DESIGN_ECLIPSE) return eclipse(p, t, aa);
    if (design == DESIGN_AMETHYST) return amethyst(p, t, aa);
    if (design == DESIGN_END) return theEnd(p, t, aa);
    return cherry(p, t, aa);
}

// The colour of the edges of the cloth, and of its inside (the lining).
vec3 vc_trim(int design) {
    if (design == DESIGN_MIDNIGHT) return vec3(0.58, 0.46, 0.90);
    if (design == DESIGN_PEARL) return vec3(0.74, 0.62, 0.98);
    if (design == DESIGN_NEBULA) return vec3(0.52, 0.36, 0.92);
    if (design == DESIGN_AURORA) return vec3(0.36, 0.84, 0.66);
    if (design == DESIGN_ECLIPSE) return vec3(0.80, 0.70, 1.0);
    if (design == DESIGN_AMETHYST) return vec3(0.70, 0.52, 1.0);
    if (design == DESIGN_END) return vec3(0.60, 0.70, 0.56);
    if (design == DESIGN_CHERRY) return vec3(0.92, 0.56, 0.70);
    return vec3(0.76, 0.62, 1.0);
}

vec3 vc_lining(int design) {
    if (design == DESIGN_MIDNIGHT) return vec3(0.050, 0.046, 0.068);
    if (design == DESIGN_PEARL) return vec3(0.880, 0.850, 0.930);
    if (design == DESIGN_NEBULA) return vec3(0.070, 0.045, 0.160);
    if (design == DESIGN_AURORA) return vec3(0.030, 0.070, 0.100);
    if (design == DESIGN_ECLIPSE) return vec3(0.030, 0.025, 0.050);
    if (design == DESIGN_AMETHYST) return vec3(0.130, 0.065, 0.215);
    if (design == DESIGN_END) return vec3(0.030, 0.065, 0.075);
    if (design == DESIGN_CHERRY) return vec3(0.960, 0.820, 0.880);
    return vec3(0.130, 0.075, 0.250);
}

// The whole cape texture at a point of the 64 by 32 layout: rgb, and alpha (0 where the elytra has no wing).
//
//   the cape: its outside (1..11, 1..17) shows the design with a thin trim round it; its inside (12..22) is the lining;
//             its edges (the 1 px sides, top and bottom) are trim
//   the elytra (22..46, 0..22): the wing's outer face (36..46, 2..22) shows the design, the rest is trim; the wing's
//             shape is the game's own elytra texture's alpha
vec4 vc_cape(int design, vec2 uv, float t, float aa, bool glow) {
    vec3 trim = vc_trim(design);
    bool outside = uv.x >= 1.0 && uv.x < 11.0 && uv.y >= 1.0 && uv.y < 17.0;
    bool wing = uv.x >= 36.0 && uv.x < 46.0 && uv.y >= 2.0 && uv.y < 22.0;
    bool lining = uv.x >= 12.0 && uv.x < 22.0 && uv.y >= 1.0 && uv.y < 17.0;
    bool elytra = uv.x >= 22.0;
    float alpha = elytra ? texelFetch(ElytraSampler, ivec2(uv), 0).a : 1.0;
    Cloth c;
    c.glow = vec4(0.0);
    if (outside || wing) {
        // the wing is 10 by 20 (the game mirrors it for the other wing): the design keeps its proportions, centred on
        // the wing, reaching a little past its own top and bottom
        vec2 p = outside ? uv - 1.0 : vec2(uv.x - 36.0, uv.y - 4.0);
        c = vc_design(design, p, t, aa, wing);
        // a thin band of trim round the edge (on a wing only its outer edge, u 36), the cloth softly shaded inside it
        vec2 q = min(p, vec2(10.0, 16.0) - p);
        float edge = outside ? min(q.x, q.y) : uv.x - 36.0;
        float band = vc_cover(0.24 - edge, aa);
        float shade = exp(-max(edge - 0.24, 0.0) / 0.35) * (1.0 - band);
        c.color *= 1.0 - shade * 0.18;
        float sheen = 0.88 + 0.12 * sin((p.x - p.y) * 0.8 + t * 0.9);
        c.color = mix(c.color, trim * sheen, band);
        vc_light(c, trim, band * 0.45);
    } else if (lining) {
        // the inside of the cape: plain, softly woven, a little darker towards the hem
        vec2 p = vec2(uv.x - 12.0, uv.y - 1.0);
        float weave = vc_noise(p * vec2(2.0, 0.35)) * 0.5 + 0.5 * vc_noise(p * vec2(0.4, 3.0));
        c.color = vc_lining(design) * (0.9 + 0.12 * weave) * (1.0 - 0.15 * p.y / 16.0);
    } else {
        c.color = trim * 0.82;
        c.glow = vec4(trim, 0.3);
    }
    if (!outside && !wing && !lining && !elytra && (uv.x >= 22.0 || uv.y >= 17.0)) {
        alpha = 0.0;
    }
    if (glow) {
        return vec4(c.glow.a > 0.001 ? c.glow.rgb : c.color, c.glow.a * CapeLook.x * alpha);
    }
    return vec4(c.color, alpha);
}
