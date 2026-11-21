#version 330

#moj_import <purxiy:hand_trail.glsl>

// Two triangles over where the trail can be, at the far plane: the depth test lets them through only where your hand is
// not.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    gl_Position = vec4(mix(TrailRect.xy, TrailRect.zw, corner), 1.0, 1.0);
}
