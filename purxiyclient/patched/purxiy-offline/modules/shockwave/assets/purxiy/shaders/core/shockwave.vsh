#version 330

#moj_import <purxiy:shockwave.glsl>

// Six vertices: the rectangle of the screen the moving ground covers.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? ShockRect.z : ShockRect.x, i == 2 || i == 4 || i == 5 ? ShockRect.w : ShockRect.y);
    gl_Position = vec4(corner, 0.0, 1.0);
}
