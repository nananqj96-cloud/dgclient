#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:hit_splash.glsl>

// The ink of Hit Splash (see dev.purxiy.hitsplash.SplashRenderer) on one cluster's rectangle, as a deferred decal: the
// scene's surface under this pixel comes from the depth, and where it faces up and lies in the cluster's box, the splats
// of the cluster are added up on it:
//
//   ink    their distance fields (from the shape atlas) joined with a smooth minimum, so splats that touch run together
//          like a liquid: deep violet at the thin edges, lighter where it is thick, white-hot where heat piles up
//   glow   light hugging the ink, from each splat's own distance
//   heat   a broad warmth around every splat (a Gaussian), adding up where hits land on the same spot
//
// Anti-aliasing and the atlas's level of detail follow the size of a pixel on the ground here, measured between the
// scene points of neighbouring pixels.

uniform sampler2D DepthSampler;
uniform sampler2D ShapeSampler;

flat in int cluster;

out vec4 fragColor;

// keep in sync with SplatShapes and SplashRenderer
const float TILE = 256.0;
const float UNIT = 54.0;
const float FINE = 12.0;
const float COARSE = 64.0;
const vec2 ATLAS = vec2(1024.0, 512.0);
const float HALF = 2.34;
const float BAND = 0.45;

vec3 scenePoint(ivec2 px) {
    float depth = texelFetch(DepthSampler, px, 0).r;
    vec2 ndc = (vec2(px) + 0.5) / SplashScreen.xy * 2.0 - 1.0;
    vec4 p = SplashInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    if (texelFetch(DepthSampler, px, 0).r >= 1.0) {
        discard;
    }
    vec3 p = scenePoint(px);
    vec4 lo = SplashClusters[cluster * 3];
    vec4 hi = SplashClusters[cluster * 3 + 1];
    if (any(lessThan(p, lo.xyz)) || any(greaterThan(p, hi.xyz))) {
        discard;
    }

    // the surface, from the neighbour on the same surface on each side: ink only lies on ground that faces up
    ivec2 edge = ivec2(SplashScreen.xy) - 1;
    vec3 right = scenePoint(min(px + ivec2(1, 0), edge));
    vec3 left = scenePoint(max(px - ivec2(1, 0), ivec2(0)));
    vec3 above = scenePoint(min(px + ivec2(0, 1), edge));
    vec3 below = scenePoint(max(px - ivec2(0, 1), ivec2(0)));
    vec3 dx = distance(right, p) < distance(p, left) ? right - p : p - left;
    vec3 dy = distance(above, p) < distance(p, below) ? above - p : p - below;
    vec3 normal = normalize(cross(dx, dy));
    if (dot(normal, p) > 0.0) {
        normal = -normal;
    }
    float facing = smoothstep(0.55, 0.8, normal.y);
    if (facing <= 0.0) {
        discard;
    }
    // blocks per pixel on the ground here
    float pixel = sqrt(max(length(dx) * length(dy), 1.0e-12));

    int first = int(lo.w + 0.5);
    int end = first + int(hi.w + 0.5);
    float ink = 1.0e3;
    float nearest = 1.0e3;
    float span = 0.1;
    float grain = 0.5;
    float flash = 0.0;
    float opacity = 0.0;
    float glow = 0.0;
    float heat = 0.0;
    for (int i = first; i < end; i++) {
        vec4 a = SplashSplats[i * 3];
        float rise = abs(p.y - a.y);
        if (rise > BAND) {
            continue;
        }
        vec4 b = SplashSplats[i * 3 + 1];
        vec4 c = SplashSplats[i * 3 + 2];
        float band = 1.0 - smoothstep(BAND * 0.6, BAND, rise);
        float size = a.w;
        vec2 d = p.xz - a.xz;
        float r2 = dot(d, d);
        float sigma = 0.3 + 0.7 * size;
        heat += c.x * band * exp(-r2 / (2.0 * sigma * sigma));
        if (r2 > 2.0 * HALF * HALF * size * size) {
            continue;
        }
        vec2 q = vec2(dot(d, b.xy), dot(d, vec2(-b.y, b.x))) / size;
        int tile = int(b.z + 0.5);
        if (tile >= 8) {
            q.y = -q.y;
            tile -= 8;
        }
        if (max(abs(q.x), abs(q.y)) > HALF) {
            continue;
        }
        vec2 uv = (vec2(float(tile % 4), float(tile / 4)) * TILE + TILE * 0.5 + q * UNIT) / ATLAS;
        vec4 s = textureLod(ShapeSampler, uv, log2(max(pixel * UNIT / size, 1.0e-3)));
        // shape units: the fine channel near the edge, the coarse one further out
        float coarse = (0.5 - s.g) * 2.0 * COARSE / UNIT;
        float dist = abs(coarse) < 0.9 * FINE / UNIT ? (0.5 - s.r) * 2.0 * FINE / UNIT : coarse;
        // it spreads from the middle out, and wears away unevenly at the end
        dist = max(dist, length(q) - b.w);
        dist += c.y * (0.55 + 0.9 * s.b);
        dist += (1.0 - band) * 0.5;
        float blocks = dist * size;
        ink = smin(ink, blocks, 0.03);
        float outside = max(dist, 0.0);
        glow += c.x * band * exp(-outside * size / min(0.045, 0.35 * size)) * (1.0 - smoothstep(0.18, 0.36, outside));
        if (blocks < nearest) {
            nearest = blocks;
            span = size;
            grain = s.b;
            flash = c.z;
            opacity = c.w;
        }
    }
    if (ink > 0.2 && glow < 0.002 && heat < 0.01) {
        discard;
    }

    float lit = 1.0 - exp(-heat * 0.9);
    float hot = smoothstep(1.2, 4.0, heat);
    // hot pools breathe, softly
    float breathe = SplashSecond.w > 0.5 ? 1.0 : 1.0 + 0.06 * sin(SplashFirst.w * 2.4) * hot;

    // the ink, anti-aliased over a pixel of ground
    float cover = clamp(0.5 - ink / pixel, 0.0, 1.0) * facing;
    // deep at the rim, lighter inside: over a band as wide as the splat is big
    float thick = clamp(-ink / (0.16 * span + 0.004), 0.0, 1.0);
    vec3 deep = pow(SplashSecond.rgb, vec3(2.0));
    vec3 color = mix(deep, SplashSecond.rgb, smoothstep(0.0, 0.35, thick));
    color = mix(color, SplashFirst.rgb, smoothstep(0.3, 1.0, thick));
    vec3 core = mix(mix(SplashFirst.rgb, SplashAccent.rgb, 0.3), vec3(1.0, 0.98, 1.0), 0.6);
    color = mix(color, core, hot * smoothstep(0.2, 1.0, thick) * 0.9);
    color = mix(color, SplashAccent.rgb, 0.25 * (grain - 0.5));
    color *= (0.92 + 0.12 * lit) * breathe;
    color = mix(color, vec3(1.0), flash * 0.7);
    float alpha = cover * 0.93 * opacity;
    vec4 result = vec4(min(color, vec3(1.0)) * alpha, alpha);

    // light around it: hugging the ink, and a broad warmth where the heat piles up; it mostly adds, so it glows on dark
    // ground and tints bright ground
    float tight = (1.0 - exp(-glow)) * (1.0 - cover);
    float broad = smoothstep(1.0, 4.0, heat) * 0.55;
    float g = SplashScreen.w * (0.6 * tight * (0.45 + 0.55 * lit) + 0.4 * broad) * facing * breathe;
    vec3 light = mix(pow(SplashSecond.rgb, vec3(1.5)), SplashFirst.rgb, 0.15 + 0.3 * hot);
    result += vec4(light * g, g * 0.2) * (1.0 - result.a);

    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(p), fog_cylindrical_distance(p), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
