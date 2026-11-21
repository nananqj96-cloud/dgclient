#version 330

#moj_import <purxiy:theme.glsl>

layout(std140) uniform ProfileAvatar {
    mat4 ViewProj;
    mat4 View;
    vec4 Build;
    vec4 Rim;
};

uniform sampler2D Sampler0;

in vec2 vUv;
in vec3 vNormal;
in float vHeight;
in float vLayer;
in vec3 vTint;

out vec4 fragColor;

float texelHash(vec2 texel) {
    vec3 p = fract(vec3(texel.xyx) * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

void main() {
    vec4 tex = texture(Sampler0, vUv);
    if (tex.a < 0.1) {
        discard;
    }

    // The model builds up from the feet: every texel has its own moment, so it arrives pixel by pixel under a scan line.
    vec2 texel = floor(vUv * vec2(textureSize(Sampler0, 0)));
    float jitter = (texelHash(texel + vLayer * 17.0) - 0.5) * Build.y * 0.9;
    float lead = Build.x - (vHeight + jitter);
    if (lead < 0.0) {
        discard;
    }
    float band = 1.0 - smoothstep(0.0, Build.y, lead);

    vec3 N = normalize(vNormal);
    vec3 key = normalize(vec3(-0.46, 0.66, 0.60));
    vec3 fill = normalize(vec3(0.78, 0.18, 0.40));
    float diffuse = 0.60 + 0.36 * max(dot(N, key), 0.0) + 0.10 * max(dot(N, fill), 0.0);
    // light from behind in the brand's lilac, on the sides that turn away from you
    float rim = pow(1.0 - clamp(N.z, 0.0, 1.0), 2.2) * (0.55 + 0.45 * max(N.y, 0.0));
    // an outer layer in 3D keeps its contact shadow in the colour (see Skins3D); everything else is white there
    vec3 color = tex.rgb * diffuse * vTint;
    color += Rim.rgb * rim * (Rim.a + Build.w * 0.10);
    // a gentle top light from the sky above
    color += tex.rgb * max(N.y, 0.0) * 0.05;

    vec3 scan = prx_theme(vec3(0.86, 0.55, 1.0));
    color = mix(color, scan, band * 0.75) + scan * band * band * 0.5;
    fragColor = vec4(color, 1.0);
}
