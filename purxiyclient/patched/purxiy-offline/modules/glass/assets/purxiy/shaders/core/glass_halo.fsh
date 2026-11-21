#version 330

#moj_import <purxiy:glass.glsl>

// The light Glass gives around itself (see dev.purxiy.glass.GlassRenderer), at a quarter of the resolution:
//
//   SOURCE    where the glass is, from its depth, in the colour of its light; glass far away gives less of it, or a far
//             figure would drown in a blot of light
//   BLUR_X/Y  a soft Gaussian, across and then down
//   (none)    added to the image around the glass; over the glass itself only a little, so it stays clear. By day it is
//             softer, as any light is.

#if defined(SOURCE) || !defined(BLUR_X) && !defined(BLUR_Y)
uniform sampler2D GlassDepthSampler;
#endif
#ifndef SOURCE
uniform sampler2D HaloSampler;
#endif

in vec2 uv;

out vec4 fragColor;

void main() {
#if defined(SOURCE)
    // a texel of the halo covers 4 x 4 pixels: four of them tell how much of it is glass, and how near
    ivec2 full = textureSize(GlassDepthSampler, 0);
    ivec2 base = ivec2(gl_FragCoord.xy) * 4;
    float light = 0.0;
    for (int i = 0; i < 4; i++) {
        ivec2 at = min(base + ivec2(1 + 2 * (i & 1), 1 + 2 * (i >> 1)), full - 1);
        float depth = texelFetch(GlassDepthSampler, at, 0).r;
        if (depth < 1.0) {
            vec2 ndc = (vec2(at) + 0.5) / vec2(full) * 2.0 - 1.0;
            vec4 p = GlassInverse * vec4(ndc, depth * 2.0 - 1.0, 1.0);
            float away = length(p.xyz / p.w);
            light += 0.25 * (GlassRight.w > 0.5 ? 0.8 : clamp(1.35 - away / 36.0, 0.3, 1.0));
        }
    }
    fragColor = vec4(GlassGlow.rgb * light, light);
#elif defined(BLUR_X) || defined(BLUR_Y)
    vec2 texel = 1.0 / vec2(textureSize(HaloSampler, 0));
#ifdef BLUR_X
    vec2 step = vec2(texel.x, 0.0);
#else
    vec2 step = vec2(0.0, texel.y);
#endif
    vec4 sum = vec4(0.0);
    float total = 0.0;
    for (int i = -7; i <= 7; i++) {
        float w = exp(-float(i * i) / (2.0 * 4.6 * 4.6));
        sum += texture(HaloSampler, uv + step * float(i)) * w;
        total += w;
    }
    fragColor = sum / total;
#else
    vec3 halo = texture(HaloSampler, uv).rgb;
    float inside = texelFetch(GlassDepthSampler, ivec2(gl_FragCoord.xy), 0).r < 1.0 ? 1.0 : 0.0;
    float day = mix(1.0, 0.45, GlassUp.w * (1.0 - GlassRight.w * 0.5));
    fragColor = vec4(halo * GlassGlow.w * 1.6 * day * (1.0 - 0.8 * inside), 0.0);
#endif
}
