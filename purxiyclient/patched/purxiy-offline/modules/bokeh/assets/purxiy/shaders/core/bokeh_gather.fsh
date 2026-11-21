#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh: the blur, gathered on the working grid. Every texel looks around itself, as far as the largest blur nearby can
// reach (the dilated tiles), at samples on a golden-angle spiral. A sample counts where its own disc, in the lens's
// shape, covers this texel, weighted by how much of its light lands here (one over its disc's area); bright light
// counts for more, as if it were brighter than white, so it spreads into brighter discs.
//
// Two layers keep edges honest. What lies behind this texel cannot cover it by more than twice its own blur, so a sharp
// object keeps its silhouette against a soft background. What is blurred in front of it, nearer the camera, is gathered
// apart and laid over it by how much it covers it, so a blurred foreground melts over whatever is behind it. Alpha says
// how much of the result is that foreground: the composite keeps it over the sharp picture.

uniform sampler2D PrepSampler;
uniform sampler2D HalfSampler;
uniform sampler2D TileSampler;

out vec4 fragColor;

// the golden angle, as a turn: every sample lies this much further round than the one before
const vec2 GOLDEN = vec2(-0.73736887, 0.67549029);
const int MAX_SAMPLES = 100;

float bokehWeight(vec3 c) {
    float h = smoothstep(0.45, 0.95, max(c.r, max(c.g, c.b)));
    return 1.0 + BkLook.x * 3.0 * h * h;
}

void main() {
    ivec2 g = ivec2(gl_FragCoord.xy);
    ivec2 last = ivec2(BkGrid.xy) - 1;
    float f = BkScreen.z;
    vec4 centre = texelFetch(PrepSampler, g, 0);
    float c0 = bokehDecode(centre.a) / f;
    float reach = texelFetch(TileSampler, g / 8, 0).r * BkLens.y / f;
    if (reach < 0.6) {
        fragColor = vec4(centre.rgb, 0.0);
        return;
    }
    // a large blur samples the grid at half its size: each sample stands for four texels, so a quarter as many do
    bool coarse = reach > 6.0;
    int n = coarse ? clamp(int(reach * reach * 0.15) + 12, 16, 72) : clamp(int(reach * reach * 0.3) + 10, 16, MAX_SAMPLES);
    ivec2 lastHalf = textureSize(HalfSampler, 0) - 1;
    // the samples crowd towards the middle (radius ~ t^0.75 rather than sqrt(t)), so small discs nearby are seen by many
    // of them; each one stands for the area around its radius (over pi): reach^2 * 1.5 * sqrt(t) / n
    float area = bokehLensArea();
    vec2 screen = (vec2(g) + 0.5) / BkGrid.xy;
    float spin = bokehNoise(gl_FragCoord.xy) * 6.2831853;
    vec2 turn = vec2(cos(spin), sin(spin));

    float w0 = bokehWeight(centre.rgb) / (BOKEH_PI * max(c0 * c0, 0.25) * area);
    vec3 farSum = centre.rgb * w0;
    vec3 farWeight = vec3(w0);
    vec3 nearSum = vec3(0.0);
    vec3 nearWeight = vec3(0.0);
    vec3 nearCover = vec3(0.0);
    // what is this much nearer than the centre (and blurred in front of the focus) is foreground
    float front = min(c0, 0.0) - 1.0;
    for (int i = 0; i < n; i++) {
        float t = (float(i) + 0.5) / float(n);
        float r = reach * pow(t, 0.75);
        float share = reach * reach * 1.5 * sqrt(t) / float(n);
        float spacing = max(sqrt(share), 1.0);
        vec2 offset = turn * r;
        turn = vec2(turn.x * GOLDEN.x - turn.y * GOLDEN.y, turn.x * GOLDEN.y + turn.y * GOLDEN.x);
        vec4 s;
        vec2 at;
        if (coarse) {
            ivec2 h = clamp(ivec2(floor((vec2(g) + offset + 0.5) * 0.5)), ivec2(0), lastHalf);
            s = texelFetch(HalfSampler, h, 0);
            at = vec2(h) * 2.0 + 0.5;
        } else {
            ivec2 texel = clamp(ivec2(floor(vec2(g) + offset + 0.5)), ivec2(0), last);
            s = texelFetch(PrepSampler, texel, 0);
            at = vec2(texel);
        }
        float cs = bokehDecode(s.a) / f;
        bool inFront = cs < front;
        float radius = abs(cs);
        if (!inFront && cs > c0) {
            radius = min(radius, 2.0 * abs(c0));
        }
        if (radius < 0.5) {
            continue;
        }
        // where the centre lies in the sample's disc
        vec2 q = (vec2(g) - at) / radius;
        float d = bokehLensGauge(q, screen);
        vec3 edge = d / bokehFringe(BkLook.y, radius * f);
        vec3 cover = clamp((1.0 - edge) * radius / spacing + 0.5, 0.0, 1.0);
        // a disc smaller than the samples' spacing is hit by chance: it counts no more than one the spacing's size
        vec3 w = cover * (share / (max(radius, spacing) * max(radius, spacing) * area)) * bokehWeight(s.rgb);
        // and foreground the gather cannot resolve is barely blurred: it belongs to the picture beneath
        float solid = inFront ? smoothstep(0.75 * spacing, 1.5 * spacing, radius) : 0.0;
        nearCover += cover * (share / (max(radius, spacing) * max(radius, spacing) * area)) * solid;
        nearSum += s.rgb * w * solid;
        nearWeight += w * solid;
        farSum += s.rgb * w * (1.0 - solid);
        farWeight += w * (1.0 - solid);
    }
    vec3 far = farSum / farWeight;
    vec3 alpha = clamp(nearCover, 0.0, 1.0);
    vec3 near = nearSum / max(nearWeight, vec3(1.0e-6));
    fragColor = vec4(mix(far, near, alpha), max(alpha.r, max(alpha.g, alpha.b)));
}
