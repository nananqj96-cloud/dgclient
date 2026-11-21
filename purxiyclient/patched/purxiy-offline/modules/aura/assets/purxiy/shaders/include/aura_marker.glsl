#version 330

// The Silent Aura's target ring (see dev.purxiy.aura.AuraMarker). Positions are camera-relative, like the rest of the
// world; sizes are in blocks. Keep the limits in sync with AuraMarker.

#define MAX_MARKERS 4
#define SEGMENTS 72

layout(std140) uniform PurxiyAuraMarker {
    mat4 MarkerViewProj;  // camera-relative position -> clip space
    mat4 MarkerInverse;   // clip space -> camera-relative position
    vec4 MarkerScreen;    // xy = framebuffer size (px), z = rings
    vec4 MarkerFirst;     // the palette's first colour
    vec4 MarkerSecond;    // its second colour
    vec4 MarkerAccent;    // its accent
    // per ring: [0] xyz = the target's feet, w = radius;
    //           [1] x = height of the ring above the feet, y = where its tail ends, z = half width of the line, w = glow reach;
    //           [2] x = opacity, y = hit pulse (0..1), z = where the gradient starts (turns), w = strength of the tail
    vec4 Markers[MAX_MARKERS * 3];
};
