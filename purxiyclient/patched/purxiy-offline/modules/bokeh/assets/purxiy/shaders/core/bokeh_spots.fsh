#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh: the points of light that become discs of their own. Per cell (2 x 2 texels of the working grid): the light in it
// that stands out above its neighbourhood (a lamp, a star, sky through leaves; not the bright edge of a large bright
// area), in the light's own colour, summed over the cell's pixels, with the blur of the light itself (from its own depth,
// not from what may stand in front of it) in alpha. With a shader pack, a mask keeps lights on your hand out.

uniform sampler2D SceneSampler;
uniform sampler2D PrepSampler;
uniform sampler2D DepthSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

// How bright a colour is as light: its brightest channel, so a coloured lamp counts as much as a white one.
float bokehValue(vec3 c) {
    return max(c.r, max(c.g, c.b));
}

void main() {
    ivec2 cell = ivec2(gl_FragCoord.xy);
    int f = int(BkScreen.z);
    int span = 2 * f;
    // the neighbourhood: 8 x 8 texels of the grid around the cell, in 16 filtered taps; and the wider context, 32 x 32
    vec2 texel = 1.0 / BkGrid.xy;
    float mean = 0.0, wide = 0.0;
    for (int y = 0; y < 4; y++) {
        for (int x = 0; x < 4; x++) {
            vec2 tap = vec2(x, y) * 2.0 - 2.0;
            mean += bokehValue(textureLod(PrepSampler, (vec2(cell * 2) + tap) * texel, 0.0).rgb);
            wide += bokehValue(textureLod(PrepSampler, (vec2(cell * 2) + tap * 5.0 - 3.0) * texel, 0.0).rgb);
        }
    }
    mean /= 16.0;
    wide /= 16.0;
    // a light is bright for how bright the scene around it is: at night a lamp stands out at three quarters of white, in
    // daylight only what is as bright as the sky does (sky through a gap in the leaves, not a birch in the sun)
    float threshold = clamp(2.2 * wide + mix(0.45, 0.2, BkLook.x), 0.4, 0.96);
    // and it stands out above the scene around it (the open sky beside a treetop does not), and a little above its own
    // surroundings, so a lamp as big as a block still is one
    float floorLevel = max(max(0.9 * mean + 0.05, 1.2 * wide + 0.25), threshold - 0.3);

    ivec2 base = cell * span;
    ivec2 last = ivec2(BkScreen.xy) - 1;
    vec3 light = vec3(0.0);
    float total = 0.0;
    float peak = 0.0;
    ivec2 peakAt = base;
    for (int y = 0; y < span; y++) {
        for (int x = 0; x < span; x++) {
            ivec2 p = min(base + ivec2(x, y), last);
            vec3 c = texelFetch(SceneSampler, p, 0).rgb;
            float l = bokehValue(c);
            float e = max(l - floorLevel, 0.0);
            light += c * (e / max(l, 1.0e-3));
            total += e;
            if (l > peak) {
                peak = l;
                peakAt = p;
            }
        }
    }
    vec2 at = vec2(peakAt) + 0.5;
    float coc = bokehCoc(bokehDepth(at, texelFetch(DepthSampler, peakAt, 0).r), at.y / BkScreen.y);
#ifdef MASKED
    if (texelFetch(HandMaskSampler, peakAt, 0).r > 0.5) {
        coc = 0.0;
    }
#endif
    if (peak < threshold || total <= 0.0 || abs(coc) < 2.5) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.5);
        return;
    }
    fragColor = vec4(sqrt(clamp(light / float(span * span), 0.0, 1.0)), bokehEncode(coc));
}
