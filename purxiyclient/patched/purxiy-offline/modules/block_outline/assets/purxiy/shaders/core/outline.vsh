#version 330

#moj_import <purxiy:outline.glsl>

// Two triangles over the outline's rectangle on screen; the fragment shader does the rest from gl_FragCoord.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    gl_Position = vec4(mix(OutlineRect.xy, OutlineRect.zw, corner), 0.0, 1.0);
}
