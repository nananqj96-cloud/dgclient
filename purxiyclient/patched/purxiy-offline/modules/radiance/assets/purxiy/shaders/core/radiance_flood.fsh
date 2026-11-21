#version 330

#moj_import <purxiy:radiance.glsl>

// One step of the jump flood, at half resolution: of the edges its neighbours at this step know the way to, a coarse
// pixel keeps the nearest. Steps halve from the reach down to one pixel, so every coarse pixel within reach of an edge
// ends up knowing the way to it (in pixels of the screen, from twice its position).

uniform sampler2D FloodSampler;

layout(std140) uniform RadianceStep {
    vec4 Step; // x = step (coarse pixels)
};

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    ivec2 limit = radianceCoarseLimit();
    int step = int(Step.x + 0.5);
    float reach = RadianceSecond.w;
    float best = reach * reach + 1.0;
    vec4 result = vec4(0.0);
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            ivec2 q = p + ivec2(x, y) * step;
            if (q.x < 0 || q.y < 0 || q.x > limit.x || q.y > limit.y) {
                continue;
            }
            vec4 texel = texelFetch(FloodSampler, q, 0);
            if (texel.w < 0.5) {
                continue;
            }
            vec2 offset = radianceOffset(texel) + 2.0 * vec2(q - p);
            if (max(abs(offset.x), abs(offset.y)) > 126.0) {
                continue;
            }
            // from the centre of this coarse pixel to the centre of the edge pixel
            vec2 d = offset - 0.5;
            float distance = dot(d, d);
            if (distance < best) {
                best = distance;
                result = radiancePack(offset, radianceByte(texel.z));
            }
        }
    }
    fragColor = result;
}
