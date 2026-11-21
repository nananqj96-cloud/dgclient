#version 330

#ifndef TRAIL_PREVIEW
#moj_import <minecraft:fog.glsl>
#endif
#moj_import <purxiy:trail.glsl>

// A quad of a trail (see dev.purxiy.trail.TrailGeometry), drawn into the trail's own layer:
//
//   line     a clean line of light: a round core, creamy down its middle, in a soft glow of the palette. Its distance is
//            measured on the screen, so it is sharp at any distance and its ends and bends are round.
//   band     the path: a line of light down the middle of a band on the ground, its light falling off to the sides,
//            measured across the band in the world so it lies on the ground as light would
//   mote     a soft round light that breathes a little (Stardust)
//   orb      the previews' runner
//
// Every pixel of the layer keeps only the brightest light that reaches it: the depth written is one minus that light and
// the depth test keeps the least. So the pieces of a line meet seamlessly, and where the trail overlaps itself nothing
// doubles into a crease. The layer is laid over the scene afterwards (trail_composite).
//
// In the world a trail tests itself against the scene's depth (sampled, not attached): what stands in front of it
// covers it softly, over a few centimetres, never with a hard edge.

#ifndef TRAIL_PREVIEW
uniform sampler2D DepthSampler;
#endif

flat in int vKind;
flat in vec4 vEnds;
flat in vec4 vRadii;
flat in vec4 vLife;
flat in vec4 vExtra;
flat in vec4 vDepth;
flat in vec3 vStart;
flat in vec3 vEnd;
flat in float vThread;
in vec3 vWorld;
in vec4 vLocal;
in vec3 vBand;
in vec4 vColor;

out vec4 fragColor;

// The palette along the trail: its first colour where it leaves you, the second, then the accent as it fades.
vec3 paletteAlong(float t) {
    vec3 c = mix(TrailFirst.rgb, TrailSecond.rgb, smoothstep(0.0, 0.55, t));
    return mix(c, TrailAccent.rgb, smoothstep(0.5, 1.0, t));
}

// Where along the palette a part of the trail is: further on as it ages, and drifting slowly, so its colours flow like
// the rings of Jump Circles (not with Reduce motion). The odometer keeps the drift fixed to the world.
float paletteAt(float age, float odo) {
    float drift = TrailUp.w > 0.5 ? 0.0 : 0.12 * sin(odo * 0.9 - TrailScreen.z * 1.3);
    return clamp(age * 1.15 + 0.06 + drift, 0.0, 1.0);
}

// A colour of the palette made more vivid, for glows (as Target ESP's): a light core in a saturated glow reads as neon.
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
    return vec4(mix(v, vec3(1.0), white) * solid, solid) + vec4(v * glow, min(glow * 0.8, 1.0)) * (1.0 - solid);
}

// A glow falling off from a line or a light: a soft core with a longer tail, gone at its reach (x = 0 .. 1).
float glowAt(float x) {
    return (0.6 * exp(-x * x * 9.0) + 0.4 * exp(-x * 4.5)) * (1.0 - smoothstep(0.6, 1.0, x));
}

// How much of it the scene leaves visible: it fades out over `soft` blocks before it reaches a surface.
float visibility(float away, float soft) {
#ifdef TRAIL_PREVIEW
    return 1.0;
#else
    vec2 ndc = gl_FragCoord.xy / TrailScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = TrailInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
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
    float age = clamp(mix(vLife.x, vLife.y, u), 0.0, 1.0);
    float arc = mix(vLife.z, vLife.w, u);
    float shade = mix(vExtra.x, vExtra.y, u);
    float odo = mix(vExtra.z, vExtra.w, u);
    world = mix(vStart, vEnd, u);

    // the core: round and anti-aliased, creamier down its middle
    float core = 1.0 - smoothstep(radius - 0.75, radius + 0.75, dist);
    float middle = 1.0 - clamp(dist / radius, 0.0, 1.0);
    // the glow around it
    float glow = glowAt(max(dist - radius, 0.0) / max(reach - radius, 1.0e-3)) * TrailScreen.w;

    // lavender where it leaves you, lilac further on, a blush of the accent as it fades; a helix's second thread runs
    // ahead in the palette, so the two read apart
    float along = paletteAt(age, odo);
    vec3 tint = paletteAlong(vThread > 0.5 ? 0.4 + 0.6 * along : along);
    // a soft wave of light runs down the line, away from you
    float flow = TrailUp.w > 0.5 ? 0.0 : 0.5 + 0.5 * cos((arc - TrailScreen.z * 2.4) * 2.1);
    float shimmer = 1.0 + 0.12 * flow * (1.0 - age);

    float life = pow(1.0 - age, 1.2) * shade * cover;
    return lightOf(tint, core * 0.96, glow * 0.95 * shimmer, 0.15 + 0.3 * middle * middle) * life;
}

