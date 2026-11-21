#version 330

layout(std140) uniform ProfileAvatar {
    mat4 ViewProj;
    mat4 View;
    vec4 Build; // x = scan height (model pixels above the feet), y = band width, z = time (s), w = hover (0..1)
    vec4 Rim;   // rgb = rim light colour, a = strength
};

in vec3 Position; // model pixels, feet at y = 0, facing +Z (already posed and turned)
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 vUv;
out vec3 vNormal;
out float vHeight;
out float vLayer;
out vec3 vTint;

void main() {
    gl_Position = ViewProj * vec4(Position, 1.0);
    vUv = UV0;
    vNormal = mat3(View) * Normal;
    vHeight = Position.y;
    vLayer = Color.a;
    vTint = Color.rgb;
}
