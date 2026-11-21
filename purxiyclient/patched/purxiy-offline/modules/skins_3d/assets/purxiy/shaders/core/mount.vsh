#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform PurxiyMount {
    mat4 MountModel;
    vec4 MountSun;
    vec4 MountEnvironment;
    vec4 MountOpening;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec3 vPosition;
out vec3 vSurface;
out vec3 vNormal;
out vec4 vColor;
out vec2 vMaterial;

void main() {
    vec4 world = MountModel * vec4(Position, 1.0);
    vPosition = world.xyz;
    vSurface = Position;
    vNormal = mat3(MountModel) * Normal;
    vColor = Color;
    vMaterial = UV0;
    gl_Position = ProjMat * ModelViewMat * world;
}
