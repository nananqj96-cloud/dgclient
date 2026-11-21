#version 330

#ifndef AURA_PREVIEW
#moj_import <minecraft:fog.glsl>
#endif
#moj_import <purxiy:aura.glsl>

// A quad of an aura (see dev.purxiy.auras.AuraGeometry), drawn into the aura's own layer:
//
//   line   a clean line of light: a round core, creamy down its middle, in a soft glow of its colour. Its distance is
//          measured on the screen, so it is sharp at any distance and its ends and bends are round.
//   mote   a soft round light, whiter in its middle; a star also has a fine four-pointed sparkle, like the Purxiy star.
//
// Every pixel of the layer keeps only the brightest light that reaches it: the depth written is one minus that light and
// the depth test keeps the least. So the pieces of a tail meet seamlessly, and where lights cross nothing doubles into
// a crease or burns out. The layer is laid over the scene afterwards (aura_composite).
//
// In the world the aura tests itself against the scene's depth (sampled, not attached): your body covers what circles
// behind it, softly, over a few centimetres. The wardrobe's stage draws the aura in two halves instead, behind and in
// front of you (AuraFade.z), with you drawn in between.

#ifndef AURA_PREVIEW
uniform sampler2D DepthSampler;
#endif

flat in int vKind;
flat in vec4 vEnds;
flat in vec4 vRadii;
flat in vec4 vShade;
flat in vec4 vDepth;
flat in vec3 vStart;
flat in vec3 vEnd;
flat in vec3 vColorA;
flat in vec3 vColorB;

out vec4 fragColor;

// A colour made more vivid, for glows (as Target ESP's and Trail's): a light core in a saturated glow reads as neon.
vec3 vivid(vec3 c) {
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(luma), c, 1.6), 0.0, 1.0);
}

// Light with some body (as Target ESP's): a core (`solid`, lifted towards white by `white`) in a glow of the vivid
// colour. At night it glows; on bright ground and against the sky it still tints them instead of washing them out.
vec4 lightOf(vec3 tint, float solid, float glow, float white) {
    solid = clamp(solid, 0.0, 1.0);
    glow = min(glow, 1.5);
    vec3 v = vivid(tint);
    return vec4(mix(v, vec3(1.0), clamp(white, 0.0, 1.0)) * solid, solid) + vec4(v * glow, min(glow * 0.8, 1.0)) * (1.0 - solid);
}

// A glow falling off from a line or a light: a soft core with a longer tail, gone at its reach (x = 0 .. 1).
float glowAt(float x) {
    return (0.6 * exp(-x * x * 9.0) + 0.4 * exp(-x * 4.5)) * (1.0 - smoothstep(0.6, 1.0, x));
}

// The glow behind you (a light without a core): nearly even over its middle, which your body covers, and soft towards
// its reach, so what is seen of it is a calm band of light around you.
float hazeAt(float x) {
    return pow(1.0 - smoothstep(0.35, 1.0, x), 1.4);
}

// How bright the glows are: some glow stays even at 0%, so a light never looks cut out.
float glowSetting() {
    return 0.25 + 0.75 * AuraScreen.w;
}

// A four-pointed sparkle, like the star in the Purxiy logo: p in its own units (1 = the tips).
float sparkle(vec2 p) {
    vec2 a = abs(p);
    float rays = exp(-a.x * 24.0) * exp(-a.y * 3.4) + exp(-a.y * 24.0) * exp(-a.x * 3.4);
    return rays * (1.0 - smoothstep(0.65, 1.0, max(a.x, a.y)));
}

// How much of it the scene leaves visible: it fades out over `soft` blocks before it reaches a surface.
float visibility(float away, float soft) {
#ifdef AURA_PREVIEW
    return 1.0;
#else
    vec2 ndc = gl_FragCoord.xy / AuraScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = AuraInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    float sceneDistance = length(scene.xyz / scene.w);
    return clamp((sceneDistance - away + 0.015) / soft, 0.0, 1.0);
#endif
}

