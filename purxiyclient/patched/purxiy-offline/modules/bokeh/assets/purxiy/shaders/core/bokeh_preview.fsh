#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:theme.glsl>

// Bokeh's shape picker (see dev.purxiy.bokeh.BokehPreview): a night scene seen through the lens, far out of focus. Lights
// drift and breathe as discs in one shape, with the same edge, rim and fringe as in the world.

layout(std140) uniform PurxiyBokehPreview {
    // x, y = the tile's size (px); z = the shape; w = how selected or hovered the tile is (0..1)
    vec4 PvTile;
    // x = fringe (0..1); y = highlights (0..1); z = swirl (0..1); w = how many discs
    vec4 PvLook;
    // xy = where (0..1 of the tile), z = radius (px), w = brightness
    vec4 PvDiscs[16];
    // rgb = colour
    vec4 PvColours[16];
};

out vec4 fragColor;

void main() {
    vec2 size = PvTile.xy;
    vec2 uv = gl_FragCoord.xy / size;
    int shape = int(PvTile.z);
    // the night: deep violet at the top, almost black below, a soft glow of city light low in the middle
    vec3 colour = mix(prx_theme(vec3(0.035, 0.028, 0.07)), prx_theme(vec3(0.11, 0.075, 0.2)), uv.y);
    vec2 low = (uv - vec2(0.5, 0.18)) * vec2(1.2, 2.2);
    float glow = exp(-dot(low, low) * 2.0);
    colour += prx_theme(vec3(0.16, 0.09, 0.26)) * glow * (0.55 + 0.45 * PvTile.w);
    int count = int(PvLook.w);
    for (int i = 0; i < 16; i++) {
        if (i >= count) {
            break;
        }
        vec4 disc = PvDiscs[i];
        vec2 local = (gl_FragCoord.xy - disc.xy * size) / disc.z;
        float d = max(bokehGauge(local, shape), bokehCatsEye(local, disc.xy, size.x / size.y, PvLook.z));
        vec3 edge = d / bokehFringe(PvLook.x, disc.z);
        vec3 cover = clamp((1.0 - edge) / (1.0 / disc.z + 0.05) + 0.5, 0.0, 1.0);
        float rim = 0.92 + 0.14 * smoothstep(0.65, 1.0, d);
        colour += PvColours[i].rgb * disc.w * (0.45 + 0.75 * PvLook.y) * cover * rim;
    }
    fragColor = vec4(colour, 1.0);
}
