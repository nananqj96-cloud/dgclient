#version 330

#moj_import <purxiy:home.glsl>

#ifndef CLOUDS
uniform sampler2D Sampler0;
#endif

in vec2 vUv;
in vec3 vColor;
in vec3 vWorld;
in float vFog;
flat in int vKind;

out vec4 fragColor;

const int LEAF = 253, GLOW = 248;

void main() {
#ifdef CLOUDS
    float a = CloudColor.a * (1.0 - vFog);
    vec3 col = CloudColor.rgb * vColor;
    fragColor = vec4(col * a, a);
#elif defined(WATER)
    vec4 tex = texture(Sampler0, vUv);
    vec3 view = normalize(vWorld - Camera.xyz);
    vec3 r = reflect(view, vec3(0.0, 1.0, 0.0));
    // the sky on the water, the more the flatter you look across it
    float fresnel = pow(1.0 - clamp(-view.y, 0.0, 1.0), 5.0);
    vec3 base = tex.rgb * vColor * (Light.rgb + Ambient.rgb);
    vec3 col = mix(base, homeSky(r), 0.22 + 0.6 * fresnel);
    float night = FogRange.w;
    col += vec3(1.0, 0.95, 0.86) * pow(max(dot(r, normalize(SunDir.xyz)), 0.0), 320.0) * 2.2 * (1.0 - night);
    col += vec3(0.72, 0.78, 0.95) * pow(max(dot(r, normalize(MoonDir.xyz)), 0.0), 160.0) * 0.9 * night;
    float a = mix(0.72, 0.94, fresnel);
    col = mix(col, FogColor.rgb, vFog);
    a = mix(a, 1.0, vFog);
    fragColor = vec4(col * a, a);
#else
    vec4 tex = texture(Sampler0, vUv);
    if (tex.a < (vKind == LEAF ? 0.5 : 0.1)) {
        discard;
    }
    vec3 col = tex.rgb * vColor * (Light.rgb + Ambient.rgb);
    if (vKind == GLOW) {
        // a firefly bush lights up after dark
        col = mix(col, tex.rgb * 1.4, FogRange.w);
    }
    fragColor = vec4(mix(col, FogColor.rgb, vFog), 1.0);
#endif
}
