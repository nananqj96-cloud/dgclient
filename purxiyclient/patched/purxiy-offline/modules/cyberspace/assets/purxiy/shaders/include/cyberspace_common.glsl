#version 330

// What the passes of Cyberspace share: hashes, anti-aliased lines and the colours of the two looks. Needs
// cyberspace.glsl first.

uint hashu(uvec3 v) {
    v = v * 1664525u + 1013904223u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    v ^= v >> 16u;
    v.x += v.y * v.z;
    v.y += v.z * v.x;
    v.z += v.x * v.y;
    return v.x ^ v.y ^ v.z;
}

float hashf(uvec3 v) {
    return float(hashu(v) & 0xFFFFFFu) / 16777216.0;
}

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

// An anti-aliased line: d = distance from its middle, hw = half its width, fw = how much d changes over a pixel. A line
// thinner than a pixel gets fainter rather than wider.
float line(float d, float hw, float fw) {
    float w = max(hw, fw * 0.5);
    return clamp((w - d) / fw + 0.5, 0.0, 1.0) * (hw / w);
}

// The two looks: a midnight network (dark cells, lines of light) and a porcelain one (pale cells, lines of ink).
vec3 deepColor() {
    return pow(CySecond.rgb, vec3(2.2));
}

vec3 voidColor() {
    vec3 night = deepColor() * 0.025 + vec3(0.004, 0.003, 0.009);
    vec3 day = mix(vec3(0.95, 0.94, 0.97), CyFirst.rgb, 0.12);
    return mix(night, day, CyLook.x);
}

// the sky's gradient alone: what far surfaces fade into
vec3 skyBase(vec3 ray) {
    float h = ray.y;
    vec3 deep = deepColor();
    vec3 zenith = voidColor() * mix(0.7, 1.0, CyLook.x);
    vec3 horizon = mix(deep * 0.11 + CySecond.rgb * 0.012, mix(vec3(1.0), CyFirst.rgb, 0.35), CyLook.x);
    vec3 color = mix(horizon, zenith, smoothstep(0.0, 0.45, abs(h)));
    // a fine bright horizon
    color += mix(CyFirst.rgb * 0.3, CySecond.rgb * -0.12, CyLook.x) * exp(-abs(h) * 110.0);
    return color;
}

// Lines of light: added on midnight, drawn as ink on porcelain.
vec3 lightOn(vec3 base, vec3 light, float amount) {
    vec3 night = base + light * amount;
    vec3 ink = mix(pow(CySecond.rgb, vec3(1.6)), CySecond.rgb, 0.35);
    vec3 day = mix(base, ink * mix(1.0, 0.6, luma(light) - 0.5), clamp(amount * 0.8, 0.0, 0.92));
    return mix(night, day, CyLook.x);
}
