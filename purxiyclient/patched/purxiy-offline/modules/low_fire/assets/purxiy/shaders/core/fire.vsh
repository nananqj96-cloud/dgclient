#version 330

#moj_import <purxiy:fire.glsl>

// The first six vertices are two triangles over the flames (the fragment shader works from gl_FragCoord); then six per
// ember, a quad around it, stretched along its motion.
flat out int ember;
// in an ember's quad: where this corner lies from its centre (px), x along its motion, y across it
out vec2 local;

void main() {
    int i = gl_VertexID % 6;
    vec2 corner = vec2(i == 1 || i == 2 || i == 4 ? 1.0 : 0.0, i == 2 || i == 4 || i == 5 ? 1.0 : 0.0);
    int quad = gl_VertexID / 6;
    if (quad == 0) {
        ember = -1;
        local = vec2(0.0);
        gl_Position = vec4(mix(FireRect.xy, FireRect.zw, corner), 0.0, 1.0);
        return;
    }
    ember = quad - 1;
    vec4 a = FireEmbers[ember * 2];
    vec4 b = FireEmbers[ember * 2 + 1];
    float speed = length(b.xy);
    vec2 along = speed > 1.0e-3 ? b.xy / speed : vec2(0.0, 1.0);
    // pixel embers stay square to the screen
    if (FireStyle.y > 0.5) {
        along = vec2(1.0, 0.0);
    }
    vec2 across = vec2(-along.y, along.x);
    // the glow reaches a few radii out
    float reach = a.z * 4.0 + 2.0 * FireScreen.z;
    local = (corner * 2.0 - 1.0) * vec2(a.z + fireStreak(speed) + reach, a.z + reach);
    vec2 p = a.xy + along * local.x + across * local.y;
    gl_Position = vec4(p / FireScreen.xy * 2.0 - 1.0, 0.0, 1.0);
}
