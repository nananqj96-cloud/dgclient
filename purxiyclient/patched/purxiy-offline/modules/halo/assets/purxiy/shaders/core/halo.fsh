#version 330

#ifndef HALO_PREVIEW
#moj_import <minecraft:fog.glsl>
#endif
#moj_import <purxiy:halo.glsl>

// The halo (see dev.purxiy.halo.HaloRenderer), as light on the screen. Every piece is a line through its points (a ring
// seen from the camera is two of them, its far half and its near half) or a single point, given in pixels with a width
// and a glow of its own at every point:
//
//   core   a tube of light, anti-aliased on real pixels at any size, its middle burning towards white
//   glow   a soft halo of the piece's colour round it, mostly adding light
//   star   a dot can flare into a four-pointed star
//
// Pieces come far to near and are laid over each other, so a near arc passes in front of a far one. In the world each
// piece is tested against the scene's depth, which is sampled rather than attached, so what stands in front of the halo
// covers it softly.

#ifndef HALO_PREVIEW
uniform sampler2D DepthSampler;
#endif

out vec4 fragColor;

vec3 colourOf(float code) {
    float b = floor(code / 65536.0);
    float g = floor((code - b * 65536.0) / 256.0);
    float r = code - b * 65536.0 - g * 256.0;
    return vec3(r, g, b) / 255.0;
}

void main() {
    vec2 p = gl_FragCoord.xy;
    float sceneDistance = 1.0e9;
#ifndef HALO_PREVIEW
    if (HaloScreen.w > 0.5) {
        vec2 ndc = p / HaloScreen.xy * 2.0 - 1.0;
        float depth = texelFetch(DepthSampler, ivec2(p), 0).r;
        vec4 scene = HaloInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
        sceneDistance = length(scene.xyz / scene.w);
    }
#endif

    vec4 result = vec4(0.0);
    int pieces = int(HaloScreen.z);
    for (int s = 0; s < pieces; s++) {
        vec4 rect = HaloPieces[s * 2 + 1];
        if (p.x < rect.x || p.y < rect.y || p.x > rect.z || p.y > rect.w) {
            continue;
        }
        vec4 head = HaloPieces[s * 2];
        int first = int(head.x + 0.5);
        int count = int(head.y + 0.5);

        // the nearest point of the piece: where its edge is closest, so a tapering spire keeps its shape
        int at = first;
        float t = 0.0;
        float d = 0.0;
        if (count == 1) {
            d = length(p - HaloPoints[first * 3].xy);
        } else {
            float best = 1.0e9;
            for (int i = first; i < first + count - 1; i++) {
                vec4 a = HaloPoints[i * 3];
                vec4 b = HaloPoints[(i + 1) * 3];
                vec2 ab = b.xy - a.xy;
                float l2 = dot(ab, ab);
                float u = l2 > 1.0e-6 ? clamp(dot(p - a.xy, ab) / l2, 0.0, 1.0) : 0.0;
                float dist = length(p - a.xy - ab * u);
                float edge = dist - mix(a.w, b.w, u);
                if (edge < best) {
                    best = edge;
                    at = i;
                    t = u;
                    d = dist;
                }
            }
        }
        int next = count == 1 ? at : at + 1;
        vec4 A = mix(HaloPoints[at * 3], HaloPoints[next * 3], t);
        vec4 B = mix(HaloPoints[at * 3 + 1], HaloPoints[next * 3 + 1], t);
        vec4 C0 = HaloPoints[at * 3 + 2];
        vec4 C1 = HaloPoints[next * 3 + 2];
        vec3 coreColour = mix(colourOf(C0.x), colourOf(C1.x), t);
        vec3 glowColour = mix(colourOf(C0.y), colourOf(C1.y), t);
        float white = mix(C0.z, C1.z, t);

        float hw = A.w;
        float reach = max(B.x, 1.0e-3);
        // the core: never thinner than a pixel; thinner lines fade instead of breaking up
        float w = max(hw, 0.5);
        float cover = clamp(w + 0.5 - d, 0.0, 1.0) * min(hw * 2.0, 1.0);
        // across the tube: the colour deepens towards its edges (so it stays drawn against a bright sky) and its middle
        // burns towards white
        float across = clamp(d / max(hw, 0.75), 0.0, 1.0);
        float hot = 1.0 - across * across;
        vec3 tube = mix(glowColour, coreColour, smoothstep(0.0, 0.65, hot));
        vec3 core = mix(tube, vec3(1.0, 0.985, 0.955), white * hot * hot) * B.y;

        // the glow: a soft core with a longer tail, gone at its reach
        float x = max(d - hw, 0.0) / reach;
        float glow = (0.55 * exp(-x * x * 7.0) + 0.45 * exp(-x * 3.6)) * (1.0 - smoothstep(0.55, 1.0, x));
        if (count == 1 && head.w > 0.0) {
            // a star: two thin rays crossing, a little longer than its glow
            vec2 q = abs(p - A.xy);
            float thin = max(hw * 0.45, 0.4);
            float rays = exp(-min(q.x, q.y) / thin) * max(1.0 - max(q.x, q.y) / (reach * 1.35), 0.0);
            glow += rays * rays * head.w * 1.6;
            cover = max(cover, clamp(rays * head.w * 1.2 - 0.2, 0.0, 1.0) * 0.6);
        }
        glow *= B.z;

        float opacity = B.w;
        vec4 piece = vec4(core * cover, cover * opacity) + vec4(glowColour * glow, glow * 0.38) * (1.0 - cover * opacity);

        // what stands in front of it: soft over a few centimetres, like light fading into a surface
        float soft = 0.05 + A.z * 0.004;
        piece *= clamp((sceneDistance - A.z) / soft + 1.0, 0.0, 1.0);
        result = piece + (1.0 - piece.a) * result;
    }
    result *= HaloCentre.w;
    if (result.a <= 0.0 && result.r + result.g + result.b <= 0.0) {
        discard;
    }

#ifndef HALO_PREVIEW
    // the world's fog
    vec3 centre = HaloCentre.xyz;
    float fog = total_fog_value(fog_spherical_distance(centre), fog_cylindrical_distance(centre), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);
#endif

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = min(result, vec4(1.0));
}
