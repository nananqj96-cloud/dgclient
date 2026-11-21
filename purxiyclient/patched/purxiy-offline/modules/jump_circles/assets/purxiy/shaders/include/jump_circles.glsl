#version 330

// Jump Circles (see dev.purxiy.jumpcircles.JumpCircles). Positions are camera-relative, like the rest of the world;
// sizes are in blocks. Keep the limit in sync with JumpCircles.

#define MAX_RINGS 32

layout(std140) uniform PurxiyJumpCircles {
    mat4 CircleViewProj;  // camera-relative position -> clip space
    mat4 CircleInverse;   // clip space -> camera-relative position
    vec4 CircleScreen;    // xy = framebuffer size (px), z = rings, w = how far the glow reaches beyond the line (blocks)
    vec4 CircleFirst;     // the palette's first colour, w = glow
    vec4 CircleSecond;    // its second colour
    vec4 CircleAccent;    // its accent
    // per ring: [0] xyz = centre (on the ground), w = radius;
    //           [1] x = half width of the line, y = opacity, z = age (0..1), w = where the gradient starts (turns)
    vec4 CircleRings[MAX_RINGS * 2];
};
