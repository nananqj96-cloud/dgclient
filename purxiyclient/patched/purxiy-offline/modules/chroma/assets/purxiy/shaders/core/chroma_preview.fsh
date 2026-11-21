#version 330

#moj_import <purxiy:chroma_common.glsl>

// Chroma's preview (see dev.purxiy.chroma.ChromaPreview): a little landscape seen from the side, in the grey of the tone
// except where the drop of light has painted it, with the watercolour of the world.

layout(std140) uniform PurxiyChromaPreview {
    // x, y = target size (px), z = blocks per pixel, w = time (s)
    vec4 PvScreen;
    // the part of the landscape the stage shows (scene uv, y down): u0, v0, u1, v1
    vec4 PvView;
    // x, y = the landscape's size (blocks), z = how much colour the grey keeps
    vec4 PvScene;
    vec4 PvShadow;
    vec4 PvMid;
    vec4 PvLight;
    // x = blooms
    ivec4 PvCount;
    // per bloom: (x, y (blocks, y down), radius now, strength), (wet, 0, 0, 0)
    vec4 PvBlooms[160];
};

uniform sampler2D SceneSampler;

out vec4 fragColor;

// The landscape is small: its watercolour is this much finer than the world's, noise and feather alike.
const float FINE = 2.0;

// The block textures (premultiplied), crisp at any size: nearest within a texel, a pixel of blending at its border.
vec4 pixelArt(vec2 uv) {
    vec2 size = PvScene.xy * 16.0;
    vec2 p = uv * size;
    vec2 whole = floor(p);
    vec2 pixelsPerTexel = PvScreen.xy / ((PvView.zw - PvView.xy) * size);
    vec2 f = clamp((p - whole - 0.5) * pixelsPerTexel + 0.5, 0.0, 1.0);
    return texture(SceneSampler, (whole + f) / size);
}

void main() {
    vec2 f = gl_FragCoord.xy / PvScreen.xy;
    vec2 uv = vec2(mix(PvView.x, PvView.z, f.x), mix(PvView.w, PvView.y, f.y));
    vec2 b = uv * PvScene.xy;
    vec4 tile = pixelArt(uv);
    vec3 sky = mix(vec3(0.53, 0.71, 1.0), vec3(0.81, 0.89, 1.0), clamp(uv.y * 1.2, 0.0, 1.0));
    vec3 scene = sky * (1.0 - tile.a) + tile.rgb;

    float paint = 0.0;
    float wet = 0.0;
    for (int i = 0; i < PvCount.x; i++) {
        vec4 bloom = PvBlooms[2 * i];
        // as in the world, colour climbs trees and walls above further than it sinks into the ground
        vec2 off = b - bloom.xy;
        off.y *= off.y < 0.0 ? 0.45 : 1.0;
        float p = bloom.z > 0.0 ? clamp((bloom.z - length(off)) * FINE / CHROMA_FEATHER + 0.5, 0.0, 1.0) * bloom.w : 0.0;
        paint = max(paint, p);
        wet = max(wet, p * PvBlooms[2 * i + 1].x);
    }
    vec2 n = b * FINE;
    vec2 whole = floor(n);
    float noise = chromaWatercolour(ivec3(ivec2(whole), 0), vec3(n - whole, 0.5), PvScreen.z * FINE);
    float t = chromaEdge(paint, noise);
    float aa = clamp(fwidth(t) * 0.8, 0.004, 0.25);
    vec3 grey = mix(chromaTone(scene, PvShadow.rgb, PvMid.rgb, PvLight.rgb), scene, PvScene.z);
    fragColor = vec4(chromaPaint(scene, grey, t, aa, wet), 1.0);
}
