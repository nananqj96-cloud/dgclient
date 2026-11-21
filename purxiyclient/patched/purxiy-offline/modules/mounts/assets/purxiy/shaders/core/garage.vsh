#version 330

#ifndef STUDIO
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#else
layout(std140) uniform StudioCamera {
    mat4 StudioView;      // car space to view space
    mat4 StudioProjection;
};
#endif

layout(std140) uniform GarageFrame {
    mat4 Parts[24];
    vec4 Sun;
    vec4 SunColor;
    vec4 SkyTop;
    vec4 SkyEdge;
    vec4 Levels;
    vec4 Paint;
    vec4 Cabin;
    vec4 LampA;
    vec4 LampB;
    vec4 Target;
    vec4 Opening;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

// the depth-first pass and the shading pass must land on exactly the same depth
invariant gl_Position;

out vec3 vWorld;
out vec3 vNormal;
out vec3 vLocal;
out vec2 vUv;
out float vAo;
out float vMask;
flat out int vLamp;

// The normal arrives octahedral at 16 bits a channel, in the four colour bytes.
vec3 decodeNormal(vec4 c) {
    ivec4 b = ivec4(c * 255.0 + 0.5);
    vec2 e = vec2(float(b.r * 256 + b.g), float(b.b * 256 + b.a)) / 65535.0 * 2.0 - 1.0;
    vec3 n = vec3(e.xy, 1.0 - abs(e.x) - abs(e.y));
    if (n.z < 0.0) n.xy = (1.0 - abs(n.yx)) * vec2(n.x >= 0.0 ? 1.0 : -1.0, n.y >= 0.0 ? 1.0 : -1.0);
    return normalize(n);
}

void main() {
    int part = clamp(int(Normal.x * 127.0 + 0.5), 0, 23);
    int lampData = int(Normal.z * 127.0 + 0.5);
    mat4 M = Parts[part];
    vec4 world = M * vec4(Position, 1.0);
    vWorld = world.xyz;
    vLocal = Position;
    vNormal = mat3(M) * decodeNormal(Color);
    vUv = UV0;
    vAo = clamp(Normal.y * 127.0 / 127.0, 0.0, 1.0);
    vLamp = lampData / 16;
    vMask = float(lampData % 16) / 15.0;
#ifdef STUDIO
    gl_Position = StudioProjection * StudioView * world;
#else
    gl_Position = ProjMat * ModelViewMat * world;
#endif
}
