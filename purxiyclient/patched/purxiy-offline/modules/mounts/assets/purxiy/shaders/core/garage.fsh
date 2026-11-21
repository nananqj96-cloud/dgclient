#version 330

#ifndef STUDIO
#moj_import <minecraft:fog.glsl>
#endif

layout(std140) uniform GarageFrame {
    mat4 Parts[24];
    vec4 Sun;        // xyz towards the sun, w strength
    vec4 SunColor;   // rgb, w exposure
    vec4 SkyTop;     // rgb, w build line red
    vec4 SkyEdge;    // rgb, w build line green
    vec4 Levels;     // x sky light where the ride stands, y block light, z rain, w seconds
    vec4 Paint;      // rgb, w flake amount
    vec4 Cabin;      // the light inside after dark, w build line blue
    vec4 LampA;      // running lights, headlights, tail, brake
    vec4 LampB;      // indicator left, indicator right, reverse, interior
    vec4 Target;     // x 1 / width, y 1 / height, z supersampling, w display glow
    vec4 Opening;    // x clear, y solid, z on, w inside
};

layout(std140) uniform GarageMaterial {
    vec4 MatBase;
    vec4 MatPbr;      // metal, roughness, clear coat, normal strength
    vec4 MatEmissive; // rgb, gain
    vec4 MatFlags;    // class, paint tint, alpha mode, cutoff
};

#ifndef STUDIO
uniform sampler2D SceneDepth;
#endif
uniform samplerCube Env;
uniform sampler2D BaseTex;
uniform sampler2D MrTex;
uniform sampler2D NormalTex;

in vec3 vWorld;
in vec3 vNormal;
in vec3 vLocal;
in vec2 vUv;
in float vAo;
in float vMask;
flat in int vLamp;

out vec4 fragColor;

const float PI = 3.14159265;
const int PAINT = 1, GLASS = 2, LENS = 3, CHROME = 4, RUBBER = 5, CARBON = 6, LEATHER = 7, EMISSIVE = 8, DISPLAY = 9;
const float MAX_LOD = 5.0;

float dither() {
    return fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
}

vec3 toLinear(vec3 c) { return pow(c, vec3(2.2)); }

// ---------------------------------------------------------------- the world the ride reflects
vec3 skyColor(vec3 d) {
#ifdef STUDIO
    // the showroom: a dark floor, a soft horizon, a long softbox overhead and a lilac rim light
    vec3 floorColor = SkyEdge.rgb * 0.05;
    vec3 c = mix(floorColor, SkyEdge.rgb * 0.45, smoothstep(-0.30, 0.04, d.y));
    c = mix(c, SkyTop.rgb * 0.8, smoothstep(0.05, 0.6, d.y));
    float strip = smoothstep(0.62, 0.86, d.y) * smoothstep(0.75, 0.25, abs(d.x));
    c += vec3(5.5) * strip;
    float side = smoothstep(0.91, 0.985, dot(d, normalize(vec3(0.85, 0.28, -0.42))));
    c += SunColor.rgb * side * 1.8;
    float side2 = smoothstep(0.93, 0.99, dot(d, normalize(vec3(-0.9, 0.22, 0.32))));
    c += vec3(2.2) * side2;
    return c;
#else
    float h = d.y;
    vec3 c = mix(SkyEdge.rgb, SkyTop.rgb, pow(clamp(h * 1.15, 0.0, 1.0), 0.58));
    float grey = dot(c, vec3(0.3, 0.6, 0.1));
    c = mix(vec3(grey), c, 0.48) * 1.08;
    return mix(c, SkyEdge.rgb * 0.62, smoothstep(0.0, -0.22, h));
#endif
}

vec3 environment(vec3 d, float lod) {
    vec4 e = textureLod(Env, d, lod);
    // premultiplied by how much of the direction is world rather than sky
    return toLinear(e.rgb) + skyColor(d) * e.a;
}

// ---------------------------------------------------------------- microfacet terms
float distributionGGX(float NoH, float a) {
    float a2 = a * a;
    float d = NoH * NoH * (a2 - 1.0) + 1.0;
    return a2 / max(PI * d * d, 1e-5);
}

