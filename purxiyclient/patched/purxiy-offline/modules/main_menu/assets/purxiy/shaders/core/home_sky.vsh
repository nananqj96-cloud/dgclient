#version 330

#moj_import <purxiy:home.glsl>

// One triangle over the whole picture; the fragment shader paints the sky along each view ray.
out vec3 vRay;

void main() {
    vec2 ndc = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2) * 2.0 - 1.0;
    gl_Position = vec4(ndc, 0.0, 1.0);
    // points on a plane of equal depth vary linearly over the screen, so interpolating the ray is exact
    vec4 near = InvViewProj * vec4(ndc, -1.0, 1.0);
    vec4 far = InvViewProj * vec4(ndc, 1.0, 1.0);
    vRay = far.xyz / far.w - near.xyz / near.w;
}
