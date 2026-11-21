#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <purxiy:aura_marker.glsl>

// The Silent Aura's ring (see dev.purxiy.aura.AuraMarker), on the band standing on it:
//
//   line   a thin horizontal line of light, a distance field in blocks, anti-aliased on real pixels at any distance
//   glow   a soft halo above and below the line, mostly adding light
//   tail   the light the ring leaves behind as it sweeps along the body, fading away from the line
//
// The colours run around the ring (the palette's first, second and accent, blending into each other) and brighten with
// every hit. Everything is tested against the scene's depth, which is sampled rather than attached, so the body in front
// of the back half of the ring covers it softly instead of cutting it off.

uniform sampler2D DepthSampler;

in float angle;
in float height;
in vec3 world;
flat in int ring;

out vec4 fragColor;

const float TAU = 6.28318530718;

// The palette at a point around the ring (turns): three soft lobes, the accent a little quieter.
vec3 paletteAt(float turns) {
    float a = turns * TAU;
    vec3 w = max(vec3(cos(a), cos(a - TAU / 3.0), cos(a + TAU / 3.0)), 0.0);
    w *= w * vec3(1.0, 1.0, 0.75);
    return (MarkerFirst.rgb * w.x + MarkerSecond.rgb * w.y + MarkerAccent.rgb * w.z) / (w.x + w.y + w.z);
}

// How much of the ring the scene leaves visible here: soft over a few centimetres, like light fading into a surface.
float visibility(float away) {
    vec2 ndc = gl_FragCoord.xy / MarkerScreen.xy * 2.0 - 1.0;
    float depth = texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 scene = MarkerInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
    float sceneDistance = length(scene.xyz / scene.w);
    float soft = 0.04 + away * 0.004;
    return clamp((sceneDistance - away) / soft + 1.0, 0.0, 1.0);
}

void main() {
    vec4 band = Markers[ring * 3 + 1];
    vec4 look = Markers[ring * 3 + 2];
    float pulse = look.y;
    float halfWidth = band.z * (1.0 + 0.6 * pulse);
    float reach = band.w * (1.0 + 0.35 * pulse);

    float d = height - band.x;
    // blocks per pixel here, up the band
    float px = max(fwidth(height), 1.0e-5);

    // the line: never thinner than a pixel; thinner lines fade instead of breaking up
    float hw = halfWidth / px;
    float line = clamp(max(hw, 0.5) + 0.5 - abs(d) / px, 0.0, 1.0) * min(hw * 2.0, 1.0);

    // the glow: a soft core with a longer tail, gone at its reach
    float x = max(abs(d) - halfWidth, 0.0) / reach;
    float glow = (0.6 * exp(-x * x * 9.0) + 0.4 * exp(-x * 4.5)) * (1.0 - smoothstep(0.6, 1.0, x)) * (0.45 + 0.5 * pulse);

    // the tail, on the side the ring came from
    float span = band.y - band.x;
    float tail = 0.0;
    if (abs(span) > 1.0e-3 && d * span > 0.0) {
        float s = clamp(d / span, 0.0, 1.0);
        tail = pow(1.0 - s, 1.5) * look.w * smoothstep(0.0, 0.1, abs(span));
    }

    vec3 tint = paletteAt(angle + look.z);
    float lineAlpha = line * 0.95;
    float light = glow + tail * 0.7;
    // light mostly adds up: it glows on dark clothes and only warms bright ones
    vec4 result = vec4(mix(tint, vec3(1.0), 0.3 + 0.3 * pulse) * lineAlpha, lineAlpha) + vec4(tint * light, light * 0.5) * (1.0 - lineAlpha);

    float away = length(world);
    result *= look.x * visibility(away);
    if (result.a <= 0.0 && result.r + result.g + result.b <= 0.0) {
        discard;
    }

    // the world's fog
    float fog = total_fog_value(fog_spherical_distance(world), fog_cylindrical_distance(world), FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd) * FogColor.a;
    result.rgb = mix(result.rgb, FogColor.rgb * result.a, fog);

    // a little dither keeps the soft light from banding
    float dither = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    result.rgb = max(result.rgb + (dither - 0.5) / 255.0, 0.0);
    fragColor = result;
}
