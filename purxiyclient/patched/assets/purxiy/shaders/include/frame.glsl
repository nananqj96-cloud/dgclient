#version 330

// Per-frame data of the Purxiy renderer (see PurxiyRenderer#writeFrameUniforms).
layout(std140) uniform PurxiyFrame {
    mat4 ProjMat;
    // x = framebuffer width (px), y = framebuffer height (px), z = device pixels per UI unit, w = time (s)
    vec4 ScreenInfo;
    // how the logo's colours move (see tint.glsl): its own purple, the accent or a colour picked for it
    vec4 LogoTurn;
    vec4 LogoMore;
};
