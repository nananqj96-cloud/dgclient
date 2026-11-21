#version 330

// Aura (see dev.purxiy.auras.AuraRenderer). Positions are camera-relative, like the rest of the world; sizes are in
// blocks. What a quad is comes with its vertices (Head.w): keep the kinds in sync with AuraGeometry.

#define KIND_LINE 0
#define KIND_MOTE 1

layout(std140) uniform PurxiyAura {
    mat4 AuraViewProj;  // camera-relative position -> clip space
    mat4 AuraInverse;   // clip space -> camera-relative position
    vec4 AuraScreen;    // xy = size of the target (px), z = time (s), w = glow (0..1)
    vec4 AuraLimits;    // x = widest core (px of radius), y = furthest glow (px), z = thinnest line (px), w = pixels per block at a distance of one block
    vec4 AuraFade;      // x = what comes closer to the camera than this is gone, y = from here on it is whole (blocks); z = which side of the split is drawn (0 both, 1 behind it, -1 in front of it); w = the split's distance (blocks)
    vec4 AuraForward;   // xyz = the camera's forward (for the split)
};
