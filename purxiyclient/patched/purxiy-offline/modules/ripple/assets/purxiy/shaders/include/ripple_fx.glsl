#version 330

// The light Ripple sets free in the world (see dev.purxiy.ripple.RippleFx): stars, sparkles, pillars, rings and
// flares. Positions are camera-relative; sizes in blocks. Keep the limit in sync with RippleFx.

#define MAX_ITEMS 128

#define STAR 0
#define SPARKLE 1
#define PILLAR 2
#define RING 3
#define FLARE 4

// How long a pillar lives (s); keep in sync with RippleFx.PILLAR_LIFE.
#define PILLAR_LIFE 1.7

layout(std140) uniform PurxiyRippleFx {
    mat4 FxViewProj;  // camera-relative position -> clip space
    mat4 FxInverse;   // clip space -> camera-relative position
    vec4 FxScreen;    // xy = framebuffer size (px), z = pieces, w = pixels per 1080p pixel
    vec4 FxLook;      // x = glow, y = motion (0 when reduced), z = time (s), w = pixels a block covers one block away
    vec4 FxFirst;     // the palette's first colour
    vec4 FxSecond;    // its second colour
    vec4 FxAccent;    // its accent
    // per piece: [0] xyz = where it is, w = its kind; [1] depends on the kind:
    //   STAR, SPARKLE  x = radius of its body (blocks), y = how far through its life, z = turn, w = a random flare
    //   PILLAR         x = height, y = width of the body, z = how far through its life, w = height of the body
    //   RING           x = how far it rolls, y = how far through its life, z = where its colours start (turns)
    //   FLARE          x = radius (blocks), y = how far through its life
    vec4 FxItems[MAX_ITEMS * 2];
};
