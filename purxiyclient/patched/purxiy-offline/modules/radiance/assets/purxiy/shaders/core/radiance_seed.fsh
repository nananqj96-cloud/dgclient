#version 330

#moj_import <purxiy:radiance.glsl>

// Where the coarse field (half resolution) starts: a coarse pixel over an edge of the silhouettes knows the way to it,
// exactly, in pixels of the screen.

uniform sampler2D MaskSampler;

out vec4 fragColor;

void main() {
    ivec2 coarse = ivec2(gl_FragCoord.xy);
    ivec2 limit = ivec2(RadianceScreen.xy) - 1;
    ivec2 base = coarse * 2;
    // the four pixels under it and the ring around them
    int slots[16];
    for (int i = 0; i < 16; i++) {
        slots[i] = radianceSlotAt(MaskSampler, base + ivec2(i & 3, i >> 2) - 1, limit);
    }
    vec4 result = vec4(0.0);
    for (int i = 3; i >= 0; i--) {
        int x = (i & 1) + 1, y = (i >> 1) + 1;
        int slot = slots[y * 4 + x];
        if (slot != 0 && (radianceEnds(slot, slots[y * 4 + x + 1]) || radianceEnds(slot, slots[y * 4 + x - 1])
            || radianceEnds(slot, slots[y * 4 + x + 4]) || radianceEnds(slot, slots[y * 4 + x - 4]))) {
            result = radiancePack(vec2(i & 1, i >> 1), slot);
        }
    }
    fragColor = result;
}
