#version 330

#moj_import <minecraft:fog.glsl>

layout(std140) uniform PurxiyMount {
    mat4 MountModel;
    vec4 MountSun;
    vec4 MountEnvironment;
    // x clear within, y solid from (blocks from the eye), z 1 = open around the eye, w how deep the eye is inside
    vec4 MountOpening;
};

in vec3 vPosition;
in vec3 vSurface;
in vec3 vNormal;
in vec4 vColor;
in vec2 vMaterial;
out vec4 fragColor;

// One inexpensive, elongated value-noise cell; no textures, layers or camera-space swimming.
float coatHash(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float coatGrain(vec3 surface) {
    vec3 p = surface * vec3(145.0, 32.0, 145.0);
    float footprint = max(length(dFdx(p)), length(dFdy(p)));
    float visible = 1.0 - smoothstep(0.45, 1.55, footprint);
    if (visible <= 0.0) return 1.0;
    vec3 cell = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float nearLayer = mix(mix(coatHash(cell), coatHash(cell + vec3(1,0,0)), f.x),
        mix(coatHash(cell + vec3(0,1,0)), coatHash(cell + vec3(1,1,0)), f.x), f.y);
    float farLayer = mix(mix(coatHash(cell + vec3(0,0,1)), coatHash(cell + vec3(1,0,1)), f.x),
        mix(coatHash(cell + vec3(0,1,1)), coatHash(cell + vec3(1,1,1)), f.x), f.y);
    return 1.0 + (mix(nearLayer, farLayer, f.z) - 0.5) * 0.12 * visible;
}

// Interleaved gradient noise: a fine, even screen-space dither, so the opening reads as a soft fade, not a pattern.
float openingDither() {
    return fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
}

void main() {
    float fog = total_fog_value(fog_spherical_distance(vPosition), fog_cylindrical_distance(vPosition),
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
#ifdef CONTACT_SHADOW
    float r2 = dot(vMaterial, vMaterial);
    float alpha = exp(-r2 * 3.8) * (1.0 - smoothstep(0.55, 1.0, r2)) * vColor.a * (1.0 - fog);
    if (alpha < 0.003) discard;
    fragColor = vec4(0.015, 0.021, 0.033, alpha);
#else
    vec3 N = normalize(vNormal);
    vec3 V = normalize(-vPosition);
    // Close to the eye the ride dissolves instead of being sliced by the near plane: you can walk right into it.
    // With the eye inside the body its shell is seen from within: those back faces go too, as culling would.
    float opening = MountOpening.z * (1.0 - smoothstep(MountOpening.x, MountOpening.y, length(vPosition)));
    if (dot(N, V) < 0.0) opening = max(opening, MountOpening.w);
    if (opening > 0.0 && opening >= openingDither()) discard;
    // Geometry is deliberately double sided, so thin fins and mane ribbons shade naturally from below too.
    if (dot(N, V) < 0.0) N = -N;
    vec3 L = normalize(MountSun.xyz);
    vec3 H = normalize(L + V);
    float day = MountSun.w;
    float sky = MountEnvironment.x;
    float block = MountEnvironment.y;
    float rain = MountEnvironment.z;
    // Exact packed tag shared with MountMesh.FUR_MATERIAL; ordinary .03 materials stay unchanged.
    bool fur = abs(vMaterial.y - 0.03125) < 0.0001;
    float metal = fur ? 0.0 : clamp(vMaterial.y, 0.0, 1.0);
    float emission = clamp(vMaterial.x, 0.0, 1.0);
    float hemisphere = N.y * 0.5 + 0.5;
    vec3 ambient = mix(vec3(0.065, 0.060, 0.075), vec3(0.22, 0.28, 0.37), hemisphere);
    ambient *= 0.25 + sky * mix(0.30, 1.5, day);
    ambient += vec3(1.0, 0.70, 0.43) * block * block * 0.65;
    vec3 sunlight = mix(vec3(0.23, 0.31, 0.50), vec3(1.0, 0.92, 0.79), day);
    float direct = max(dot(N, L), 0.0) * sky * mix(0.15, 0.85, day) * (1.0 - rain * 0.6);
    vec3 base = vColor.rgb;
    if (fur) base *= coatGrain(vSurface);
    vec3 diffuse = base * (ambient + sunlight * direct);
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 5.0);
    vec3 specularColor = mix(vec3(0.045), mix(vec3(0.68), base, 0.4), metal);
    float exponent = mix(14.0, 100.0, metal);
    float specular = pow(max(dot(N, H), 0.0), exponent) * (0.14 + metal * 1.3) * sky;
    vec3 reflection = mix(vec3(0.04, 0.055, 0.075), vec3(0.33, 0.46, 0.62), smoothstep(-0.2, 0.7, reflect(-V, N).y));
    vec3 color = diffuse + sunlight * specular * specularColor;
    color += reflection * (specularColor + fresnel * (1.0 - specularColor)) * metal * (0.2 + sky * day);
    if (fur) {
        // Short fur scatters at grazing angles instead of producing a smooth plastic highlight.
        float softDirect = max((dot(N, L) + 0.12) / 1.12, 0.0) * sky * mix(0.15, 0.85, day) * (1.0 - rain * 0.6);
        float nap = pow(1.0 - max(dot(N, V), 0.0), 2.0);
        color = base * (ambient + sunlight * softDirect);
        color += mix(base, vec3(0.63), 0.18) * nap * 0.10 * sky * (0.4 + 0.6 * max(dot(N, L), 0.0));
    }
    color = mix(color, base * 1.3 + vec3(0.12) * emission, emission);
    color = mix(color, FogColor.rgb, fog);
    fragColor = vec4(color, 1.0);
#endif
}
