#version 330

layout(std140) uniform PurxiyWeatherParticles {
    vec4 WeatherDepth;
    vec4 WeatherScreen;
    vec4 WeatherExtra;
};
uniform sampler2D DepthSampler;
#ifdef MASKED
// your hand, which a shader pack has drawn into the picture but not into the depth: drops stay behind it
uniform sampler2D HandMaskSampler;
#endif

in vec2 vLocal;
flat in vec4 vBody;
flat in vec4 vGlow;
flat in vec4 vShape;
flat in vec2 vSize;
flat in float vFade;
out vec4 fragColor;

float linearDepth(float depth) {
    float n = WeatherDepth.x, f = WeatherDepth.y;
    return 2.0 * n * f / (f + n - (depth * 2.0 - 1.0) * (f - n));
}

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 d = b - a;
    return length(p - a - d * clamp(dot(p - a, d) / max(dot(d, d), 0.00001), 0.0, 1.0));
}

float crystal(vec2 p, float turn) {
    float r = length(p);
    float sector = 1.04719755;
    float angle = mod(atan(p.y, p.x) + turn + sector * 0.5, sector) - sector * 0.5;
    vec2 q = vec2(cos(angle), abs(sin(angle))) * r;
    float d = segment(q, vec2(0.0), vec2(0.94, 0.0)) - 0.042;
    d = min(d, segment(q, vec2(0.40, 0.0), vec2(0.65, 0.26)) - 0.030);
    d = min(d, segment(q, vec2(0.66, 0.0), vec2(0.82, 0.17)) - 0.025);
    d = min(d, abs(dot(q, vec2(0.8660254, 0.5)) - 0.24) - 0.028);
    d = min(d, r - 0.115);
    return 1.0 - smoothstep(-fwidth(d), fwidth(d), d);
}

vec4 rain(vec2 p) {
    // The round head, slender tapered tail and offset glint give a drop a clear, liquid silhouette.
    float along = p.y * 0.5 + 0.5;
    float taper = mix(0.65, 0.19, along);
    float body = exp(-p.x * p.x / max(0.01, taper * taper)) * pow(max(0.0, 1.0 - p.y * p.y), 0.48);
    float head = exp(-p.x * p.x * 6.0 - pow((p.y + 0.58) * 3.0, 2.0));
    float glint = exp(-pow((p.x + 0.17) * 9.0, 2.0)) * pow(max(0.0, 1.0 - p.y * p.y), 2.0);
    float glow = exp(-p.x * p.x * 0.75) * max(0.0, 1.0 - p.y * p.y);
    float alpha = (body * 0.64 + head * 0.18) * vBody.a;
    vec3 rgb = vBody.rgb * alpha + vec3(0.80, 0.93, 1.0) * glint * vBody.a * 0.19;
    rgb += vGlow.rgb * glow * vGlow.a;
    return vec4(rgb, alpha);
}

vec4 snow(vec2 p, bool resting) {
    float r = length(p);
    float soft = exp(-r * r * 3.5) * (1.0 - smoothstep(0.65, 1.15, r));
    float detailed = crystal(p, resting ? vShape.z * 6.2831853 : vShape.y);
    float shape = mix(soft, detailed, smoothstep(2.5, 7.5, vSize.x));
    float glint = pow(max(0.0, sin(WeatherScreen.z * 1.9 + vShape.z * 72.0)), 22.0) * 0.12;
    float alpha = shape * vBody.a;
    vec3 rgb = vBody.rgb * alpha * (1.0 + glint) + vGlow.rgb * soft * vGlow.a;
    return vec4(rgb, alpha);
}

vec4 flame(vec2 p, bool spark) {
    float along = clamp(p.y * 0.5 + 0.5, 0.0, 1.0);
    float flutter = sin(along * 12.0 + WeatherScreen.z * 14.0 + vShape.z * 20.0) * along * 0.16;
    float x = p.x + flutter;
    float width = mix(0.80, 0.10, along);
    float body = exp(-x * x / (width * width)) * pow(max(0.0, 1.0 - p.y * p.y), 0.7);
    float heat = exp(-x * x * 9.0 - pow((p.y + 0.52) * 2.1, 2.0));
    float halo = exp(-x * x * 0.37) * pow(max(0.0, 1.0 - p.y * p.y), 1.2);
    vec3 hot = mix(vec3(1.0, 0.27, 0.035), vBody.rgb, 1.0 - along * 0.8);
    vec3 rgb = hot * body * vBody.a + vec3(1.0, 0.93, 0.65) * heat * vBody.a * 0.48;
    rgb += vGlow.rgb * halo * vGlow.a * (spark ? 0.72 : 0.95);
    return vec4(rgb, body * vBody.a * 0.68);
}

