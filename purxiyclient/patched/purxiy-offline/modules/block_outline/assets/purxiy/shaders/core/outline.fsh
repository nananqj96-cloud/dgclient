#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:outline.glsl>

// The Purxiy block outline (see dev.purxiy.outline.PurxiyOutline), in one pass over the outline's rectangle on
// screen. Every edge is a distance field in pixels, so lines are anti-aliased at any width, joints are seamless and
// the glow is even (overlapping edges take the strongest, they never add up). The faces are found by casting this
// pixel's ray at the shape's boxes. Both are tested against the scene's depth, which is sampled rather than attached,
// so the glow of a visible edge spills over the block like light does.
//
//   lines     core and glow, coloured along a gradient; the shimmer's band, the aim light and a placed block's flash
//             light them up
//   mining    arms of light grow from every corner and close in the middle of each edge as the block breaks
//   faces     an optional tint, and the aim light: a soft glow where the crosshair hits
//   hidden    edges behind the block, dashed like a blueprint

uniform sampler2D DepthSampler;

out vec4 fragColor;

const vec3 WHITE = vec3(1.0);

// ---------------------------------------------------------------------------------------------------------- edges

// Screen parameter (0..1 along the projected edge) of the point at 3D parameter t, and back: perspective-correct.
float toScreen(float t, float w0, float w1) {
    return t * w1 / mix(w0, w1, t);
}

float toWorld(float s, float w0, float w1) {
    return s * w0 / mix(w1, w0, s);
}

// Distance from p to the drawn parts of an edge, [0, sa] and [sb, 1] (all of it when sa >= sb); s receives the
// parameter of the closest point.
float partDistance(vec2 p, vec2 a, vec2 ab, float sRaw, float sa, float sb, out float s) {
    float s1 = clamp(sRaw, 0.0, sa);
    float s2 = clamp(sRaw, sb, 1.0);
    float d1 = length(p - a - ab * s1);
    float d2 = length(p - a - ab * s2);
    s = d1 <= d2 ? s1 : s2;
    return min(d1, d2);
}

// Coverage of a line of half width hw at distance d; lines thinner than a pixel fade instead of breaking up.
float lineCoverage(float d, float hw) {
    return clamp(max(hw, 0.5) + 0.5 - d, 0.0, 1.0) * min(hw * 2.0, 1.0);
}

// Light of the glow at distance d: a soft core with a longer tail, gone at the reach.
float glowFalloff(float d, float hw, float reach) {
    float x = max(d - hw, 0.0) / reach;
    return (0.55 * exp(-x * x * 6.0) + 0.45 * exp(-x * 4.0)) * (1.0 - smoothstep(0.65, 1.0, x));
}

// Camera-relative point on edge i at screen parameter s.
vec3 edgePoint(int i, float s) {
    vec4 e0 = OutlineSegments[i * 4];
    vec4 e1 = OutlineSegments[i * 4 + 1];
    return mix(OutlineSegments[i * 4 + 2].xyz, OutlineSegments[i * 4 + 3].xyz, toWorld(s, e0.w, e1.w));
}

int edgeInstance(int i) {
    return int(OutlineSegments[i * 4 + 2].w + 0.5);
}

// Whether the scene hides an edge near its point q. Three texels across the edge are tested, each against the edge's
// depth where the texel lies along it: seen at a grazing angle, a face that ends in the edge is nearer than the edge
// by more than the bias a pixel away from it, and one sample would break the line up.
bool edgeVisible(vec2 q, vec2 a, vec2 ab, float len2, float z0, float z1) {
    vec2 across = len2 > 1.0e-6 ? vec2(-ab.y, ab.x) * inversesqrt(len2) : vec2(0.0, 1.0);
    vec2 limit = OutlineScreen.xy - 1.0;
    for (int k = -1; k <= 1; k++) {
        vec2 texel = clamp(floor(q + across * float(k)), vec2(0.0), limit);
        float s = len2 > 1.0e-6 ? clamp(dot(texel + 0.5 - a, ab) / len2, 0.0, 1.0) : 0.0;
        if (mix(z0, z1, s) <= texelFetch(DepthSampler, ivec2(texel), 0).r) {
            return true;
        }
    }
    return false;
}

