#version 330

#moj_import <purxiy:jump_circles.glsl>

out vec2 local;
out vec3 world;
flat out int ring;

// Six vertices per ring: a square on the ground around it, as far as its glow reaches.
void main() {
    ring = gl_VertexID / 6;
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : -1.0, i == 2 || i == 4 || i == 5 ? 1.0 : -1.0);
    vec4 centre = CircleRings[ring * 2];
    local = corner * (centre.w + CircleRings[ring * 2 + 1].x + CircleScreen.w);
    world = centre.xyz + vec3(local.x, 0.0, local.y);
    gl_Position = CircleViewProj * vec4(world, 1.0);
}