vec4 ripple(vec2 p) {
    float r = length(p), age = vShape.y;
    float aa = max(fwidth(r), 0.01);
    if (vShape.w < 0.5) {
        // A drop's ring on wet glass: a soft crest that catches the light, the water just inside it a little darker
        // (it mirrors a steeper, dimmer part of the sky), and a fainter second ring that follows.
        float width = 0.03 + aa;
        float crest = exp(-pow((r - 0.82) / width, 2.0));
        float trough = exp(-pow((r - 0.72) / (width * 1.8), 2.0));
        float second = exp(-pow((r - 0.52) / (width * 0.8), 2.0)) * 0.4 * (1.0 - age);
        float gleam = 0.75 + 0.25 * sin(atan(p.y, p.x) + vShape.z * 6.28);
        float light = (crest + second) * gleam * vBody.a;
        float shade = trough * vBody.a * 0.35;
        return vec4(vBody.rgb * light + vGlow.rgb * crest * vGlow.a, light * 0.55 + shade);
    }
    float ring = 1.0 - smoothstep(0.028, 0.028 + aa, abs(r - 0.82));
    float secondary = (1.0 - smoothstep(0.016, 0.016 + aa, abs(r - 0.53))) * 0.35 * (1.0 - age);
    float cover = ring + secondary;
    float gleam = 0.6 + 0.4 * sin(atan(p.y, p.x) + vShape.z * 6.28);
    float halo = exp(-pow((r - 0.78) * 8.0, 2.0));
    if (vShape.w > 0.5) {
        // Molten impacts have an irregular cooling rim and a warm pool of emitted light in their centre.
        float distort = 0.03 * sin(atan(p.y, p.x) * 7.0 + vShape.z * 15.0);
        cover = exp(-pow((r + distort - 0.73) * 19.0, 2.0));
        halo = exp(-r * r * 3.0) * (1.0 - smoothstep(0.7, 1.0, r));
    }
    float alpha = cover * vBody.a * gleam;
    return vec4(vBody.rgb * alpha + vGlow.rgb * halo * vGlow.a, alpha);
}

vec4 bolt(vec2 p) {
    float core = exp(-p.x * p.x * 3.2);
    float electric = exp(-abs(p.x) * 1.5);
    float bloom = exp(-p.x * p.x * 0.16) * (1.0 - smoothstep(5.0, 7.0, abs(p.x)));
    // Connected segments overlap a little, so a sharply angled branch never leaves a square break.
    float end = 1.0 - smoothstep(0.95, 1.0, abs(p.y));
    vec3 rgb = vBody.rgb * core * vBody.a * 2.15 + vGlow.rgb * (electric * 0.75 + bloom * 0.46) * vGlow.a;
    return vec4(rgb * end, core * vBody.a * 0.68 * end);
}

void main() {
#ifdef MASKED
    if (texelFetch(HandMaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    int kind = int(vShape.x + 0.5);
    vec4 color;
    if (kind == 0) color = rain(vLocal);
    else if (kind == 1) color = snow(vLocal, false);
    else if (kind == 2 || kind == 5) color = flame(vLocal, kind == 5);
    else if (kind == 3) color = ripple(vLocal);
    else if (kind == 4) color = snow(vLocal, true);
    else color = bolt(vLocal);

    float scene = linearDepth(texelFetch(DepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float mine = linearDepth(gl_FragCoord.z);
    float visibility = kind == 3 || kind == 4 ? step(mine, scene + 0.027)
        : clamp((scene - mine) / (kind == 6 ? 0.05 : WeatherDepth.z), 0.0, 1.0);
    color *= visibility * vFade;
    float brightness = max(color.r, max(color.g, color.b));
    if (brightness < 0.5 / 255.0 && color.a < 0.5 / 255.0) discard;
    // Fabulous keeps additive light only if its layer has a trace of coverage.
    color.a = max(color.a, 1.0 / 255.0);
    fragColor = max(color, vec4(0.0));
}
