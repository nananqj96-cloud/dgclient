#version 330

#moj_import <purxiy:sky.glsl>

// One triangle that covers the screen; the fragment shader paints the sky from the view ray.
out vec3 vRay;

void main() {
    vec2 ndc = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2) * 2.0 - 1.0;
    gl_Position = vec4(ndc, 0.0, 1.0);
    // SkyRays is worked out from the level's camera in double precision on the CPU; the ray is linear across the
    // screen, so interpolating it is exact
    vRay = mat3(SkyRays) * vec3(ndc, 1.0);
}