vec4 lineLight(out vec3 world) {
    vec2 p = gl_FragCoord.xy;
    vec2 s0 = vEnds.xy, s1 = vEnds.zw;
    vec2 d = s1 - s0;
    float l2 = dot(d, d);
    float t = l2 > 1.0e-6 ? clamp(dot(p - s0, d) / l2, 0.0, 1.0) : 0.0;
    float dist = length(p - (s0 + d * t));
    // the same point of the piece in the world (on the screen, depth does not run evenly along it)
    float w0 = vDepth.x, w1 = vDepth.y;
    float u = t * w0 / max((1.0 - t) * w1 + t * w0, 1.0e-6);
    float radius = mix(vRadii.x, vRadii.y, t);
    float reach = mix(vRadii.z, vRadii.w, t);
    float cover = mix(vDepth.z, vDepth.w, t);
    world = mix(vStart, vEnd, u);

    // the core: round and anti-aliased, creamier down its middle
    float core = 1.0 - smoothstep(radius - 0.75, radius + 0.75, dist);
    float middle = 1.0 - clamp(dist / radius, 0.0, 1.0);
    // the glow around it
    float glow = glowAt(max(dist - radius, 0.0) / max(reach - radius, 1.0e-3)) * glowSetting();
    vec3 tint = mix(vColorA, vColorB, u);
    float strength = mix(vShade.x, vShade.y, u);
    float white = mix(vShade.z, vShade.w, u);
    return lightOf(tint, core * 0.96, glow * 0.9, white * (0.45 + 0.55 * middle * middle)) * strength * cover;
}

vec4 moteLight(out vec3 world) {
    vec2 p = gl_FragCoord.xy - vEnds.xy;
    float d = length(p);
    float core = vEnds.z, reach = vEnds.w;
    world = vStart;
    // a round core, anti-aliased, whiter in its middle: creamy rather than flat (a glow alone has no core)
    float solid = core > 0.0 ? 1.0 - smoothstep(core - 0.8, core + 0.8, d) : 0.0;
    float middle = core > 0.0 ? exp(-d * d / max(core * core, 1.0e-4) * 1.4) : 0.0;
    float glow = (core > 0.0 ? glowAt(d / reach) : hazeAt(d / reach)) * glowSetting();
    // a star's fine rays, turned a little
    float rays = 0.0;
    if (vRadii.x > 0.0) {
        vec2 q = vec2(p.x * vRadii.z + p.y * vRadii.w, p.y * vRadii.z - p.x * vRadii.w) / max(vRadii.y, 1.0e-3);
        rays = sparkle(q) * vRadii.x;
    }
    float white = mix(vShade.y, 1.0, middle * 0.45);
    return lightOf(vColorA, solid, glow * 0.85 + rays * 0.95, white) * vShade.x * vDepth.x;
}

void main() {
    vec3 world;
    vec4 result;
    float soft;
    if (vKind == KIND_LINE) {
        result = lineLight(world);
        soft = 0.05;
    } else {
        result = moteLight(world);
        // a glow without a core (the haze behind you) fades long before a surface, so the ground never cuts it
        soft = vEnds.z > 0.0 ? 0.06 : 0.6;
    }
    float away = length(world);
    // what comes close to the camera fades: your own aura in first person, someone walking right past you
    result *= smoothstep(AuraFade.x, AuraFade.y, away);
    // the wardrobe's stage: only the half behind you, or the half in front of you
    if (AuraFade.z != 0.0 && (dot(world, AuraForward.xyz) - AuraFade.w) * AuraFade.z < 0.0) {
        discard;
    }
    result *= visibility(away, soft);

#ifndef AURA_PREVIEW
    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(world), fog_cylindrical_distance(world), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);
#endif

    // a little dither keeps soft light from banding in the layer's eight bits
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715)))) - 0.5;
    result = clamp(result + vec4(dither / 255.0), 0.0, 1.0);
    // every pixel keeps only the brightest light that reaches it (the depth test keeps the least depth)
    float key = max(result.a, max(result.r, max(result.g, result.b)));
    if (key < 0.75 / 255.0) {
        discard;
    }
    gl_FragDepth = 1.0 - key;
    fragColor = result;
}