// ---------------------------------------------------------------------------------------------------------- colour

float gradient(vec3 local) {
    return clamp(local.y * 0.8 + (local.x + local.z) * 0.1, 0.0, 1.0);
}

vec3 localPoint(vec3 point, int inst) {
    vec3 lo = OutlineInstances[inst * 3].xyz;
    return clamp((point - lo) / max(OutlineInstances[inst * 3 + 1].xyz - lo, vec3(1.0e-4)), 0.0, 1.0);
}

// The aim light at squared distance r2 (blocks) from where the crosshair hits: a bright spot in a wide, soft halo.
float aimLight(float r2, float spot, float halo) {
    return 0.65 * exp(-r2 / (2.0 * spot * spot)) + 0.35 * exp(-r2 / (2.0 * halo * halo));
}

// Colour of an instance's lines at a camera-relative point, lit by the shimmer's band, the aim light, the heat of a
// ripple and the flash of a placed block; light receives how much brighter the glow there is.
vec4 paint(vec3 point, int inst, out float light) {
    vec3 local = localPoint(point, inst);
    vec4 color = mix(OutlineColorA, OutlineColorB, gradient(local));

    float band = (dot(local, vec3(0.3, 0.45, 0.25)) - OutlineSheen.x) / OutlineSheen.y;
    float sheen = exp(-band * band) * OutlineFill.w;
    vec3 toAim = point - OutlineAim.xyz;
    float aim = aimLight(dot(toAim, toAim), 0.16, 0.42) * OutlineFill.y * OutlineAim.w;
    float heat = OutlineInstances[inst * 3 + 1].w;
    float flash = OutlineInstances[inst * 3 + 2].y;

    light = sheen * 0.9 + aim * 1.1 + heat * 0.6 + flash;
    color.rgb = mix(color.rgb, WHITE, clamp(sheen * 0.5 + aim * 0.32 + heat * 0.3 + flash * 0.45, 0.0, 0.8));
    return color;
}

// Premultiplied "src over dst".
vec4 over(vec4 dst, vec4 src) {
    return src + dst * (1.0 - src.a);
}

// ---------------------------------------------------------------------------------------------------------- faces

// The front face of the target's shape under this pixel, if the scene does not hide it: tint and aim light.
vec4 faces(vec2 p, float progress, out vec3 hitPoint) {
    hitPoint = vec3(0.0);
    vec2 ndc = p / OutlineScreen.xy * 2.0 - 1.0;
    vec4 nearPoint = OutlineInverse * vec4(ndc, -1.0, 1.0);
    vec4 farPoint = OutlineInverse * vec4(ndc, 1.0, 1.0);
    vec3 ro = nearPoint.xyz / nearPoint.w;
    vec3 rd = farPoint.xyz / farPoint.w - ro;
    rd = sign(rd) * max(abs(rd), vec3(1.0e-8)) + vec3(equal(rd, vec3(0.0))) * 1.0e-8;
    vec3 inv = 1.0 / rd;

    float best = 2.0;
    vec3 normal = vec3(0.0);
    int boxes = int(OutlineCounts.y + 0.5);
    for (int i = 0; i < boxes; i++) {
        vec3 t0 = (OutlineBoxes[i * 2].xyz - ro) * inv;
        vec3 t1 = (OutlineBoxes[i * 2 + 1].xyz - ro) * inv;
        vec3 tn = min(t0, t1);
        vec3 tf = max(t0, t1);
        float enter = max(max(tn.x, tn.y), tn.z);
        float exit = min(min(tf.x, tf.y), tf.z);
        if (enter <= exit && enter > 0.0 && enter < best) {
            best = enter;
            normal = -sign(rd) * vec3(equal(tn, vec3(enter)));
        }
    }
    if (best >= 2.0) {
        return vec4(0.0);
    }
    vec3 hit = ro + rd * best;
    vec4 clip = OutlineViewProj * vec4(hit * OUTLINE_SHRINK, 1.0);
    if (clip.z / clip.w * 0.5 + 0.5 > texelFetch(DepthSampler, ivec2(p), 0).r) {
        return vec4(0.0);
    }
    hitPoint = hit;

    float alpha = OutlineInstances[0].w;
    vec4 color = mix(OutlineColorA, OutlineColorB, gradient(localPoint(hit, 0)));
    // the face you would build on is a touch brighter; a block being broken fills with light
    float aimed = step(0.5, dot(normal, OutlineFace.xyz));
    float mining = progress * OutlineInstances[2].x; // the target's mining arms
    float tint = (OutlineFill.x * (0.3 + 0.12 * aimed) + mining * 0.14) * alpha * color.a;
    vec4 face = vec4(color.rgb * tint, tint);

    vec3 toAim = hit - OutlineAim.xyz;
    float light = aimLight(dot(toAim, toAim), 0.11, 0.28) * OutlineFill.y * OutlineAim.w * alpha * 0.36;
    // light mostly adds up: it glows on dark blocks and only warms bright ones
    return over(face, vec4(mix(color.rgb, WHITE, 0.35) * light, light * 0.45));
}

