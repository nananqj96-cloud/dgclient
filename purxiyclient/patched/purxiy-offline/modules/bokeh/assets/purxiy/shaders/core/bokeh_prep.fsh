#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh, first: the picture on the working grid (the mean of f x f pixels per texel), and in alpha how blurred each texel
// is: the blur of its nearest surface, so the edges of what is in front stay where they are. With a shader pack your
// hand is in the picture but not in the depth: a mask of it keeps it sharp.

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
#ifdef MASKED
uniform sampler2D HandMaskSampler;
#endif

out vec4 fragColor;

void main() {
    int f = int(BkScreen.z);
    ivec2 base = ivec2(gl_FragCoord.xy) * f;
    ivec2 last = ivec2(BkScreen.xy) - 1;
    vec3 sum = vec3(0.0);
    float nearest = 2.0;
    ivec2 nearestAt = base;
    bool hand = false;
    for (int y = 0; y < f; y++) {
        for (int x = 0; x < f; x++) {
            ivec2 p = min(base + ivec2(x, y), last);
            sum += texelFetch(SceneSampler, p, 0).rgb;
            float depth = texelFetch(DepthSampler, p, 0).r;
            if (depth < nearest) {
                nearest = depth;
                nearestAt = p;
            }
#ifdef MASKED
            hand = hand || texelFetch(HandMaskSampler, p, 0).r > 0.5;
#endif
        }
    }
    vec2 at = vec2(nearestAt) + 0.5;
    float coc = hand ? 0.0 : bokehCoc(bokehDepth(at, nearest), at.y / BkScreen.y);
    fragColor = vec4(sum / float(f * f), bokehEncode(coc));
}
