#version 330

#moj_import <purxiy:home.glsl>

uniform sampler2D CelestialSampler;

in vec3 vRay;

out vec4 fragColor;

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

// A quiet field of stars: a few cells of a fine grid round the sphere hold one, each twinkling at its own pace.
vec3 stars(vec3 d) {
    const float CELLS = 150.0;
    vec3 cell = floor(d * CELLS);
    float h = hash13(cell);
    if (h > 0.0032) {
        return vec3(0.0);
    }
    vec3 jitter = vec3(hash13(cell + 1.7), hash13(cell + 3.1), hash13(cell + 5.3)) - 0.5;
    vec3 centre = normalize((cell + 0.5 + jitter * 0.6) / CELLS);
    float dist = length(centre - d) * CELLS;
    float star = smoothstep(0.42, 0.0, dist);
    float twinkle = 0.62 + 0.38 * sin(Camera.w * (1.3 + h * 700.0) + h * 5000.0);
    float bright = 0.45 + 0.55 * fract(h * 917.0);
    return star * twinkle * bright * mix(vec3(0.78, 0.84, 1.0), vec3(1.0, 0.9, 0.82), fract(h * 731.0));
}

// The sun or the moon: Minecraft's square picture of it, on the plane facing it.
vec4 body(vec3 d, vec3 dir, float tanHalf, vec4 uv) {
    float c = dot(d, dir);
    if (c <= 0.0 || uv.z <= uv.x) {
        return vec4(0.0);
    }
    vec3 right = normalize(cross(dir, vec3(0.0, 1.0, 0.0001)));
    vec3 up = normalize(cross(right, dir));
    vec2 q = vec2(dot(d, right), dot(d, up)) / (c * tanHalf);
    if (abs(q.x) > 1.0 || abs(q.y) > 1.0) {
        return vec4(0.0);
    }
    return texture(CelestialSampler, mix(uv.xy, uv.zw, q * 0.5 + 0.5));
}

void main() {
    vec3 d = normalize(vRay);
    vec3 col = homeSky(d);
    float above = smoothstep(-0.02, 0.06, d.y);
    float night = FogRange.w;
    col += stars(d) * CamUp.w * above;
    vec3 sunDir = normalize(SunDir.xyz);
    vec4 sun = body(d, sunDir, SunDir.w, SunUv);
    col += sun.rgb * sun.a * above * (1.0 - night * 0.9);
    vec4 moon = body(d, normalize(MoonDir.xyz), MoonDir.w, MoonUv);
    col += moon.rgb * moon.a * above * mix(0.35, 1.0, night);
    // a soft halo round the sun
    float s = max(dot(d, sunDir), 0.0);
    col += Light.rgb * (pow(s, 80.0) * 0.32 + pow(s, 12.0) * 0.06) * (1.0 - night);
    fragColor = vec4(col, 1.0);
}
