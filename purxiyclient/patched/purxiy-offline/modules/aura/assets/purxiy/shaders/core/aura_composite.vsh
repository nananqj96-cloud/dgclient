#version 330

// One pair of triangles over the whole screen: the aura's layer laid over the scene.
void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : -1.0, i == 2 || i == 4 || i == 5 ? 1.0 : -1.0);
    gl_Position = vec4(corner, 0.0, 1.0);
}
