#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh, last: the picture put back together at full size. Where a pixel is sharp, the sharp picture; where it is blurred,
// the gathered blur (read from the nine nearest texels of the grid, those blurred like this pixel counting most, so a
// sharp edge does not pick up the blur beside it); in between, the sharp detail fades out. Blurred foreground that the
// gather laid over a pixel stays over it; the discs of light are added on top. With a shader pack, a mask keeps your hand
// as it is.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D PrepSampler;
uniform sampler2D GatherSampler;
uniform sampler2D SpriteSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

void main() {
    ivec2 p = ivec2(gl_FragCoord.xy);
    vec3 sharp = texelFetch(SceneSampler, p, 0).rgb;
#ifdef MASKED
    if (texelFetch(HandMaskSampler, p, 0).r > 0.5) {
        fragColor = vec4(sharp, 1.0);
        return;
    }
#endif
    float coc = bokehCoc(bokehDepth(gl_FragCoord.xy, texelFetch(DepthSampler, p, 0).r), gl_FragCoord.y / BkScreen.y);

    // how blurred this pixel is: sharp detail fades out over the first pixel and a half of blur
    float soft = smoothstep(0.4, 1.6, abs(coc));

    // the nine nearest texels of the grid, under a tent a little wider than a bilinear filter (it smooths the fine grain
    // of the gather), weighted by whether they are as sharp or as blurred as this pixel, so a sharp edge keeps clear of
    // the blur beside it (how much two blurred texels are blurred does not matter: both are soft)
    vec2 grid = gl_FragCoord.xy / BkScreen.z - 0.5;
    ivec2 g0 = ivec2(floor(grid + 0.5));
    ivec2 last = ivec2(BkGrid.xy) - 1;
    vec4 blurred = vec4(0.0);
    vec3 low = vec3(0.0);
    float total = 0.0;
    float around = 0.0;
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            ivec2 at = g0 + ivec2(x, y);
            vec2 tent = max(1.0 - abs(vec2(at) - grid) / 1.5, 0.0);
            at = clamp(at, ivec2(0), last);
            vec4 prep = texelFetch(PrepSampler, at, 0);
            float texelSoft = smoothstep(0.4, 1.6, abs(bokehDecode(prep.a)));
            float w = tent.x * tent.y * exp(-abs(texelSoft - soft) * 6.0) + 1.0e-5;
            blurred += texelFetch(GatherSampler, at, 0) * w;
            low += prep.rgb * w;
            around += texelSoft * w;
            total += w;
        }
    }
    blurred /= total;
    low /= total;
    around /= total;
    // the sharp detail comes back where this pixel is sharp and so is the grid around it; a sharp pixel whose texels are
    // all blurred is a gap in something blurred in front of it (leaves), and melts into that
    vec3 colour = blurred.rgb + (1.0 - blurred.a) * (1.0 - soft) * (1.0 - around) * (sharp - low);
    vec3 discs = texture(SpriteSampler, gl_FragCoord.xy / BkScreen.z / BkGrid.xy).rgb;
    // the discs are light on top (screened): where they pile up they saturate softly instead of clipping, and where there
    // are none the picture is untouched
    colour = 1.0 - (1.0 - clamp(colour, 0.0, 1.0)) * (1.0 - clamp(discs, 0.0, 1.0));

    // tilt-shift: the colours of a model railway, a little richer and brighter
    if (BkLens.w > 0.0) {
        float luma = bokehLuma(colour);
        vec3 rich = mix(vec3(luma), colour, 1.28);
        rich = mix(rich, rich * rich * (3.0 - 2.0 * rich), 0.22);
        colour = mix(colour, clamp(rich, 0.0, 1.0), BkLens.w * BkLook.z);
    }

    int view = int(BkLook.w);
    if (view == 1) {
        // blur: violet behind the focus, blue in front of it, white where sharp
        float b = clamp(abs(coc) / BkLens.y, 0.0, 1.0);
        colour = mix(vec3(1.0), coc > 0.0 ? vec3(0.7, 0.2, 1.0) : vec3(0.1, 0.5, 1.0), sqrt(b)) * (0.35 + 0.65 * bokehLuma(sharp));
    } else if (view == 2) {
        colour = vec3(texelFetch(PrepSampler, ivec2(grid + 0.5), 0).a);
    } else if (view == 3) {
        colour = discs;
    } else if (view == 4) {
        colour = mix(sharp * 0.5, vec3(1.0, 0.4, 0.8), blurred.a);
    } else if (view == 5) {
        colour = texelFetch(GatherSampler, ivec2(grid + 0.5), 0).rgb;
    } else if (view == 6) {
        colour = texelFetch(PrepSampler, ivec2(grid + 0.5), 0).rgb;
    } else if (view == 7) {
        colour = vec3(soft, blurred.a, total / 2.25);
    }
    fragColor = vec4(colour + (bokehNoise(gl_FragCoord.xy + 17.0) - 0.5) / 255.0, 1.0);
}
