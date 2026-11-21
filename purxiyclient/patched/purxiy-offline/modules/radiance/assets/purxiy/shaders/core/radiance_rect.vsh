#version 330

#moj_import <purxiy:radiance.glsl>

// Two triangles over each rectangle the passes run over; the fragment shaders work from gl_FragCoord.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    vec4 rect = RadianceRects[gl_VertexID / 6];
    gl_Position = vec4(mix(rect.xy, rect.zw, corner), 0.0, 1.0);
}
