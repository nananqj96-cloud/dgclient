#version 330

#moj_import <purxiy:radiance.glsl>

// The distance field at full resolution: of the edges the coarse field knows the way to around a pixel, and of the edge
// pixels right next to it, the nearest. Near the edges, where the line is drawn, that is exact; further out the coarse
// field is within a fraction of a pixel, which the soft light never shows.

uniform sampler2D MaskSampler;
uniform sampler2D FloodSampler;

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    ivec2 limit = ivec2(RadianceScreen.xy) - 1;
    float reach = RadianceSecond.w;
    float best = reach * reach + 1.0;
    vec4 result = vec4(0.0);
    // the edge pixels right around it, from the slots of the 5x5 pixels around it
    int slots[25];
    for (int i = 0; i < 25; i++) {
        slots[i] = radianceSlotAt(MaskSampler, p + ivec2(i % 5, i / 5) - 2, limit);
    }
    for (int y = 1; y <= 3; y++) {
        for (int x = 1; x <= 3; x++) {
            int slot = slots[y * 5 + x];
            float distance = float((x - 2) * (x - 2) + (y - 2) * (y - 2));
            if (slot != 0 && distance < best && (radianceEnds(slot, slots[y * 5 + x + 1]) || radianceEnds(slot, slots[y * 5 + x - 1])
                || radianceEnds(slot, slots[y * 5 + x + 5]) || radianceEnds(slot, slots[y * 5 + x - 5]))) {
                best = distance;
                result = radiancePack(vec2(x - 2, y - 2), slot);
            }
        }
    }
    // the four coarse pixels nearest to it
    ivec2 coarseLimit = radianceCoarseLimit();
    ivec2 first = (p - 1) / 2;
    for (int i = 0; i < 4; i++) {
        ivec2 coarse = clamp(first + ivec2(i & 1, i >> 1), ivec2(0), coarseLimit);
        vec4 texel = texelFetch(FloodSampler, coarse, 0);
        if (texel.w < 0.5) {
            continue;
        }
        vec2 offset = radianceOffset(texel) + vec2(coarse * 2 - p);
        if (max(abs(offset.x), abs(offset.y)) > 126.0) {
            continue;
        }
        float distance = dot(offset, offset);
        if (distance < best) {
            best = distance;
            result = radiancePack(offset, radianceByte(texel.z));
        }
    }
    fragColor = result;
}