float visibilitySmith(float NoV, float NoL, float a) {
    float k = a * 0.5;
    float gv = NoV * (1.0 - k) + k;
    float gl = NoL * (1.0 - k) + k;
    return 0.25 / max(gv * gl, 1e-4);
}

vec3 fresnel(vec3 f0, float VoH) {
    float f = pow(1.0 - clamp(VoH, 0.0, 1.0), 5.0);
    return f0 + (1.0 - f0) * f;
}

// Karis' fit of the split-sum environment term
vec3 envBrdf(vec3 f0, float rough, float NoV) {
    const vec4 c0 = vec4(-1.0, -0.0275, -0.572, 0.022);
    const vec4 c1 = vec4(1.0, 0.0425, 1.04, -0.04);
    vec4 r = rough * c0 + c1;
    float a004 = min(r.x * r.x, exp2(-9.28 * NoV)) * r.x + r.y;
    vec2 ab = vec2(-1.04, 1.04) * a004 + r.zw;
    return f0 * ab.x + ab.y;
}

vec3 sunSpecular(vec3 N, vec3 V, vec3 L, float rough, vec3 f0) {
    float a = max(rough * rough, 0.0016);
    vec3 H = normalize(L + V);
    float NoL = max(dot(N, L), 0.0), NoV = max(dot(N, V), 0.001), NoH = max(dot(N, H), 0.0), VoH = max(dot(V, H), 0.0);
    return min(distributionGGX(NoH, a) * visibilitySmith(NoV, NoL, a), 40.0) * fresnel(f0, VoH) * NoL;
}

// ---------------------------------------------------------------- surface
vec3 applyNormalMap(vec3 N, vec3 p, vec2 uv) {
    if (MatPbr.w <= 0.0) return N;
    vec3 map = texture(NormalTex, uv).xyz * 2.0 - 1.0;
    map.xy *= MatPbr.w;
    vec3 dp1 = dFdx(p), dp2 = dFdy(p);
    vec2 duv1 = dFdx(uv), duv2 = dFdy(uv);
    vec3 dp2perp = cross(dp2, N), dp1perp = cross(N, dp1);
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
    float scale = max(dot(T, T), dot(B, B));
    if (scale < 1e-14) return N;
    float inv = 1.0 / sqrt(scale);
    return normalize(mat3(T * inv, B * inv, N) * map);
}

