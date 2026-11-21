#version 330

#moj_import <purxiy:home.glsl>

in vec3 Position;  // the particle's centre
in vec2 UV0;
in vec4 Color;
in vec4 Grow;      // xy = this corner (-1..1), z = size (blocks; negative: it glows), w = turn (radians)

out vec2 vUv;
out vec4 vColor;
out float vFog;
out float vGlow;

void main() {
    float size = abs(Grow.z);
    float c = cos(Grow.w), s = sin(Grow.w);
    vec2 corner = vec2(Grow.x * c - Grow.y * s, Grow.x * s + Grow.y * c) * size;
    vec3 p = Position + CamRight.xyz * corner.x + CamUp.xyz * corner.y;
    gl_Position = ViewProj * vec4(p, 1.0);
    vUv = UV0;
    vColor = Color;
    vGlow = Grow.z < 0.0 ? 1.0 : 0.0;
    vFog = smoothstep(FogRange.x, FogRange.y, length(p - Camera.xyz));
}
