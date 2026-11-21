#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
layout(std140) uniform PorscheSurface { mat4 SurfaceModel; vec4 SurfaceMaterial; };
in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;
out vec2 texCoord;
out vec3 surfacePosition;
out vec3 surfaceNormal;
void main() {
    vec4 p=SurfaceModel*vec4(Position,1.0);
    surfacePosition=p.xyz;
    surfaceNormal=mat3(SurfaceModel)*Normal;
    texCoord=UV0;
    gl_Position=ProjMat*ModelViewMat*p;
}
