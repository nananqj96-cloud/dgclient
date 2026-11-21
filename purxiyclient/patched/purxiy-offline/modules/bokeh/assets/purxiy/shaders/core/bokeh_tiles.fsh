#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh: how far the blur has to look. First the largest blur in every tile of 8 x 8 texels; then (DILATE) the largest
// blur that can reach each tile from the tiles around it (a tile only counts if its blur spans the gap in between).
// Stored as a share of the largest blur there can be. HALF instead makes the grid at half its size (2 x 2 texels each,
// the blur of the nearest), which the gather samples where the blur is large.

#ifdef DILATE
uniform sampler2D TileSampler;
#else
uniform sampler2D PrepSampler;
#endif

out vec4 fragColor;

void main() {
    ivec2 t = ivec2(gl_FragCoord.xy);
#if defined(HALF)
    ivec2 last = ivec2(BkGrid.xy) - 1;
    vec3 sum = vec3(0.0);
    float nearest = 1.0;
    for (int i = 0; i < 4; i++) {
        vec4 s = texelFetch(PrepSampler, min(t * 2 + ivec2(i & 1, i >> 1), last), 0);
        sum += s.rgb;
        nearest = min(nearest, s.a);
    }
    fragColor = vec4(sum * 0.25, nearest);
#elif defined(DILATE)
    ivec2 tiles = textureSize(TileSampler, 0);
    // the largest blur, in texels of the grid, and how many tiles it can span
    float scale = BkLens.y / BkScreen.z;
    int span = int(ceil(scale / 8.0));
    float best = texelFetch(TileSampler, t, 0).r;
    for (int y = -span; y <= span; y++) {
        for (int x = -span; x <= span; x++) {
            ivec2 at = t + ivec2(x, y);
            if (at.x < 0 || at.y < 0 || at.x >= tiles.x || at.y >= tiles.y) {
                continue;
            }
            float blur = texelFetch(TileSampler, at, 0).r;
            vec2 gap = max(abs(vec2(x, y)) - 1.0, 0.0) * 8.0;
            if (blur * scale >= length(gap)) {
                best = max(best, blur);
            }
        }
    }
    fragColor = vec4(best, 0.0, 0.0, 1.0);
#else
    ivec2 last = ivec2(BkGrid.xy) - 1;
    float blur = 0.0;
    for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 8; x++) {
            blur = max(blur, abs(bokehDecode(texelFetch(PrepSampler, min(t * 8 + ivec2(x, y), last), 0).a)));
        }
    }
    // rounded up, so the gather never looks short
    fragColor = vec4(min(blur / BkLens.y + 0.5 / 255.0, 1.0), 0.0, 0.0, 1.0);
#endif
}
