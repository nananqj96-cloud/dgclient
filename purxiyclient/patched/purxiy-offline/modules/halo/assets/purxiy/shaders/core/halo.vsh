#version 330

#moj_import <purxiy:halo.glsl>

// Six vertices: the rectangle of the screen the halo's light reaches.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    vec2 pixel = mix(HaloRect.xy, HaloRect.zw, corner);
    gl_Position = vec4(pixel / HaloScreen.xy * 2.0 - 1.0, 0.0, 1.0);
}
