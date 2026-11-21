#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <purxiy:ambience.glsl>

// One quad per particle (see dev.purxiy.ambience.PurxiyAmbience). All four vertices carry the whole particle; the
// corner comes from the vertex id. Billboards face the camera and are measured in pixels, so the smallest stay round
// and steady instead of flickering; petals are real flat quads that tumble through the world.

in vec4 Center; // xyz = position relative to the camera (blocks), w = radius of the body (blocks)
in vec4 Shape;  // x = shape, y = turn (rad), z = seed, w = shape specific: sparkle flare, petal width, flake glint
in vec4 Orient; // petals: orientation in the world (quaternion)
in vec4 Color;  // body, a = opacity
in vec4 Color2; // glow, a = strength

out vec2 vLocal;      // billboards: pixels from the centre; petals: petal units (the petal spans -1..1)
flat out vec4 vBody;
flat out vec4 vGlow;
flat out vec4 vSize;  // x = radius of the body (px), y = radius of its bokeh (px), z = defocus (0..1), w = shape
flat out vec4 vShape; // x = turn, y = seed, z = shape specific, w = how squarely a petal faces the camera
flat out float vFade; // fog, and less light for particles smaller than the smallest radius drawn

vec3 rotate(vec4 q, vec3 v) {
    return v + 2.0 * cross(q.xyz, cross(q.xyz, v) + q.w * v);
}

void main() {
    int corner = gl_VertexID & 3;
    vec2 c = vec2(corner >= 2 ? 1.0 : -1.0, corner == 1 || corner == 2 ? 1.0 : -1.0);
    int shape = int(Shape.x + 0.5);

    vec4 view = ModelViewMat * vec4(Center.xyz, 1.0);
    float depth = max(-view.z, 0.05);
    float pxPerBlock = ProjMat[1][1] * AmbScreen.y * 0.5 / depth;
    float body = Center.w * pxPerBlock;
    float fog = total_fog_value(fog_spherical_distance(Center.xyz), fog_cylindrical_distance(Center.xyz), FogEnvironmentalStart,
        FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd);

    // depth of field: up close, particles melt into soft discs (petals and flakes stay sharp a little closer, to show
    // their shape)
    float focus = shape == ORB || shape == SPARKLE ? 4.5 : 2.8;
    float defocus = AmbLook.y * (1.0 - smoothstep(1.4, focus, depth));

    vBody = Color;
    vGlow = Color2;
    vShape = vec4(Shape.y, Shape.z, Shape.w, 0.0);

    if (shape == PETAL) {
        // a flat petal, tumbling in the world, with a margin around it for its glow
        vec2 extent = vec2(Shape.w, 1.0) * Center.w * 1.3;
        vec3 world = Center.xyz + rotate(Orient, vec3(c * extent, 0.0));
        gl_Position = ProjMat * ModelViewMat * vec4(world, 1.0);
        vLocal = c * 1.3;
        vShape.w = abs(dot(rotate(Orient, vec3(0.0, 0.0, 1.0)), normalize(Center.xyz)));
        vSize = vec4(body, body, defocus, float(shape));
        // smaller than a couple of pixels, a petal fades rather than shimmers
        vFade = (1.0 - fog) * smoothstep(0.6, 2.2, body);
        return;
    }

    // smaller than the smallest radius drawn, a particle keeps that size and gives less light instead
    float energy = 1.0;
    if (body < AmbScreen.z) {
        energy = (body / AmbScreen.z) * (body / AmbScreen.z);
        body = AmbScreen.z;
    }
    float bokeh = max(body * 1.35, defocus * AmbScreen.y * 0.024);
    vec2 offset = c * (max(max(ambienceReach(shape, body, Shape.w), bokeh), body) + 1.5);
    view.xy += offset / pxPerBlock;
    gl_Position = ProjMat * view;
    vLocal = offset;
    vSize = vec4(body, bokeh, defocus, float(shape));
    vFade = (1.0 - fog) * energy;
}
