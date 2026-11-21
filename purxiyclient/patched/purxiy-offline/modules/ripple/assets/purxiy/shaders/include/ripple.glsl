#version 330

// The waves of Ripple over the bodies they hit (see dev.purxiy.ripple.PurxiyRipple). Positions are camera-relative,
// like the rest of the world; sizes in blocks, times in seconds. Keep the limits in sync with PurxiyRipple.

#define MAX_BODIES 16
#define MAX_WAVES 4
#define BODY_ROWS 9

layout(std140) uniform PurxiyRipple {
    mat4 RippleViewProj;   // camera-relative position -> clip space
    mat4 RippleInverse;    // clip space -> camera-relative position
    vec4 RippleScreen;     // xy = framebuffer size (px), z = time (s), w = glow
    vec4 RippleFirst;      // the palette's first colour, w = how long the light lingers (s)
    vec4 RippleSecond;     // its second colour
    vec4 RippleAccent;     // its accent
    vec4 RippleScale;      // x = pixels per 1080p pixel
    // per body: [0] x = waves, y = strength, z = height of its box;
    // per wave: [1 + 2i] xyz = where it started (this frame), w = age;
    //           [2 + 2i] x = how far it runs, y = 1 for a critical hit, z = how long its front takes, w = a random seed
    vec4 RippleBodies[MAX_BODIES * BODY_ROWS];
};
