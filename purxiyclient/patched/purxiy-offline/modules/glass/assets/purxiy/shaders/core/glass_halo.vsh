#version 330

// One triangle over the whole screen (see glass_halo.fsh).

out vec2 uv;

void main() {
    vec2 corner = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    uv = corner;
    gl_Position = vec4(corner * 2.0 - 1.0, 0.0, 1.0);
}
