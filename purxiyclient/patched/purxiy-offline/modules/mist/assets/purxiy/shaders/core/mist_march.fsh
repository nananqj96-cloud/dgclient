#version 330

#moj_import <purxiy:mist.glsl>
#moj_import <purxiy:mist_field.glsl>

// The mist, marched along every view ray on a grid of half the screen's size (texel q for pixel 2q): from the camera to
// what the pixel shows, or far out for the sky. Out: the light the mist adds (rgb) and how much of the picture it hides (a).
// Each frame only a quarter of the grid is marched afresh; the rest takes over the last frame's march of the same point,
// wherever that point was in the last frame's picture and still was what the pixel showed (off screen, newly uncovered,
// or something moved in front: marched afresh as well).

uniform sampler2D DepthSampler;
uniform sampler2D KeptDepthSampler;
uniform sampler2D KeptSampler;

out vec4 fragColor;

// The last frame's march of this point (rel: camera-relative now), if it holds.
bool kept(vec3 rel, bool sky, out vec4 march) {
    vec4 clip = MistKeptView * (sky ? vec4(rel, 0.0) : vec4(rel + MistMoved.xyz, 1.0));
    if (clip.w <= 1.0e-4) {
        return false;
    }
    vec2 pixel = (clip.xy / clip.w * 0.5 + 0.5) * MistScreen.xy - 0.5;
    ivec2 q = ivec2(floor(pixel * 0.5 + 0.5));
    if (any(lessThan(q, ivec2(0))) || any(greaterThanEqual(q, ivec2(MistScreen.zw)))) {
        return false;
    }
    float before = texelFetch(KeptDepthSampler, min(q * 2, ivec2(MistScreen.xy) - 1), 0).r;
    if (sky) {
        if (before < 0.999999) {
            return false;
        }
    } else if (before >= 0.999999 || abs(mistLinear(before) - clip.w) > 0.03 * clip.w + 0.05) {
        return false;
    }
    march = texelFetch(KeptSampler, q, 0);
    return true;
}

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);
    ivec2 full = min(texel * 2, ivec2(MistScreen.xy) - 1);
    float depth = texelFetch(DepthSampler, full, 0).r;
    bool sky = depth >= 0.999999;
    vec2 uv = (vec2(full) + 0.5) / MistScreen.xy;
    vec3 rel = mistPosition(uv, sky ? 0.9999 : depth);
    float distance = length(rel);
    // right around you the air is clear: nothing to march (the mist there stays below one part in 400)
    if (!sky && distance < 0.35 * MistReach.x) {
        fragColor = vec4(0.0);
        return;
    }
    // in tiles of 8 x 8 texels, so the GPU skips whole groups of pixels at once (a lone texel left to march keeps its
    // whole group busy)
    if (MistRefresh.x > -0.5 && ((texel.x >> 3 & 1) | (texel.y >> 3 & 1) << 1) != int(MistRefresh.x + 0.5)) {
        vec4 march;
        if (kept(rel, sky, march)) {
            fragColor = march;
            return;
        }
    }
    float far = sky ? MistReach.y : min(distance, MistReach.y);
    // a short ray (the ground near you, a wall) needs fewer steps for the same detail as a long one
    int steps = sky ? 26 : int(clamp(8.0 + far * 0.6, 12.0, 26.0));
    fragColor = mistMarch(rel / max(distance, 1.0e-4), far, steps, mistDither(gl_FragCoord.xy));
}