float hash31(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

// Metallic flakes: a fine glitter under the coat that only shows up close and fades before it can shimmer.
float flakes(vec3 p) {
    vec3 q = p * 900.0;
    float footprint = length(dFdx(q)) + length(dFdy(q));
    float fade = 1.0 - smoothstep(0.35, 1.1, footprint);
    if (fade <= 0.0) return 0.0;
    return fade * smoothstep(0.86, 1.0, hash31(floor(q)));
}

vec3 tonemap(vec3 c, float exposure) {
    c *= exposure;
    c = (c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14);
    return pow(clamp(c, 0.0, 1.0), vec3(1.0 / 2.2));
}

float lampLevel() {
    if (vLamp == 1) return LampA.x;
    if (vLamp == 2) return LampA.y;
    if (vLamp == 3) return LampA.z;
    if (vLamp == 4) return LampA.w;
    if (vLamp == 5) return LampB.x;
    if (vLamp == 6) return LampB.y;
    if (vLamp == 7) return LampB.z;
    return 1.0;
}

void main() {
#ifndef STUDIO
    // the level's depth, sampled: what stands in front of the ride hides it, at the screen's own resolution
    float scene = texture(SceneDepth, gl_FragCoord.xy * Target.xy).r;
    if (gl_FragCoord.z > scene + 0.00001) discard;
    // a ride that was just summoned is built from the ground up, behind a line of light
    float built = 0.0;
    float sweep = Levels.w / 0.62;
    if (sweep < 1.0) {
        float top = (1.0 - (1.0 - sweep) * (1.0 - sweep)) * 2.3 - 0.05;
        if (vLocal.y > top) discard;
        built = smoothstep(top - 0.2, top, vLocal.y);
    }
    vec3 buildLine = vec3(SkyTop.a, SkyEdge.a, Cabin.a);
#endif
    vec3 N = normalize(vNormal);
    vec3 V = normalize(-vWorld);
    bool back = !gl_FrontFacing;
    if (Opening.z > 0.0) {
        float near = 1.0 - smoothstep(Opening.x, Opening.y, length(vWorld));
        float opening = near;
        if (back) opening = max(opening, Opening.w);
        if (opening > 0.0 && opening >= dither()) discard;
    }
    if (dot(N, V) < 0.0) N = -N;

    int cls = int(MatFlags.x + 0.5);
    int alphaMode = int(MatFlags.z + 0.5);
    vec4 tex = texture(BaseTex, vUv);
    vec4 baseColor = vec4(toLinear(tex.rgb), tex.a) * MatBase;
    if (alphaMode == 1 && baseColor.a < MatFlags.w) discard;
#ifndef GLASS_PASS
    // blended surfaces with thin alpha in their picture cut out in the solid pass
    if (alphaMode == 0 && tex.a < 0.02) discard;
#endif
#ifdef DEPTH_ONLY
    fragColor = vec4(0.0);
    return;
#endif
    vec4 mr = texture(MrTex, vUv);
    float rough = clamp(MatPbr.y * mr.g, 0.035, 1.0);
    float metal = clamp(MatPbr.x * mr.b, 0.0, 1.0);
    N = applyNormalMap(N, vWorld, vUv);
    float NoV = clamp(dot(N, V), 0.001, 1.0);
    vec3 R = reflect(-V, N);
    float ao = clamp(vAo, 0.0, 1.0);

    float skyLight = Levels.x;
    float rain = Levels.z;
    float sunVisible = Sun.w * smoothstep(0.55, 0.98, skyLight) * (1.0 - 0.85 * rain) * smoothstep(0.18, 0.62, ao);
    vec3 L = normalize(Sun.xyz);
    float NoL = max(dot(N, L), 0.0);
    vec3 sunLight = SunColor.rgb * 1.5 * sunVisible;
    vec3 ambient = environment(N, MAX_LOD);
    ambient += vec3(1.0, 0.72, 0.46) * Levels.y * Levels.y * 0.5;
    float cockpit = Target.w;
    ambient *= mix(mix(0.18, 0.8, cockpit), 1.0, ao);
    // at night the cabin keeps a soft light of its own
    ambient += Cabin.rgb * LampB.w * 0.55 * (cockpit + 0.1);
    float specOcclusion = clamp(pow(NoV + ao, exp2(-16.0 * rough - 1.0)) - 1.0 + ao, 0.0, 1.0);

    vec3 color;
    float alpha = 1.0;
#ifdef GLASS_PASS
    {
        float f = 0.04 + 0.96 * pow(1.0 - NoV, 5.0);
        float a = clamp(baseColor.a, 0.0, 1.0);
        if (cls == GLASS) { a *= mix(1.0, 0.22, cockpit); f *= mix(1.0, 0.5, cockpit); }
        vec3 reflected = tonemap(environment(R, 0.15), SunColor.w);
        vec3 tintColor = tonemap(baseColor.rgb * (ambient + sunLight * NoL) * (cls == LENS ? 1.4 : 1.0), SunColor.w);
        vec3 spark = tonemap(sunSpecular(N, V, L, 0.05, vec3(0.04)) * sunLight, SunColor.w);
        float lamp = vLamp > 0 ? lampLevel() : 0.0;
        vec3 glow = tonemap(MatEmissive.rgb * MatEmissive.w * lamp * vMask * 4.0, SunColor.w);
        vec3 rgb = reflected * f + tintColor * a * (1.0 - f) + spark + glow;
        float outAlpha = clamp(f + a * (1.0 - f), 0.0, 1.0);
#ifndef STUDIO
        float fog = total_fog_value(fog_spherical_distance(vWorld), fog_cylindrical_distance(vWorld),
            FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
        rgb *= (1.0 - fog);
        outAlpha *= (1.0 - fog);
        rgb = mix(rgb, buildLine, built);
        outAlpha = max(outAlpha, built);
#endif
        fragColor = vec4(rgb, outAlpha);
        return;
    }
#else
    if (cls == PAINT) {
        vec3 paint = mix(baseColor.rgb, Paint.rgb, clamp(MatFlags.y, 0.0, 1.0));
        float flake = flakes(vLocal);
        vec3 under = paint * (ambient + sunLight * NoL);
        // the pigment's metal: a wider, tinted sheen below the coat
        vec3 sheenF0 = mix(vec3(0.05), paint, 0.72) * Paint.w;
        under += environment(R, 2.6) * envBrdf(sheenF0, 0.42, NoV) * specOcclusion;
        under += sunSpecular(N, V, L, 0.38, sheenF0) * sunLight * (1.0 + flake * 8.0);
        float f = 0.04 + 0.96 * pow(1.0 - NoV, 5.0);
        vec3 coat = environment(R, 0.35) * f * specOcclusion;
        coat += sunSpecular(N, V, L, 0.05, vec3(0.04)) * sunLight;
        color = under * (1.0 - f) + coat;
    } else {
        vec3 albedo = baseColor.rgb;
        if (cls == CHROME) { metal = max(metal, 0.6); }
        if (cls == RUBBER) { rough = max(rough, 0.72); }
        if (cls == CARBON) {
            // the weave in the texture, sealed under the same gloss as the paint
            metal = 0.0;
            rough = min(rough, 0.45);
        }
        vec3 f0 = mix(vec3(0.04), albedo, metal);
        vec3 diffuse = albedo * (1.0 - metal);
        color = diffuse * (ambient + sunLight * NoL);
        color += environment(R, rough * MAX_LOD) * envBrdf(f0, rough, NoV) * specOcclusion;
        color += sunSpecular(N, V, L, rough, f0) * sunLight;
        if (MatPbr.z > 0.0) {
            float f = (0.04 + 0.96 * pow(1.0 - NoV, 5.0)) * MatPbr.z;
            color = color * (1.0 - f) + environment(R, 0.45) * f * specOcclusion;
            color += sunSpecular(N, V, L, 0.06, vec3(0.04)) * sunLight * MatPbr.z;
        }
    }
    // lamps and screens
    float emissiveAmount = max(max(MatEmissive.r, MatEmissive.g), MatEmissive.b);
    if (cls == EMISSIVE || cls == DISPLAY || emissiveAmount > 0.0) {
        vec3 tint = emissiveAmount > 0.0 ? MatEmissive.rgb : vec3(1.0);
        float level = lampLevel();
        vec3 glow;
        if (vLamp == 3) glow = vec3(1.0, 0.03, 0.02) * (level * 0.35 + LampA.w * 1.0);
        else if (vLamp == 4) glow = vec3(1.0, 0.03, 0.02) * level;
        else if (vLamp == 5 || vLamp == 6) glow = vec3(1.0, 0.36, 0.02) * level;
        else if (vLamp == 7) glow = vec3(1.0, 0.97, 0.9) * level;
        else glow = tint * level;
        color += glow * vMask * MatEmissive.w * (cls == DISPLAY ? 1.0 : cls == EMISSIVE ? 9.0 : 0.8);
    }
#endif
#ifndef STUDIO
    float fog = total_fog_value(fog_spherical_distance(vWorld), fog_cylindrical_distance(vWorld),
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    vec3 mappedColor = tonemap(color, SunColor.w);
    mappedColor = mix(mappedColor, FogColor.rgb, fog);
    mappedColor = mix(mappedColor, buildLine, built);
#else
    vec3 mappedColor = tonemap(color, SunColor.w);
#endif
    // the eye opens up in the cabin: blacks keep their grain instead of drowning
    if (cockpit > 0.0) mappedColor = pow(mappedColor, vec3(mix(1.0, 0.8, cockpit)));
    mappedColor += (dither() - 0.5) / 255.0;
    fragColor = vec4(mappedColor, 1.0);
}
