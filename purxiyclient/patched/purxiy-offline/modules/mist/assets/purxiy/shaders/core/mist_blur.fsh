#version 330

#moj_import <purxiy:mist.glsl>

// The march smoothed on its own grid, along x (BLUR_X) and then along z: each texel takes in its neighbours whose
// distance matches its own, so the fine grain the march's staggered steps leave melts into a creamy mist, while a near
// edge keeps clear of the mist behind it.

uniform sampler2D DepthSampler;
uniform sampler2D MarchSampler;

out vec4 fragColor;

float distanceAt(ivec2 q) {
    float depth = texelFetch(DepthSampler, min(q * 2, ivec2(MistScreen.xy) - 1), 0).r;
    return depth >= 0.999999 ? 1.0e6 : mistLinear(depth);
}

void main() {
    ivec2 q = ivec2(gl_FragCoord.xy);
    ivec2 grid = ivec2(MistScreen.zw);
#ifdef BLUR_X
    ivec2 axis = ivec2(1, 0);
#else
    ivec2 axis = ivec2(0, 1);
#endif
    float own = distanceAt(q);
    vec4 sum = vec4(0.0);
    float total = 0.0;
    for (int i = -2; i <= 2; i++) {
        ivec2 at = clamp(q + axis * i, ivec2(0), grid - 1);
        float off = abs(distanceAt(at) - own) / max(own, 0.5);
        float w = (i == 0 ? 1.0 : abs(i) == 1 ? 0.75 : 0.4) * exp(-off * off * 600.0);
        sum += texelFetch(MarchSampler, at, 0) * w;
        total += w;
    }
    fragColor = sum / total;
}
