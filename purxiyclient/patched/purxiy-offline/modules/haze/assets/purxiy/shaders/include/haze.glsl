#version 330

// Per-frame data of the Purxiy haze (see HazeRenderer#uniforms) and the haze itself along a view ray, shared by the march
// (on a grid of half the screen's size) and the composite (which marches again, with fewer steps, for pixels the grid does
// not see). Distances are in blocks; colours are as the picture holds them (0..1), already in the light of the hour.
layout(std140) uniform PurxiyHaze {
    // camera-relative world position from screen position and depth
    mat4 HazeInverse;
    // x, y = size (px), z = seconds (wrapped), w = debug view
    vec4 HazeScreen;
    // x, z = the camera's place in the drifting colour (wrapped), y = camera height, w = the height of the ground the haze lies on
    vec4 HazeCamera;
    // x = extinction low over the ground (per block), y = 1 / height of the haze, z = strength of the thin veil above it, w = 1 / its height
    vec4 HazeShape;
    // x = how far around you the air is clear, y = over how many blocks beyond that the haze comes in, z = presence, w = how far
    // out the haze has turned to its far colour
    vec4 HazeReach;
    // x, y = projection near and far, z = how far the land can be seen (the render distance, blocks), w = how much of the sky is
    // the Purxiy sky (0..1)
    vec4 HazeDepth;
    // rgb = the haze close by and low over the land, w = unused
    vec4 HazeLow;
    // rgb = the haze far away and higher up, w = unused
    vec4 HazeHigh;
    // rgb = the pockets of colour drifting through it, w = how much they show
    vec4 HazePocket;
    // rgb = its glow towards the sun (or moon), strength included, w = how much of it is the tight halo around the sun
    vec4 HazeSun;
    // xyz = direction towards the sun (or moon), w = unused
    vec4 HazeSunDir;
    // x = how much less players and mobs are hidden (0..1), y = 1 while the depth copies are bound
    vec4 HazeCreatures;
};

uniform sampler2D NoiseSampler;

vec3 hazePosition(vec2 uv, float depth) {
    vec4 p = HazeInverse * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// Distance along the view (blocks) of a depth value.
float hazeLinear(float depth) {
    float n = HazeDepth.x, f = HazeDepth.y;
    float z = depth * 2.0 - 1.0;
    return 2.0 * n * f / (f + n - z * (f - n));
}

// How far the haze is marched along a view ray: to what the pixel shows, or for the sky as far as the land can be seen in
// that direction (where the world's own fog ends), so land and sky meet in one haze.
float hazeEnd(vec3 dir, float distance, bool sky) {
    return sky ? HazeDepth.z / max(max(length(dir.xz), abs(dir.y)), 0.02) : distance;
}

// Interleaved gradient noise: a fine, even pattern for per-pixel offsets.
float hazeDither(vec2 pixel) {
    return fract(52.9829189 * fract(dot(pixel, vec2(0.06711056, 0.00583715))));
}

// Four smooth value noises at once (one per channel), 256 texels across: one read between texels moved to a smoothstep.
vec4 hazeNoise(vec2 x) {
    vec2 p = floor(x);
    vec2 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return textureLod(NoiseSampler, (p + f + 0.5) / 256.0, 0.0);
}

// The mean of exp(-k * max(h, 0)) while h goes evenly from a to b, given e(a) and e(b) (that exp at both ends, which the steps
// of a ray share): how thick a layer that thins with height is, on average, along one step (exact, so a long step far away
// gives what many short ones would).
float hazeBand(float a, float b, float ea, float eb, float k) {
    float span = abs(b - a);
    if (span < 1.0e-3) {
        return ea;
    }
    float lo = min(a, b), hi = max(a, b);
    float below = clamp(-lo, 0.0, span);
    float above = hi > 0.0 ? abs(ea - eb) / k : 0.0;
    return (below + above) / span;
}

// Henyey-Greenstein, 1 straight towards the light
float hazePhase(float c, float g) {
    float g2 = g * g;
    float x = (1.0 + g2 - 2.0 * g) / max(1.0 + g2 - 2.0 * g * c, 1.0e-4);
    return x * sqrt(x);
}

// The haze along a view ray from the camera out to `end` blocks, in `steps` steps: the colour it adds (rgb, as light: the
// mean colour times how much it hides) and how much of what lies behind it still shows through (a). Steps grow with
// distance (end * u^2): short near you, where the clear air gives way to the colour, long far out, where the haze changes
// slowly. Around you the air is clear out to c blocks; beyond, the haze comes in gently over l (the mean of
// 1 - exp(-(t - c) / l) over each step), so what is near keeps its colours and the distance takes on the haze's.
vec4 hazeMarch(vec3 dir, float end, int steps) {
    float c = HazeReach.x, l = max(HazeReach.y, 1.0);
    float k = HazeShape.y, kv = HazeShape.w;
    float transmit = 1.0;
    vec3 light = vec3(0.0);
    // at the start of the step: distance, height above the ground, and the exps the steps share
    float t0 = 0.0;
    float h0 = HazeCamera.y - HazeCamera.w;
    float layer0 = exp(-k * max(h0, 0.0)), veil0 = exp(-kv * max(h0, 0.0));
    float in0 = 1.0;
    for (int i = 1; i <= steps; i++) {
        float u = float(i) / float(steps);
        float t1 = end * u * u;
        float dt = t1 - t0;
        float h1 = h0 + dir.y * dt;
        float layer1 = exp(-k * max(h1, 0.0)), veil1 = exp(-kv * max(h1, 0.0));
        float in1 = exp(-max(t1 - c, 0.0) / l);
        float layer = hazeBand(h0, h1, layer0, layer1, k);
        float veil = HazeShape.z * hazeBand(h0, h1, veil0, veil1, kv);
        float near = t1 <= c || dt < 1.0e-3 ? 0.0 : clamp(((t1 - max(t0, c)) - l * (in0 - in1)) / dt, 0.0, 1.0);
        float tm = 0.5 * (t0 + t1), hm = 0.5 * (h0 + h1);
        t0 = t1;
        h0 = h1;
        layer0 = layer1;
        veil0 = veil1;
        in0 = in1;
        if ((layer + veil) * near < 1.0e-5) {
            continue;
        }

        // soft banks, a little thicker and thinner, and the pockets of the third colour, drifting with the wind; a step
        // longer than they are sees their mean
        vec2 q = HazeCamera.xz + dir.xz * tm;
        float detail = 1.0 - smoothstep(60.0, 150.0, dt);
        vec4 n = detail > 0.0 ? hazeNoise(q * (1.0 / 128.0)) : vec4(0.5);
        float banks = mix(0.5, n.r, detail);
        float pockets = mix(0.5, n.b, detail);
        float sigma = HazeShape.x * HazeReach.z * (layer * (0.7 + 0.6 * banks) + veil) * near;

        // close by and low it has its own colour; far away and higher up it turns to the far one; the pockets drift through
        float far = smoothstep(0.0, HazeReach.w, tm);
        float up = smoothstep(0.0, 2.5 / k, hm);
        vec3 colour = mix(HazeLow.rgb, HazeHigh.rgb, clamp(0.85 * far + 0.42 * up, 0.0, 1.0));
        colour = mix(colour, HazePocket.rgb, smoothstep(0.45, 0.8, pockets) * HazePocket.w);

        float s = 1.0 - exp(-sigma * dt);
        light += transmit * s * colour;
        transmit *= 1.0 - s;
    }
    return vec4(light, transmit);
}
