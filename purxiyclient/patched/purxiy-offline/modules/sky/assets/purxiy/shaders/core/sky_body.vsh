#version 330

#moj_import <purxiy:sky.glsl>

// Two quads over the sky, one for the planet (vertices 0-5) and one for the singularity (6-11), each covering only
// the rectangle of the screen its light can reach. An empty rectangle draws nothing.
out vec3 vRay;
flat out int vBody;

void main() {
    int corner = gl_VertexID % 6;
    vBody = gl_VertexID / 6;
    vec4 rect = vBody == 0 ? SkyPlanetRect : SkyHoleRect;
    vec2 uv = vec2(corner == 1 || corner == 2 || corner == 4 ? 1.0 : 0.0, corner == 2 || corner == 4 || corner == 5 ? 1.0 : 0.0);
    vec2 ndc = mix(rect.xy, rect.zw, uv);
    gl_Position = vec4(ndc, 0.0, 1.0);
    vRay = mat3(SkyRays) * vec3(ndc, 1.0);
}
