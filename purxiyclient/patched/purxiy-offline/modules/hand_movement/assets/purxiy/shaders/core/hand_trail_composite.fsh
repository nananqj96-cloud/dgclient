#version 330

#moj_import <purxiy:hand_trail.glsl>

// The trail over the scene, behind your hand, softened by a pixel so the edges of its moments melt together. Where the
// trail ends (the path of the hand's outline: the arc its tip draws) a line of light runs along it, faint on its own and
// bright in Radiance's colours: the light of Radiance's outline, streaming off the swing.

uniform sampler2D TrailSampler;
#ifdef MASKED
// your hand as it is now (with a shader pack, whose depth is its own)
uniform sampler2D MaskSampler;
#endif

out vec4 fragColor;

void main() {
#ifdef MASKED
    if (texelFetch(MaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    // the trail is drawn at half resolution: sample it where this pixel of the screen falls
    vec2 texel = 1.0 / vec2(textureSize(TrailSampler, 0));
    vec2 uv = gl_FragCoord.xy / TrailScreen.xy;
    vec4 trail = texture(TrailSampler, uv) * 0.36;
    trail += texture(TrailSampler, uv + vec2(texel.x, texel.y) * 0.75) * 0.16;
    trail += texture(TrailSampler, uv + vec2(-texel.x, texel.y) * 0.75) * 0.16;
    trail += texture(TrailSampler, uv + vec2(texel.x, -texel.y) * 0.75) * 0.16;
    trail += texture(TrailSampler, uv + vec2(-texel.x, -texel.y) * 0.75) * 0.16;

    // the edge of the trail: how much its coverage changes across a couple of pixels
    float reach = 1.0 * max(1.0, TrailScreen.y / 1080.0);
    float low = 1.0, high = 0.0;
    for (int i = 0; i < 8; i++) {
        float angle = float(i) * 0.7853982;
        float a = texture(TrailSampler, uv + vec2(cos(angle), sin(angle)) * texel * reach).a;
        low = min(low, a);
        high = max(high, a);
    }
    // only its outer edge, where it meets the scene (not where a later moment of it overlaps an earlier one)
    float edge = clamp((high - low) * 2.6, 0.0, 1.0) * (1.0 - smoothstep(0.0, 0.025, low));
    float radiance = TrailFirst.w;
    vec3 light = mix(vec3(1.0), mix(TrailFirst.rgb, TrailAccent.rgb, 0.35), radiance);
    float rim = edge * mix(0.22, 0.85, radiance) * TrailSecond.w;
    // light adds: it brightens what is behind it
    fragColor = trail + vec4(light * rim, rim * 0.35);
}
