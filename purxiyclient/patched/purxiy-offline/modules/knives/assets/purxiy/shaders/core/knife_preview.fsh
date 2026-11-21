#version 330

#moj_import <purxiy:theme.glsl>

uniform sampler2D CamoSampler;
in vec3 vNormal;
in vec4 vColor;
in vec2 vUv;
out vec4 fragColor;

void main() {
    vec3 N = normalize(vNormal);
    if (N.z < 0.0) N = -N;
    vec3 key = normalize(vec3(-0.43, 0.76, 0.48));
    vec3 fill = normalize(vec3(0.74, 0.25, 0.44));
    vec3 halfVector = normalize(key + vec3(0.0, 0.0, 1.0));
    float metal = vColor.a;
    float diffuse = 0.51 + 0.48 * max(dot(N, key), 0.0) + 0.08 * max(dot(N, fill), 0.0);
    float shine = pow(max(dot(N, halfVector), 0.0), mix(24.0, 68.0, metal));
    float rim = pow(1.0 - max(N.z, 0.0), 3.0);
    vec3 base = texture(CamoSampler, vUv).rgb * vColor.rgb;
    vec3 color = base * diffuse;
    color += vec3(0.91, 0.95, 1.0) * shine * (0.028 + metal * 0.44);
    color += prx_theme(vec3(0.72, 0.60, 0.94)) * rim * (0.018 + metal * 0.06);
    fragColor = vec4(color, 1.0);
}
