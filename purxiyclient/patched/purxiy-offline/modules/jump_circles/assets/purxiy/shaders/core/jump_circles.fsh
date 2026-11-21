#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:jump_circles.glsl>

// A ring of Jump Circles (see dev.purxiy.jumpcircles.JumpCircles), on its square on the ground:
//
//   line   a distance field in blocks, anti-aliased on real pixels at any distance; whiter than the palette
//   glow   a soft halo around the line, mostly adding light
//   wake   the light the ring leaves behind as it spreads, fading towards the middle and as the ring settles
//
// The colours run around the ring (the palette's first, second and accent, blending into each other). Everything is
// tested against the scene's depth, which is sampled rather than attached, so what stands in front of a ring covers
// it softly instead of cutting it off.

uniform sampler2D DepthSampler;

in vec2 local;
in vec3 world;
flat in int ring;

out vec4 fragColor;

const float TAU = 6.28318530718;

// The palette at a point around the ring (turns): three soft lobes, the accent a little quieter.
vec3 paletteAt(float turns) {
    float a = turns * TAU;
    vec3 w = max(vec3(cos(a), cos(a - TAU / 3.0), cos(a + TAU / 3.0)), 0.0);
    w *= w * vec3(1.0, 1.0, 0.75);
    return (CircleFirst.rgb * w.x + CircleSecond.rgb * w.y + CircleAccent.rgb * w.z) / (w.x + w.y + w.z);
}

// How much of the ring the scene leaves visible here: soft over a few centimetres, like light fading into a surface.
float visibility(float away) {
    vec2 ndc = gl_FragCoord.xy / CircleScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = CircleInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    float sceneDistance = length(scene.xyz / scene.w);
    float soft = 0.05 + away * 0.004;
    return clamp((sceneDistance - away) / soft + 1.0, 0.0, 1.0);
}

void main() {
    vec4 centre = CircleRings[ring * 2];
    vec4 look = CircleRings[ring * 2 + 1];
    float radius = centre.w;
    float halfWidth = look.x;
    float age = look.z;
    float reach = CircleScreen.w;

    float r = length(local);
    float d = r - radius;
    // blocks per pixel here, across the ring
    float px = max(fwidth(r), 1.0e-5);

    // the line: never thinner than a pixel; thinner lines fade instead of breaking up
    float hw = halfWidth / px;
    float line = clamp(max(hw, 0.5) + 0.5 - abs(d) / px, 0.0, 1.0) * min(hw * 2.0, 1.0);

    // the glow: a soft core with a longer tail, gone at its reach
    float x = max(abs(d) - halfWidth, 0.0) / reach;
    float glow = (0.6 * exp(-x * x * 9.0) + 0.4 * exp(-x * 4.5)) * (1.0 - smoothstep(0.6, 1.0, x)) * CircleFirst.w;

    // the wake inside the ring
    float inside = max(-d - halfWidth, 0.0) / max(radius * 0.3, 1.0e-3);
    float wake = d < 0.0 ? pow(max(1.0 - inside, 0.0), 2.0) * (1.0 - age) * (1.0 - age) : 0.0;

    vec3 tint = paletteAt(atan(local.y, local.x) / TAU + look.w);
    float lineAlpha = line * 0.95;
    float light = glow + wake * 0.7;
    // light mostly adds up: it glows on dark ground and only warms bright ground
    vec4 result = vec4(mix(tint, vec3(1.0), 0.3) * lineAlpha, lineAlpha) + vec4(tint * light, light * 0.55) * (1.0 - lineAlpha);

    float away = length(world);
    result *= look.y * visibility(away);
    if (result.a <= 0.0 && result.r + result.g + result.b <= 0.0) {
        discard;
    }

    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(world), fog_cylindrical_distance(world), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft light from banding on dark ground
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
