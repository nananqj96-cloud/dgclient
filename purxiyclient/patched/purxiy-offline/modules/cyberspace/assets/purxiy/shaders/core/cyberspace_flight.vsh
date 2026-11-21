#version 330

#moj_import <purxiy:cyberspace.glsl>
#moj_import <purxiy:cyberspace_flight.glsl>

// Eighteen vertices per packet or bit: a square facing you around its head (the cube itself is found per pixel, see
// the fragment shader), and a ribbon facing you along each of the two pieces of its trail.
out vec3 position;
out vec2 local;
flat out int object;
flat out int part;

// half the width of a trail's ribbon, its glow included (blocks)
const float RIBBON = 0.3;

void main() {
    object = gl_VertexID / 18;
    part = (gl_VertexID % 18) / 6;
    int corner = gl_VertexID % 6;
    vec2 c = corner == 0 || corner == 3 ? vec2(0.0, 0.0) : corner == 1 ? vec2(1.0, 0.0) : corner == 2 || corner == 4 ? vec2(1.0, 1.0) : vec2(0.0, 1.0);
    vec4 head = FlObjects[object * 4];
    vec4 bend = FlObjects[object * 4 + 1];
    vec4 end = FlObjects[object * 4 + 2];
    vec3 pos;
    if (part == 0) {
        // the cube and its glow
        float extent = head.w * 5.0;
        vec2 k = c * 2.0 - 1.0;
        pos = head.xyz + (FlRight.xyz * k.x + FlUp.xyz * k.y) * extent;
        local = k;
    } else {
        vec3 a = part == 1 ? head.xyz : bend.xyz;
        vec3 b = part == 1 ? bend.xyz : end.xyz;
        vec3 side = cross(b - a, (a + b) * 0.5);
        float span = length(side);
        side = span > 1.0e-6 && distance(a, b) > 1.0e-4 ? side / span : vec3(0.0);
        pos = mix(a, b, c.x) + side * (c.y * 2.0 - 1.0) * RIBBON;
        local = vec2(c.x, c.y * 2.0 - 1.0);
    }
    position = pos;
    gl_Position = FlViewProj * vec4(pos, 1.0);
}
