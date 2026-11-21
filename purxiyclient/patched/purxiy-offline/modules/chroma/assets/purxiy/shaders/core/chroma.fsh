#version 330

#moj_import <purxiy:chroma.glsl>
#moj_import <purxiy:chroma_common.glsl>

// Chroma (see dev.purxiy.chroma.Chroma): the picture in the grey of its tone, except where it has been painted. For
// every pixel the surface is found again from the scene's depth; its column says how much colour it has (the paint, a
// texture of the block columns around the camera that wraps around), and so do the blooms that are still growing. The
// edge between colour and grey is moved by noise fixed in the world, like watercolour on paper.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D PaintSampler;
#ifdef LIVING
uniform sampler2D TerrainDepthSampler;
uniform sampler2D OpaqueDepthSampler;
#endif
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

// keep in sync with PaintMap
const int SIZE = 512;
const int PERIOD = 65534;
const int FOREVER = 65535;

vec3 scenePoint(ivec2 px, float depth) {
    vec2 ndc = (vec2(px) + 0.5) / ChScreen.xy * 2.0 - 1.0;
    vec4 p = ChInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

// The colour a column (relative to the camera's) holds for a surface at height y: its paint, as its time allows.
float column(ivec2 col, float y) {
    ivec2 inside = col - ChWindow.zw;
    if (inside.x < 0 || inside.y < 0 || inside.x >= SIZE || inside.y >= SIZE) {
        return 0.0;
    }
    uvec4 t = uvec4(texelFetch(PaintSampler, (ChWindow.xy + col) & (SIZE - 1), 0) * 255.0 + 0.5);
    int stored = int((t.r << 8u) | t.g);
    if (stored == 0) {
        return 0.0;
    }
    float time = 1.0;
    if (stored != FOREVER) {
        int left = (stored - 1 - ChPaint.x) % PERIOD;
        left = left < 0 ? left + PERIOD : left;
        left = left > PERIOD / 2 ? left - PERIOD : left;
        time = clamp(float(left) / float(ChPaint.y), 0.0, 1.0);
    }
    float height = float(t.b) * 1.5 - 64.0;
    return float(t.a) / 255.0 * time * chromaBand(y - height);
}

// The settled paint under a point (camera-block-relative xz): the four columns around it, blended.
float settled(vec2 xz, float y) {
    vec2 p = xz - 0.5;
    vec2 base = floor(p);
    vec2 f = p - base;
    ivec2 c = ivec2(base);
    float a = column(c, y);
    float b = column(c + ivec2(1, 0), y);
    float d = column(c + ivec2(0, 1), y);
    float e = column(c + ivec2(1, 1), y);
    return mix(mix(a, b, f.x), mix(d, e, f.x), f.y);
}

void main() {
    ivec2 px = ivec2(gl_FragCoord.xy);
    vec3 scene = texelFetch(SceneSampler, px, 0).rgb;
    float depth = texelFetch(DepthSampler, px, 0).r;
    bool sky = depth >= 1.0;
    vec3 rel = scenePoint(px, sky ? 0.99999 : depth);
    float dist = length(rel);
    vec3 world = ChCamera.xyz + rel;
    vec3 whole = floor(world);
    ivec3 block = ChBlock.xyz + ivec3(whole);
    vec3 inBlock = world - whole;
    float y = ChCamera.w + rel.y;

    float paint = 0.0;
    float wet = 0.0;
    if (!sky) {
        paint = settled(world.xz, y);
        for (int i = 0; i < ChPaint.z; i++) {
            vec4 bloom = ChBlooms[2 * i];
            float p = bloom.w > 0.0 ? chromaStrength(bloom.w, length(rel.xz - bloom.xz)) * chromaBand(rel.y - bloom.y) : 0.0;
            paint = max(paint, p);
            wet = max(wet, p * ChBlooms[2 * i + 1].x);
        }
        // switching on and off: colour everywhere within the flood
        paint = max(paint, clamp((ChFlood.x - dist) / 10.0, 0.0, 1.0));
    }

    float noise = 0.5;
    if (paint > 0.002 && paint < 0.998) {
        noise = chromaWatercolour(block, inBlock, dist * ChScreen.z);
    }
    float t = chromaEdge(paint, noise);
    float aa = clamp(fwidth(t) * 0.8, 0.004, 0.25);

    vec3 grey = mix(chromaTone(scene, ChShadow.rgb, ChMid.rgb, ChLight.rgb), scene, ChShadow.a);
    vec3 result = sky ? mix(grey, scene, ChLight.a) : chromaPaint(scene, grey, t, aa, wet);

    float keep = 0.0;
#ifdef LIVING
    if (!sky && ChFlood.y > 0.5) {
        // something stood before the terrain once the entities were drawn, and nothing translucent was drawn over it
        float terrain = texelFetch(TerrainDepthSampler, px, 0).r;
        float opaque = texelFetch(OpaqueDepthSampler, px, 0).r;
        if (opaque < terrain) {
            float far = length(scenePoint(px, min(terrain, 0.99999)));
            float near = length(scenePoint(px, opaque));
            if (far - near > 0.02 + 0.002 * far && dist > near - 0.03 - 0.002 * near) {
                keep = 1.0;
            }
        }
    }
#endif
#ifdef MASKED
    keep = max(keep, step(0.5, texelFetch(HandMaskSampler, px, 0).r));
#endif
    result = mix(result, scene, keep);

    if (ChPaint.w == 1) {
        result = vec3(paint, wet, keep);
    } else if (ChPaint.w == 2) {
        result = vec3(smoothstep(0.5 - aa, 0.5 + aa, t));
    }
    fragColor = vec4(mix(scene, result, ChMid.a), 1.0);
}
