#version 330

// Shockwave (see dev.purxiy.shockwave.ShockRenderer). Positions are camera-relative, like the rest of the world; sizes
// are in blocks. Keep the layout in sync with ShockRenderer.

layout(std140) uniform PurxiyShockwave {
    mat4 ShockViewProj;  // camera-relative position -> clip space
    mat4 ShockInverse;   // clip space -> camera-relative position
    vec4 ShockScreen;    // xy = framebuffer size (px), z = glow (0..1), w = unused
    vec4 ShockGrid;      // xy = the corner of the first cell (x, z), z = the height tops are measured from, w = cells across
    vec4 ShockLimits;    // x = how far any tile has risen, y = how far any tile has sunk (blocks), z = focal length (px)
    vec4 ShockFirst;     // the palette's first colour
    vec4 ShockSecond;    // its second colour
    vec4 ShockAccent;    // its accent
    vec4 ShockRect;      // the part of the screen the moving ground covers (NDC x0, y0, x1, y1)
};
