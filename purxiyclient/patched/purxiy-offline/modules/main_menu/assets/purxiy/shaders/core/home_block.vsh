#version 330

#moj_import <purxiy:home.glsl>

// How this mesh grows (see HomeRenderer): x = seconds since it began growing (or shrinking), y = 1 growing, -1
// shrinking, 0 standing, z = the latest birth in the mesh (shrinking runs the growth backwards).
layout(std140) uniform HomeDraw {
    vec4 Growth;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;  // rgb = tint, shade and occlusion; a = what kind of thing this is (HomeMesh)
in vec4 Grow;   // xyz = the point it grows and sways from, w = when it is born (s)

out vec2 vUv;
out vec3 vColor;
out vec3 vWorld;
out float vFog;
flat out int vKind;

const int LOG = 254, LEAF = 253, SAPLING = 252, PLANT = 251, DECOR = 250, PLANT_GROW = 249, GLOW = 248;

float backOut(float t) {
    float u = t - 1.0;
    return 1.0 + 2.70158 * u * u * u + 1.70158 * u * u;
}

float easeOut(float t) {
    float u = 1.0 - t;
    return 1.0 - u * u * u;
}

void main() {
    int kind = int(Color.a * 255.0 + 0.5);
    vec3 p = Position;
    vec3 pivot = Grow.xyz;
    float t = Camera.w;
#ifdef CLOUDS
    p.x -= CamRight.w;
#else
    if (kind == LOG || kind == LEAF || kind == SAPLING || kind == DECOR || kind == PLANT_GROW) {
        float age = Growth.x - Grow.w;
        vec3 scale = vec3(1.0);
        if (Growth.y > 0.5) {
            if (kind == SAPLING) {
                // the sapling pops up, and makes way as its trunk comes
                scale = vec3(backOut(clamp(age / 0.22, 0.0, 1.0)) * (1.0 - smoothstep(0.42, 0.62, age)));
            } else if (kind == LOG) {
                // wood rises out of the block below, filling out as it goes
                float e = easeOut(clamp(age / 0.2, 0.0, 1.0));
                scale = age <= 0.0 ? vec3(0.0) : vec3(mix(0.45, 1.0, e), e, mix(0.45, 1.0, e));
            } else {
                // leaves and flowers pop out with a little overshoot
                scale = vec3(backOut(clamp(age / 0.32, 0.0, 1.0)));
            }
        } else if (Growth.y < -0.5) {
            // the old forest folds away: the leaves first, then the wood from the top down
            float order = (Growth.z - Grow.w) * 0.3;
            float k = clamp((Growth.x - order) / 0.2, 0.0, 1.0);
            scale = vec3(kind == SAPLING ? 0.0 : 1.0 - k * k);
        } else if (kind == SAPLING) {
            scale = vec3(0.0);
        }
        p = pivot + (p - pivot) * scale;
    }
    // the wind: leaves stir, plants bend from their roots
    float wind = Light.w;
    float phase = dot(pivot, vec3(0.37, 0.21, 0.53));
    if (kind == LEAF) {
        p += vec3(sin(t * 1.15 + phase), sin(t * 0.8 + phase * 2.1) * 0.4, cos(t * 0.95 + phase * 1.3)) * 0.026 * wind;
    } else if (kind == PLANT || kind == PLANT_GROW || kind == GLOW) {
        float h = max(p.y - pivot.y, 0.0);
        p.xz += vec2(sin(t * 1.7 + phase), cos(t * 1.3 + phase * 1.7)) * 0.07 * h * wind;
    }
#endif
    gl_Position = ViewProj * vec4(p, 1.0);
    vUv = UV0;
    vColor = Color.rgb;
    vWorld = p;
    vKind = kind;
    float dist = length(p - Camera.xyz);
#ifdef CLOUDS
    // clouds stay a band towards the horizon; straight overhead they would close the sky like a ceiling
    float rise = (p.y - Camera.y) / max(dist, 0.001);
    vFog = max(smoothstep(150.0, 300.0, dist), smoothstep(0.2, 0.42, rise));
#else
    vFog = smoothstep(FogRange.x, FogRange.y, dist);
#endif
}
