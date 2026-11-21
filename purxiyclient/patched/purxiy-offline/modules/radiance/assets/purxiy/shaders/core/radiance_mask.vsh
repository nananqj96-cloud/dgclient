#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// The silhouettes of what radiates (see dev.purxiy.radiance.RadianceMasks): the very triangles vanilla drew them with,
// tested against the scene's depth (the pipeline's depth bias keeps their own surface in front of itself).

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
}
