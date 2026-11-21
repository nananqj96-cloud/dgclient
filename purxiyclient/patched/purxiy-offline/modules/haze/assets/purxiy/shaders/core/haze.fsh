#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:haze.glsl>

// The haze laid over the picture, at full size (see dev.purxiy.haze.HazeRenderer). Each pixel takes the march of the grid
// texels around it whose distance matches its own (so a near edge keeps clear of the haze behind it); a pixel the grid does
// not see at all (a fence post or a leaf in front of the far distance) marches its own ray, with fewer steps. The haze is
// mixed in as light mixes, so the world takes on its colour the further away it is, and it glows towards the sun. Where
// the world's fog melts the land into the Purxiy sky it gives way to that fog, and the Purxiy sky itself stays exactly the
// sky you chose. Players and mobs can keep their own colours, and with a shader pack a mask keeps your hand as it is.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D HazeSampler;
#ifdef CREATURES
uniform sampler2D TerrainDepthSampler;
uniform sampler2D OpaqueDepthSampler;
#endif
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    vec3 scene = texelFetch(SceneSampler, p, 0).rgb;
#ifdef MASKED
    if (texelFetch(HandMaskSampler, p, 0).r > 0.5) {
        fragColor = vec4(scene, 1.0);
        return;
    }
#endif
    float depth = texelFetch(DepthSampler, p, 0).r;
    bool sky = depth >= 0.999999;
    vec2 uv = (vec2(p) + 0.5) / HazeScreen.xy;
    vec3 rel = hazePosition(uv, sky ? 0.9999 : depth);
    float distance = length(rel);
    vec3 dir = rel / max(distance, 1.0e-4);
    float end = hazeEnd(dir, distance, sky);

    float keep = 1.0;
    if (sky) {
        // the sky keeps most of its own colour overhead: the haze shows on it towards the horizon
        keep = (1.0 - HazeDepth.w) * (1.0 - 0.65 * smoothstep(0.04, 0.55, dir.y));
    } else if (HazeDepth.w > 0.0) {
        // where the world's own fog melts the land into the Purxiy sky, the haze gives way to it
        float fog = total_fog_value(fog_spherical_distance(rel), fog_cylindrical_distance(rel), FogEnvironmentalStart, FogEnvironmentalEnd,
            FogRenderDistanceStart, FogRenderDistanceEnd);
        keep = 1.0 - fog * HazeDepth.w;
    }
    int debug = int(HazeScreen.w + 0.5);
    if ((keep <= 0.001 || end <= HazeReach.x) && debug == 0) {
        fragColor = vec4(scene, 1.0);
        return;
    }

    // the grid texels around: texel q was marched for pixel 2q; each is weighed by how well its distance matches this pixel's
    vec2 h = vec2(p) * 0.5;
    ivec2 b = ivec2(floor(h));
    vec2 f = h - vec2(b);
    ivec2 grid = ivec2((HazeScreen.xy + 1.0) * 0.5);
    ivec2 last = ivec2(HazeScreen.xy) - 1;
    float linear = hazeLinear(depth);
    vec4 sum = vec4(0.0);
    float total = 0.0;
    float best = 1.0e9;
    for (int j = 0; j <= 1; j++) {
        for (int i = 0; i <= 1; i++) {
            float w = (i == 0 ? 1.0 - f.x : f.x) * (j == 0 ? 1.0 - f.y : f.y);
            if (w <= 0.0) {
                continue;
            }
            ivec2 q = min(b + ivec2(i, j), grid - 1);
            float qDepth = texelFetch(DepthSampler, min(q * 2, last), 0).r;
            float off = sky ? (qDepth >= 0.999999 ? 0.0 : 1.0) : abs(hazeLinear(qDepth) - linear) / max(linear, 0.5);
            best = min(best, off);
            w *= exp(-off * off * 600.0);
            sum += texelFetch(HazeSampler, q, 0) * w;
            total += w;
        }
    }
    vec3 own;
    float hidden;
    bool alone = best > 0.08 || total < 1.0e-3;
    if (alone) {
        vec4 haze = hazeMarch(dir, end, 4);
        hidden = 1.0 - haze.a;
        own = haze.rgb / max(hidden, 1.0e-4);
    } else {
        vec4 haze = sum / total;
        own = haze.rgb;
        hidden = haze.a;
    }

    // the glow towards the sun or moon: a tight halo around it and a soft, wide light, in what the haze hides
    float c = dot(dir, HazeSunDir.xyz);
    vec3 glow = HazeSun.rgb * (HazeSun.w * hazePhase(c, 0.82) + (1.0 - HazeSun.w) * hazePhase(c, 0.35)) * hidden;

#ifdef CREATURES
    // players and mobs: in front of the terrain, with nothing translucent in front of them, they keep more of their colours
    if (!sky && HazeCreatures.y > 0.5) {
        float terrain = texelFetch(TerrainDepthSampler, p, 0).r;
        float opaque = texelFetch(OpaqueDepthSampler, p, 0).r;
        if (abs(depth - opaque) < 1.0e-7 && opaque < terrain - 1.0e-6) {
            keep *= 1.0 - 0.8 * HazeCreatures.x;
        }
    }
#endif

    if (debug == 1) {
        fragColor = vec4(vec3(hidden), 1.0);
        return;
    } else if (debug == 2) {
        fragColor = vec4(own, 1.0);
        return;
    } else if (debug == 3) {
        fragColor = vec4(vec3(keep), 1.0);
        return;
    } else if (debug == 4) {
        fragColor = vec4(alone ? 1.0 : 0.0, hidden, 0.0, 1.0);
        return;
    } else if (debug == 5) {
        fragColor = vec4(glow, 1.0);
        return;
    }
    // mixed as light mixes (squared, near enough to light, not in the picture's values): the haze's colour lifts the shade of
    // what lies behind it first, so the world takes on the colour as coloured air does and does not turn grey or muddy
    vec3 hazed = sqrt(scene * scene * (1.0 - hidden) + own * own * hidden) + glow;
    // a little noise against banding, where there is haze (the picture holds 8 bits per channel)
    float grain = (hazeDither(gl_FragCoord.xy) - 0.5) / 255.0 * step(0.002, hidden);
    vec3 result = mix(scene, hazed, keep) + grain;
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
