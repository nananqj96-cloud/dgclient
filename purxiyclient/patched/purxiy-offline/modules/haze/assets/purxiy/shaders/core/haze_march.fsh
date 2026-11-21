#version 330

#moj_import <purxiy:haze.glsl>

// The haze, marched along every view ray on a grid of half the screen's size (texel q for pixel 2q): from the camera to
// what the pixel shows, or as far as the land can be seen for the sky. Out: the haze's mean colour (rgb) and how much of
// the picture it hides (a), which the composite lays over the picture at full size.

uniform sampler2D DepthSampler;

out vec4 fragColor;

void main() {
    ivec2 texel = ivec2(gl_FragCoord.xy);
    ivec2 full = min(texel * 2, ivec2(HazeScreen.xy) - 1);
    float depth = texelFetch(DepthSampler, full, 0).r;
    bool sky = depth >= 0.999999;
    vec2 uv = (vec2(full) + 0.5) / HazeScreen.xy;
    vec3 rel = hazePosition(uv, sky ? 0.9999 : depth);
    float distance = length(rel);
    vec3 dir = rel / max(distance, 1.0e-4);
    float end = hazeEnd(dir, distance, sky);
    // right around you the air is clear: nothing to march
    if (end <= HazeReach.x) {
        fragColor = vec4(0.0);
        return;
    }
    vec4 haze = hazeMarch(dir, end, 6);
    float hidden = 1.0 - haze.a;
    fragColor = vec4(haze.rgb / max(hidden, 1.0e-4), hidden);
}
