#version 330

// The main menu's world (see HomeRenderer#writeUniforms).
layout(std140) uniform HomeFrame {
    mat4 ViewProj;
    mat4 InvViewProj;
    vec4 Camera;      // xyz = eye, w = time (s), for the wind and the stars
    vec4 FogRange;    // x = fog start, y = fog end (blocks), w = how much night it is (0..1)
    vec4 FogColor;
    vec4 Light;       // rgb = daylight on the blocks, w = wind strength
    vec4 Ambient;     // rgb = light that never fades
    vec4 SkyTop;
    vec4 SkyHorizon;
    vec4 Glow;        // rgb = the glow round a low sun, w = its strength
    vec4 SunDir;      // xyz = towards the sun, w = tan of half its size
    vec4 MoonDir;
    vec4 SunUv;       // the sun in the celestial atlas (u0, v0, u1, v1)
    vec4 MoonUv;
    vec4 CamRight;    // xyz = the camera's right, w = how far the clouds drifted (blocks)
    vec4 CamUp;       // xyz = the camera's up, w = how bright the stars are
    vec4 CloudColor;  // rgb, w = opacity
};

// The sky in a direction: the horizon colour rising into the zenith, the fog below, and a warm glow round a low sun.
vec3 homeSky(vec3 d) {
    float up = d.y;
    vec3 col = mix(SkyHorizon.rgb, SkyTop.rgb, smoothstep(0.0, 0.62, up));
    col = mix(FogColor.rgb, col, smoothstep(-0.14, 0.03, up));
    float s = max(dot(d, normalize(SunDir.xyz)), 0.0);
    float low = 1.0 - smoothstep(-0.08, 0.5, up);
    col += Glow.rgb * Glow.a * (pow(s, 4.0) * 0.55 + pow(s, 24.0) * 0.45) * low;
    return col;
}
