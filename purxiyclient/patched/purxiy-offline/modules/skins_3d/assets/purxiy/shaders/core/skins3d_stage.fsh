#version 330

layout(std140) uniform SkinStage {
    mat4 ViewProj;
    mat4 View;
    mat4 Parts[7];
    vec4 Depth[2];
    vec4 Rim;
    vec4 Params;
};

uniform sampler2D Sampler0;

in vec2 vUv;
in vec3 vNormal;
in float vShade;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUv);
    if (tex.a < 0.1) {
        discard;
    }
    // the profile's studio: a soft key from the upper left, a fill from the right, the brand's lilac from behind
    vec3 N = normalize(vNormal);
    vec3 key = normalize(vec3(-0.46, 0.66, 0.60));
    vec3 fill = normalize(vec3(0.78, 0.18, 0.40));
    float diffuse = 0.58 + 0.38 * max(dot(N, key), 0.0) + 0.10 * max(dot(N, fill), 0.0);
    float rim = pow(1.0 - clamp(N.z, 0.0, 1.0), 2.2) * (0.55 + 0.45 * max(N.y, 0.0));
    // corners where the layer meets the body are a little darker: the contact shadow of the blocks
    vec3 color = tex.rgb * diffuse * vShade;
    color += Rim.rgb * rim * (Rim.a + Params.y * 0.10) * mix(0.6, 1.0, vShade);
    color += tex.rgb * max(N.y, 0.0) * 0.05;
    fragColor = vec4(color, 1.0);
}
