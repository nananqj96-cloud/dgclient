#version 330

// Trail (see dev.purxiy.trail.TrailRenderer). Positions are camera-relative, like the rest of the world; sizes are in
// blocks. What a quad is comes with its vertices (Head.w): keep the kinds in sync with TrailGeometry.

#define KIND_LINE 0
#define KIND_BAND 1
#define KIND_MOTE 2
#define KIND_ORB 3

layout(std140) uniform PurxiyTrail {
    mat4 TrailViewProj;  // camera-relative position -> clip space
    mat4 TrailInverse;   // clip space -> camera-relative position
    vec4 TrailScreen;    // xy = size of the target (px), z = time (s), w = glow (0..1)
    vec4 TrailRight;     // xyz = the camera's right in the world, w = pixels per block at a distance of one block
    vec4 TrailUp;        // xyz = the camera's up in the world, w = 1 with Reduce motion
    vec4 TrailFirst;     // the palette's first colour, w = style (TrailStyle ordinal)
    vec4 TrailSecond;    // its second colour, w = size (1 = 100%)
    vec4 TrailAccent;    // its accent
    vec4 TrailLimits;    // x = widest line (px of radius), y = furthest glow (px), z = thinnest line (px), w = near fade (blocks)
};
