#version 330

// The frame of Chroma (see dev.purxiy.chroma.ChromaRenderer); keep in sync with it. Positions are camera-relative,
// like the rest of the world; sizes are in blocks.
layout(std140) uniform PurxiyChroma {
    // clip space to the camera-relative world
    mat4 ChInverse;
    // x, y = framebuffer size (px), z = blocks per pixel one block from the camera, w = clock (s)
    vec4 ChScreen;
    // xyz = where in its block the camera is, w = the camera's height in the world
    vec4 ChCamera;
    // xyz = the camera's block
    ivec4 ChBlock;
    // xy = the texel of the camera's column in the paint (it wraps around), zw = the first column the paint holds,
    // relative to the camera's
    ivec4 ChWindow;
    // x = now (paint ticks, modulo PaintMap.PERIOD), y = ticks over which colour fades once its time is up,
    // z = growing blooms, w = development view (0: none)
    ivec4 ChPaint;
    // rgb = the tone's shadow, a = how much colour the grey world keeps
    vec4 ChShadow;
    // rgb = the tone's middle, a = how grey the world is (0 while Chroma is off)
    vec4 ChMid;
    // rgb = the tone's light, a = how much colour the sky has
    vec4 ChLight;
    // x = the flood of colour around the camera (blocks), y = 1: living things keep their colour
    vec4 ChFlood;
    // per bloom: (x, y, z, radius now), (wet, 0, 0, 0)
    vec4 ChBlooms[128];
};
