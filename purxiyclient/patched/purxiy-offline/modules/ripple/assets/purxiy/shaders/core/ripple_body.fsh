#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:ripple.glsl>

// The waves of Ripple on a body (see dev.purxiy.ripple.PurxiyRipple). Every pixel knows where on the body it is, so a
// wave is a sphere of light growing from where the hit landed, cut by the body's own shape:
//
//   front  a thin, bright band racing out over the body, whiter than the palette, fading as it reaches the far side
//   wash   behind the front the body is washed with the palette, lit most along its edges; the light ebbs where the
//          front passed first, so it drains away from the impact point outwards
//   flash  a white-hot spot where the hit landed, for a moment
//
// A critical hit has a thinner, whiter front with an echo behind it. Premultiplied: the light covers the body a little
// and mostly adds, so dark mobs glow and bright ones take the colour instead of burning out to white.
//
// With DEPTH, only the depth is written, where the body is what you see: the light pass then lights only the front-most
// layer of it (a hat over a head, armour over a body).

uniform sampler2D Sampler0;
uniform sampler2D DepthSampler;
#ifdef MASKED
// your hands as they are now (with a shader pack, whose image already holds them and whose depth is its own)
uniform sampler2D HandMaskSampler;
#endif

in vec2 texCoord0;
in vec3 world;
in vec3 normal;
flat in int body;

out vec4 fragColor;

// How far away the scene is at this pixel (blocks), from its depth.
float sceneDistance() {
    vec2 ndc = gl_FragCoord.xy / RippleScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = RippleInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    return length(scene.xyz / scene.w);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
    // only where this is what the scene shows: not behind a wall, not the far side of the body
    float away = length(world);
    float tolerance = 0.035 + away * 0.0025;
    float behind = away - sceneDistance();
    if (behind > tolerance) {
        discard;
    }
#ifdef DEPTH
    fragColor = vec4(0.0);
#else
#ifdef MASKED
    if (texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    int base = body * BODY_ROWS;
    vec4 head = RippleBodies[base];
    int waves = int(head.x + 0.5);
    float glow = RippleScreen.w;
    float linger = max(RippleFirst.w, 0.1);
    float time = RippleScreen.z;

    // lit most along the edges of the body, where it turns away from you
    vec3 n = normalize(normal);
    float facing = abs(dot(n, normalize(-world)));
    float rim = pow(1.0 - facing, 1.6);

    vec3 light = vec3(0.0);
    float cover = 0.0;
    for (int i = 0; i < MAX_WAVES; i++) {
        if (i >= waves) {
            break;
        }
        vec4 start = RippleBodies[base + 1 + i * 2];
        vec4 look = RippleBodies[base + 2 + i * 2];
        float age = start.w;
        float reach = max(look.x, 0.1);
        bool crit = look.y > 0.5;
        float spread = max(look.z, 0.05);
        float seed = look.w;

        float d = distance(world, start.xyz);
        float u = clamp(age / spread, 0.0, 1.0);
        float eased = 1.0 - (1.0 - u) * (1.0 - u);
        float front = reach * eased;
        float life = spread + linger * 1.25;
        float end = 1.0 - smoothstep(life * 0.72, life, age);

        // the front: a white-hot core in a lilac halo that trails a longer glow, then two fainter rings behind it, like
        // a ripple on water; anti-aliased on real pixels
        float px = max(fwidth(d), 1.0e-4);
        float coreWidth = max(crit ? 0.02 : 0.028, px * 1.2);
        float haloWidth = max(crit ? 0.07 : 0.1, px * 2.0);
        float x = d - front;
        float core = exp(-(x * x) / (coreWidth * coreWidth));
        float halo = exp(-(x * x) / (haloWidth * haloWidth)) + (x < 0.0 ? 0.45 * exp(x / (haloWidth * 2.5)) : 0.0);
        float e1 = (d - front * 0.72) / (haloWidth * 0.75);
        float e2 = (d - front * 0.47) / (haloWidth * 0.75);
        float echoes = exp(-e1 * e1) * (crit ? 0.55 : 0.4) + exp(-e2 * e2) * (crit ? 0.35 : 0.2);
        float bandFade = sqrt(1.0 - u) * (1.0 - smoothstep(0.85, 1.0, u)) * smoothstep(0.0, 0.03, age);
        core *= bandFade;
        halo *= bandFade;
        echoes *= bandFade * smoothstep(0.05, 0.25, u);

        // the wash: since the front passed here (the inverse of its ease), ebbing away
        float passedAt = spread * (1.0 - sqrt(max(1.0 - d / reach, 0.0)));
        float since = age - passedAt;
        float wash = since > 0.0 ? exp(-since / (linger * 0.45)) * smoothstep(0.0, 0.06, since) : 0.0;
        wash *= end;

        // the flash where it landed
        float flash = exp(-d * d / 0.03) * exp(-age / 0.11);

        vec3 bandColor = crit ? mix(RippleAccent.rgb, vec3(1.0), 0.3) : mix(RippleFirst.rgb, RippleSecond.rgb, 0.4);
        float drift = 0.5 + 0.5 * sin(d * 5.5 - time * 2.6 + seed * 6.2831);
        vec3 washColor = mix(mix(RippleFirst.rgb, RippleSecond.rgb, 0.5), RippleSecond.rgb, smoothstep(0.0, 0.9, d / reach));
        washColor = mix(washColor, RippleAccent.rgb, 0.22 * drift);

        vec3 hot = crit ? mix(RippleAccent.rgb, vec3(1.0), 0.7) : mix(RippleFirst.rgb, vec3(1.0), 0.65);
        float washLight = wash * (0.55 + 0.6 * rim);
        light += (hot * core * (crit ? 1.5 : 1.25) + bandColor * (halo * 0.85 + echoes)) * glow + washColor * washLight * glow
            + vec3(1.0) * flash * (crit ? 1.3 : 1.0) * glow;
        cover += (core * 0.4 + halo * 0.25 + echoes * 0.2 + wash * (0.42 + 0.2 * rim) + flash * 0.35) * glow;
    }
    cover = min(cover, 0.7);
    light = min(light, vec3(1.35));
    vec4 result = vec4(light, cover);
    // softly into what is in front of it, like light fading into a surface
    result *= clamp((tolerance - behind) / (tolerance * 0.5), 0.0, 1.0) * clamp(head.y, 0.0, 1.0);
    if (result.a <= 0.002 && result.r + result.g + result.b <= 0.002) {
        discard;
    }

    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(world), fog_cylindrical_distance(world), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft light from banding on dark bodies
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
#endif
}
