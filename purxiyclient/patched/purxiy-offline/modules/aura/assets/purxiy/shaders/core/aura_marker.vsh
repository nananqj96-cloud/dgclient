#version 330

#moj_import <purxiy:aura_marker.glsl>

out float angle;
out float height;
out vec3 world;
flat out int ring;

const float TAU = 6.28318530718;

// A band standing on each ring: SEGMENTS quads around it, from below the lowest point of the ring and its tail to above
// the highest, as far as the glow reaches.
void main() {
    int perRing = SEGMENTS * 6;
    ring = gl_VertexID / perRing;
    int v = gl_VertexID - ring * perRing;
    int segment = v / 6;
    int i = v - segment * 6;
    float u = i == 1 || i == 2 || i == 4 ? 1.0 : 0.0;
    float t = i == 2 || i == 4 || i == 5 ? 1.0 : 0.0;
    vec4 base = Markers[ring * 3];
    vec4 band = Markers[ring * 3 + 1];
    float margin = band.z + band.w;
    angle = (float(segment) + u) / float(SEGMENTS);
    height = mix(min(band.x, band.y) - margin, max(band.x, band.y) + margin, t);
    float a = angle * TAU;
    world = base.xyz + vec3(cos(a) * base.w, height, sin(a) * base.w);
    gl_Position = MarkerViewProj * vec4(world, 1.0);
}
