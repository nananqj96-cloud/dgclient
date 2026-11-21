#version 330

// Per-frame data of the Purxiy mist (see MistRenderer#uniforms). Distances are in blocks.
layout(std140) uniform PurxiyMist {
    // camera-relative world position from screen position and depth
    mat4 MistInverse;
    // x, y = full size (px), z, w = march grid size (texels)
    vec4 MistScreen;
    // xz = camera position modulo the terrain map's size, y = camera height, w = the terrain map's base height
    vec4 MistCamera;
    // xyz = camera position in the drifting noise space (wrapped), w = how far the wisps have risen (noise slices, wrapped)
    vec4 MistDrift;
    // x = extinction at the ground (per block), y = 1 / height of the mist, z = strength of the thin veil above, w = 1 / its height
    vec4 MistShape;
    // x = clear radius, y = how far the mist is marched for the sky, z = mean ground near you (for columns not known), w = presence
    vec4 MistReach;
    // x, y = projection near and far, z = 1 with a shader pack, w = debug view
    vec4 MistDepth;
    // rgb = light of the open sky on the mist, w = how much shade under the canopy darkens it
    vec4 MistAmbient;
    // rgb = sun (or moon) light on the mist, w = how strongly it glows towards the light
    vec4 MistSun;
    // xyz = direction towards the sun (or moon), w = 1 while sunlight beams are drawn
    vec4 MistSunDir;
    // rgb = lantern light (strength included), w = unused
    vec4 MistLantern;
    // x = the quarter of the march grid (tiles of 8 x 8 texels) marched afresh this frame, the rest taken over from the last
    // frame's march where it still holds (0..3; -1: all of it)
    vec4 MistRefresh;
    // x = how much less players and mobs are hidden (0..1), y = 1 while a creature mask is bound, z, w = the halo's and the wide glow's strength
    vec4 MistCreatures;
    // the last frame's camera-relative position to clip space
    mat4 MistKeptView;
    // xyz = how far the camera moved since the last frame, w = unused
    vec4 MistMoved;
};

const float MIST_SIZE = 512.0;
const float MIST_PI = 3.14159265;

vec3 mistPosition(vec2 uv, float depth) {
    vec4 p = MistInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// Distance along the view (blocks) of a depth value.
float mistLinear(float depth) {
    float n = MistDepth.x, f = MistDepth.y;
    float z = depth * 2.0 - 1.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

// Interleaved gradient noise: a fine, even pattern for per-pixel offsets.
float mistDither(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}
