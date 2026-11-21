#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform PurxiyWeatherParticles {
    vec4 WeatherDepth;
    vec4 WeatherScreen;
    vec4 WeatherExtra;
};

in vec4 Center;
in vec4 Shape;
in vec4 Orient;
in vec4 Color;
in vec4 Color2;

out vec2 vLocal;
flat out vec4 vBody;
flat out vec4 vGlow;
flat out vec4 vShape;
flat out vec2 vSize;
flat out float vFade;

void main() {
    int corner = gl_VertexID & 3;
    vec2 c = vec2(corner >= 2 ? 1.0 : -1.0, corner == 1 || corner == 2 ? 1.0 : -1.0);
    int kind = int(Shape.x + 0.5);
    vec4 view = ModelViewMat * vec4(Center.xyz, 1.0);
    float pxPerBlock = max(0.001, ProjMat[1][1] * WeatherScreen.y * 0.5 / max(-view.z, 0.10));
    float radiusPx = Center.w * pxPerBlock;
    float energy = min(1.0, radiusPx / (kind == 0 ? 0.5 : 0.7));
    float width = max(Center.w, (kind == 0 ? 0.5 : 0.7) / pxPerBlock);
    vBody = Color;
    vGlow = Color2;
    vShape = Shape;
    vSize = vec2(radiusPx, 0.0);
    float fog = total_fog_value(fog_spherical_distance(Center.xyz), fog_cylindrical_distance(Center.xyz),
        FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);
    // Lightning remains visible at the horizon while still respecting the world's render-distance fog.
    vFade = (1.0 - fog) * (kind == 3 || kind == 4 ? 1.0 : energy);

    if (kind == 3 || kind == 4) {
        // Ripples and settled crystals really lie on the top of the block/fluid that caught them.
        vec3 world = Center.xyz + vec3(c.x, 0.0, c.y) * Center.w;
        gl_Position = ProjMat * ModelViewMat * vec4(world, 1.0);
        vLocal = c;
    } else if (kind == 0 || kind == 2 || kind == 5 || kind == 6) {
        vec3 direction = mat3(ModelViewMat) * normalize(Orient.xyz + vec3(0.0, 0.000001, 0.0));
        vec2 tangent = length(direction.xy) > 0.001 ? normalize(direction.xy) : vec2(0.0, 1.0);
        vec2 side = vec2(tangent.y, -tangent.x);
        float reach = kind == 0 ? 2.2 : kind == 6 ? 7.0 : 4.5;
        vec3 offset = direction * (c.y * Shape.y);
        offset.xy += side * (c.x * width * reach);
        view.xyz += offset;
        gl_Position = ProjMat * view;
        vLocal = vec2(c.x * reach, c.y);
        vSize.y = Shape.y * pxPerBlock;
    } else {
        view.xy += c * width * 1.65;
        gl_Position = ProjMat * view;
        vLocal = c * 1.65;
    }
}
