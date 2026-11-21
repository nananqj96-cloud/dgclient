#version 330

// Halo (see dev.purxiy.halo.HaloRenderer): the halo as pieces of light already on the screen, in pixels, drawn back to
// front. Keep the limits in sync with HaloRenderer.

#define HALO_PIECES 64
#define HALO_POINTS 288

layout(std140) uniform PurxiyHalo {
    mat4 HaloInverse;   // clip space -> camera-relative position (to read the scene's depth as a distance)
    vec4 HaloScreen;    // xy = target size (px), z = pieces, w = 1 to test the scene's depth
    vec4 HaloRect;      // the rectangle the light reaches: x0, y0, x1, y1 (px, from the bottom left)
    vec4 HaloCentre;    // xyz = the halo's centre, camera-relative (for the fog), w = how much of it shows
    // per piece: [0] x = first point, y = points, z = kind (0 a line, 1 a dot), w = how much a dot flares into a star;
    //            [1] the rectangle its light reaches (px)
    vec4 HaloPieces[HALO_PIECES * 2];
    // per point: [0] xy = where (px), z = distance from the camera (blocks), w = half the core's width (px);
    //            [1] x = how far the glow reaches (px), y = the core's light, z = the glow's light, w = how much the core hides;
    //            [2] x = the core's colour, y = the glow's colour (r + 256 g + 65536 b), z = how white-hot the core's middle is
    vec4 HaloPoints[HALO_POINTS * 3];
};
