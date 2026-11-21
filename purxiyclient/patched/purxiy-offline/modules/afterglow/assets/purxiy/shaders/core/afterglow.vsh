#version 330

#moj_import <minecraft:light.glsl>
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <purxiy:afterglow.glsl>

// A dissolving entity (see dev.purxiy.afterglow.PurxiyAfterglow): vanilla's entity vertex stage, lit per face like
// its no-cull types, that also hands the fragment stage where each point sits in the body's own frame.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColorFront;
out vec4 vertexColorBack;
out vec4 lightMapColor;
out vec2 texCoord0;
out vec3 bodyPos;
out vec3 viewPos;
out vec3 viewNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);

#ifdef EMISSIVE
    vertexColorFront = Color;
    vertexColorBack = Color;
    lightMapColor = vec4(1.0);
#else
    vec2 light = minecraft_compute_light(Light0_Direction, Light1_Direction, Normal);
    vertexColorFront = minecraft_mix_light_separate(light, Color);
    vertexColorBack = minecraft_mix_light_separate(-light, Color);
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
#endif

    texCoord0 = UV0;
    bodyPos = afterglowBody(Position - TextureMat[0].xyz, TextureMat[1].y, TextureMat[1].z);
    viewPos = Position;
    viewNormal = Normal;
}
