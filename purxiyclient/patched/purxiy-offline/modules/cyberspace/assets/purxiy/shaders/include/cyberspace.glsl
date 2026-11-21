#version 330

// The frame of Cyberspace (see dev.purxiy.cyberspace.CyberspaceRenderer); keep in sync with it.
layout(std140) uniform PurxiyCyberspace {
    // clip space to the camera-relative world (the view is only the camera's turn)
    mat4 CyInverse;
    // xyz = the block the camera is in, w = 1 when a mask of your hands keeps them clear
    ivec4 CyCamFloor;
    // xyz = where in that block the camera is, w = time (s)
    vec4 CyCamFract;
    // x = width, y = height (px), z = the projection's focal length (m11), w = glow
    vec4 CyScreen;
    // the palette: first, second and accent; w = reality, circuits and the digital sky (0..1)
    vec4 CyFirst;
    vec4 CySecond;
    vec4 CyAccent;
    // x = porcelain (0 midnight .. 1 porcelain), y = reduced motion, z = glitch (0..1), w = the glitch's seed
    vec4 CyLook;
    // xyz = towards the sun, w = 1 in the overworld
    vec4 CySun;
    // xyz = towards the moon
    vec4 CyMoon;
    // the fronts: xyz = where they started (camera-relative), w = the outer front (blocks)
    vec4 CyFront;
    // x = the inner front (blocks), y = how digital the sky is, z = how far the world reaches (blocks)
    vec4 CyFront2;
    // xyz = the block you look at, w = 1 if you look at one
    ivec4 CyTarget;
    // x = how far you are in breaking it, y = how long you have looked at it (s), z = 1 while a shader pack is in use
    vec4 CyTarget2;
    // scan waves: xyz = where they started (camera-relative), w = how far they have come (negative: none)
    vec4 CyScans[4];
};