vec4 bandLight(out vec3 world) {
    float across = vLocal.x;
    float age = clamp(vLocal.y, 0.0, 1.0);
    float arc = vLocal.z;
    float shade = vLocal.w;
    world = vWorld;
    float spread = vBand.x, line = vBand.y;
    // blocks from the middle, and blocks per pixel across the band here
    float d = abs(across) * spread;
    float px = max(fwidth(across) * spread, 1.0e-5);
    // the line down the middle: never thinner than a pixel, fainter instead
    float hw = line / px;
    float core = clamp(max(hw, 0.5) + 0.5 - d / px, 0.0, 1.0) * min(hw * 2.0, 1.0);
    float middle = 1.0 - clamp(d / line, 0.0, 1.0);
    // the light spilling over the ground from it, softly falling off towards the edges
    float x = max(d - line, 0.0) / max(spread - line, 1.0e-4);
    float glow = (0.55 * exp(-x * x * 3.2) + 0.45 * exp(-x * x * 14.0)) * (1.0 - smoothstep(0.7, 1.0, x)) * TrailScreen.w;
    vec3 tint = paletteAlong(paletteAt(age, vBand.z));
    float flow = TrailUp.w > 0.5 ? 0.0 : 0.5 + 0.5 * cos((arc - TrailScreen.z * 2.4) * 2.1);
    float shimmer = 1.0 + 0.12 * flow * (1.0 - age);
    float life = pow(1.0 - age, 1.2) * shade;
    return lightOf(tint, core * 0.95, glow * 0.9 * shimmer, 0.2 + 0.3 * middle * middle) * life;
}

vec4 moteLight(out vec3 world) {
    vec2 p = vLocal.xy;
    float r2 = dot(p, p);
    float progress = clamp(vLocal.z, 0.0, 1.0);
    float phase = vLocal.w;
    world = vWorld;
    float core = exp(-r2 * 7.0);
    float glow = glowAt(sqrt(r2)) * (0.35 + 0.65 * TrailScreen.w);
    // in quickly, out slowly
    float life = smoothstep(0.0, 0.15, progress) * (1.0 - smoothstep(0.45, 1.0, progress));
    float breathe = TrailUp.w > 0.5 ? 1.0 : 0.85 + 0.15 * sin(TrailScreen.z * (2.2 + 1.8 * phase) + phase * 6.2832);
    return lightOf(vColor.rgb, core, glow * 0.85, 0.3) * life * breathe * vColor.a;
}

vec4 orbLight(out vec3 world) {
    vec2 p = vLocal.xy;
    float r2 = dot(p, p);
    world = vWorld;
    float core = exp(-r2 * 10.0);
    float glow = glowAt(sqrt(r2));
    return lightOf(paletteAlong(0.1), core, glow * 0.7, 0.6) * vColor.a;
}

void main() {
    vec3 world;
    vec4 result;
    float soft;
    if (vKind == KIND_LINE) {
        result = lineLight(world);
        soft = 0.06;
    } else if (vKind == KIND_BAND) {
        result = bandLight(world);
        soft = 0.04;
    } else if (vKind == KIND_MOTE) {
        result = moteLight(world);
        soft = 0.05;
    } else {
        result = orbLight(world);
        soft = 0.08;
    }
    float away = length(world);
    // what comes close to the camera fades: your own trail in first person, someone running right past you
    result *= smoothstep(TrailLimits.w * 0.3, TrailLimits.w, away);
    result *= visibility(away, soft);

#ifndef TRAIL_PREVIEW
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
