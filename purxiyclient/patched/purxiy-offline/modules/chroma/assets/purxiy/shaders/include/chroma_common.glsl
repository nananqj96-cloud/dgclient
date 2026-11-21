#version 330

// Chroma's watercolour, shared by the world (core/chroma) and the preview in its settings (core/chroma_preview).

// Over how many blocks a bloom's edge goes from grey to full colour, centred on its radius; keep in sync with
// PaintMap.FEATHER.
const float CHROMA_FEATHER = 4.0;

// How strongly a bloom of this radius paints d blocks from its middle (half at its radius).
float chromaStrength(float radius, float d) {
    return clamp((radius - d) / CHROMA_FEATHER + 0.5, 0.0, 1.0);
}

// A lattice point's own value (0..1): its three coordinates mixed into one word, then scrambled.
float chromaHashf(ivec3 cell) {
    uint h = uint(cell.x) * 0x8da6b343u ^ uint(cell.y) * 0xd8163841u ^ uint(cell.z) * 0xcb1ab31fu;
    h = (h ^ (h >> 16u)) * 0x7feb352du;
    h = (h ^ (h >> 15u)) * 0x846ca68bu;
    h ^= h >> 16u;
    return float(h & 0xFFFFu) / 65535.0;
}

// Smooth noise through the world, a lattice point every 2^k blocks (k < 0: finer than a block). Exact far from the
// origin: whole blocks stay integers.
float chromaNoise(ivec3 block, vec3 inBlock, int k, int seed) {
    ivec3 cell;
    vec3 f;
    if (k >= 0) {
        cell = block >> k;
        f = (vec3(block & ((1 << k) - 1)) + inBlock) / float(1 << k);
    } else {
        int m = 1 << (-k);
        vec3 s = inBlock * float(m);
        ivec3 sub = clamp(ivec3(floor(s)), ivec3(0), ivec3(m - 1));
        cell = block * m + sub;
        f = s - vec3(sub);
    }
    f = f * f * (3.0 - 2.0 * f);
    cell += ivec3(seed * 7919, seed * 104729, seed * 12979);
    float c000 = chromaHashf(cell);
    float c100 = chromaHashf(cell + ivec3(1, 0, 0));
    float c010 = chromaHashf(cell + ivec3(0, 1, 0));
    float c110 = chromaHashf(cell + ivec3(1, 1, 0));
    float c001 = chromaHashf(cell + ivec3(0, 0, 1));
    float c101 = chromaHashf(cell + ivec3(1, 0, 1));
    float c011 = chromaHashf(cell + ivec3(0, 1, 1));
    float c111 = chromaHashf(cell + ivec3(1, 1, 1));
    return mix(mix(mix(c000, c100, f.x), mix(c010, c110, f.x), f.y), mix(mix(c001, c101, f.x), mix(c011, c111, f.x), f.y), f.z);
}

// Where the paint's edge runs: broad lobes (a lattice point every 8 blocks), their bays (2), frills (half a block) and a
// fine fringe (an eighth). What would be finer than a pixel (footprint: blocks per pixel) is left out, so the edge stays
// calm in the distance. Around 0.5, stretched so the sum of octaves is as uneven as one.
float chromaWatercolour(ivec3 block, vec3 inBlock, float footprint) {
    float n = 0.36 * chromaNoise(block, inBlock, 3, 1);
    n += 0.32 * chromaNoise(block, inBlock, 1, 2);
    float frills = smoothstep(0.12, 0.35, footprint);
    n += 0.2 * (frills < 1.0 ? mix(chromaNoise(block, inBlock, -1, 3), 0.5, frills) : 0.5);
    float fringe = smoothstep(0.03, 0.09, footprint);
    n += 0.12 * (fringe < 1.0 ? mix(chromaNoise(block, inBlock, -3, 4), 0.5, fringe) : 0.5);
    return clamp(0.5 + (n - 0.5) * 1.7, 0.0, 1.0);
}

// How much of a column's colour reaches a surface dy blocks above (or below) where it was painted: trees and walls
// above you take it, a cave far below does not.
float chromaBand(float dy) {
    return dy > 0.0 ? 1.0 - smoothstep(9.0, 18.0, dy) : 1.0 - smoothstep(4.0, 10.0, -dy);
}

float chromaLuma(vec3 c) {
    return dot(c, vec3(0.299, 0.587, 0.114));
}

vec3 chromaSaturate(vec3 c, float amount) {
    return clamp(mix(vec3(chromaLuma(c)), c, amount), 0.0, 1.0);
}

// The picture without its colours: its brightness placed between the tone's shadow, middle and light. In the dark (the
// night, caves, deep shade) the grey keeps the picture's own brightness, or the tone would lift it into a bright haze.
vec3 chromaTone(vec3 c, vec3 shadow, vec3 mid, vec3 light) {
    float l = clamp(chromaLuma(c), 0.0, 1.0);
    float k = mix(l, l * l * (3.0 - 2.0 * l), 0.3);
    vec3 tone = k < 0.5 ? mix(shadow, mid, k * 2.0) : mix(mid, light, k * 2.0 - 1.0);
    float dark = 1.0 - smoothstep(0.04, 0.32, l);
    return mix(tone, tone * (l / max(chromaLuma(tone), 0.001)), dark);
}

// Where the watercolour stands: the paint (0 grey .. 1 colour) moved by the noise of the paper, most at its edge and not
// at all where it is full or absent.
float chromaEdge(float paint, float noise) {
    return paint + (noise - 0.5) * 1.1 * min(4.0 * paint * (1.0 - paint) + 0.1, 1.0);
}

// The watercolour over the grey: t from chromaEdge, aa its width on screen. Just inside the edge the pigment pools (a
// little deeper and more vivid, like a drying wash); just outside, a breath of it bleeds into the paper. Fresh paint
// (wet) glows a little.
vec3 chromaPaint(vec3 scene, vec3 grey, float t, float aa, float wet) {
    float c = smoothstep(0.5 - aa, 0.5 + aa, t);
    float rim = c * (1.0 - smoothstep(0.5, 0.5 + max(0.07, 3.0 * aa), t));
    float bleed = (1.0 - c) * smoothstep(0.26, 0.5, t);
    vec3 colour = chromaSaturate(scene, 1.0 + 0.55 * rim + 0.22 * wet) * (1.0 - 0.2 * rim) + 0.03 * wet;
    return mix(grey, colour, clamp(c + 0.18 * bleed, 0.0, 1.0));
}