// ---------------------------------------------------------------------------------------------------------- main

void main() {
    vec2 p = gl_FragCoord.xy;
    float hw = OutlineLine.x;
    float reach = max(OutlineLine.y, 1.0e-3);
    float arm = OutlineLine.w;
    float progress = OutlineFace.w;
    float hidden = OutlineFill.z;
    float limit = hw + OutlineLine.y + 1.0;

    // the strongest contribution of any edge, per layer, and where it comes from
    float core = 0.0, glow = 0.0, lit = 0.0, litGlow = 0.0, dashed = 0.0;
    int coreEdge = -1, glowEdge = -1, litEdge = -1, litGlowEdge = -1, dashedEdge = -1;
    float coreS = 0.0, glowS = 0.0, litS = 0.0, litGlowS = 0.0, dashedS = 0.0;

    int count = int(OutlineCounts.x + 0.5);
    for (int i = 0; i < count; i++) {
        vec4 e0 = OutlineSegments[i * 4];
        vec4 e1 = OutlineSegments[i * 4 + 1];
        vec2 a = e0.xy;
        vec2 ab = e1.xy - a;
        float len2 = dot(ab, ab);
        float sRaw = len2 > 1.0e-6 ? dot(p - a, ab) / len2 : 0.0;
        float sFull = clamp(sRaw, 0.0, 1.0);
        vec2 q = a + ab * sFull;
        if (length(p - q) > limit) {
            continue;
        }
        vec4 e3 = OutlineSegments[i * 4 + 3];
        int inst = edgeInstance(i);
        float alpha = OutlineInstances[inst * 3].w;
        vec4 fx = OutlineInstances[inst * 3 + 2];

        // the drawn part: the whole edge, or an arm at each end (corners)
        float c = arm > 0.0 ? min(0.5, arm / max(e3.w, 1.0e-4)) : 0.5;
        float s;
        float d = partDistance(p, a, ab, sRaw, toScreen(c, e0.w, e1.w), toScreen(1.0 - c, e0.w, e1.w), s);

        // visible where nothing in the scene is in front of the edge's closest point
        if (edgeVisible(q, a, ab, len2, e0.z, e1.z)) {
            // while a block breaks its outline dims, so the arms of light stand out
            float mining = progress * fx.x;
            float strength = alpha * (1.0 - 0.45 * mining);
            float cov = lineCoverage(d, hw) * strength;
            if (cov > core) {
                core = cov;
                coreEdge = i;
                coreS = s;
            }
            float g = glowFalloff(d, hw, reach) * strength;
            if (g > glow) {
                glow = g;
                glowEdge = i;
                glowS = s;
            }
            if (mining > 0.0) {
                float f = mix(arm > 0.0 ? c : 0.0, 0.5, progress);
                float sl;
                float dl = partDistance(p, a, ab, sRaw, toScreen(f, e0.w, e1.w), toScreen(1.0 - f, e0.w, e1.w), sl);
                float grow = alpha * smoothstep(0.0, 0.06, progress);
                float cl = lineCoverage(dl, hw) * grow;
                if (cl > lit) {
                    lit = cl;
                    litEdge = i;
                    litS = sl;
                }
                float gl = glowFalloff(dl, hw, reach) * grow;
                if (gl > litGlow) {
                    litGlow = gl;
                    litGlowEdge = i;
                    litGlowS = sl;
                }
            }
        } else if (hidden > 0.0) {
            // blueprint: dashes fixed to the edge in the world, anti-aliased by their length on screen
            float perPixel = e3.w / OutlineSheen.z * e0.w * e1.w / pow(mix(e1.w, e0.w, s), 2.0) / sqrt(max(len2, 1.0e-6));
            float u = toWorld(s, e0.w, e1.w) * e3.w / OutlineSheen.z;
            float fromCentre = abs(fract(u + 0.25) - 0.5);
            float dash = clamp((0.25 - fromCentre) / max(perPixel, 1.0e-4) + 0.5, 0.0, 1.0);
            float cov = lineCoverage(d, hw * 0.75) * dash * alpha;
            if (cov > dashed) {
                dashed = cov;
                dashedEdge = i;
                dashedS = s;
            }
        }
    }

    vec4 result = vec4(0.0);
    vec3 point = vec3(0.0);
    float light;

    if (dashedEdge >= 0) {
        point = edgePoint(dashedEdge, dashedS);
        vec4 color = paint(point, edgeInstance(dashedEdge), light);
        float a = dashed * hidden * color.a;
        result = vec4(color.rgb * a, a);
    }
    if (OutlineFill.x > 0.0 || OutlineFill.y > 0.0 || progress > 0.0) {
        vec3 hit;
        vec4 face = faces(p, progress, hit);
        if (face.a > 0.0 || face.r > 0.0) {
            result = over(result, face);
            point = hit;
        }
    }
    if (glowEdge >= 0 && OutlineLine.z > 0.0) {
        point = edgePoint(glowEdge, glowS);
        vec4 color = paint(point, edgeInstance(glowEdge), light);
        float a = min(glow * OutlineLine.z * 0.55 * (1.0 + light) * color.a, 1.0);
        // partly additive, like light
        result = over(result, vec4(color.rgb * a, a * 0.7));
    }
    if (litGlowEdge >= 0 && OutlineLine.z > 0.0) {
        vec4 color = paint(edgePoint(litGlowEdge, litGlowS), edgeInstance(litGlowEdge), light);
        float a = min(litGlow * (0.35 + OutlineLine.z * 0.5) * color.a, 1.0);
        result = over(result, vec4(mix(color.rgb, WHITE, 0.3) * a, a * 0.6));
    }
    if (coreEdge >= 0) {
        point = edgePoint(coreEdge, coreS);
        vec4 color = paint(point, edgeInstance(coreEdge), light);
        float a = core * color.a;
        result = over(result, vec4(color.rgb * a, a));
    }
    if (litEdge >= 0) {
        vec4 color = paint(edgePoint(litEdge, litS), edgeInstance(litEdge), light);
        float a = lit * color.a;
        result = over(result, vec4(mix(color.rgb, WHITE, 0.5) * a, a));
    }

    if (result.a <= 0.002 && max(max(result.r, result.g), result.b) <= 0.002) {
        discard;
    }

    // the world's fog, as on vanilla's lines
    float fog = total_fog_value(fog_spherical_distance(point), fog_cylindrical_distance(point), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft glow from banding on dark blocks
    float noise = fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (noise - 0.5) / 255.0, 0.0);
    fragColor = result;
}
