#version 330

// The Purxiy block outline (see dev.purxiy.outline.PurxiyOutline). Positions are camera-relative, like the rest of
// the world; screen positions are in framebuffer pixels (gl_FragCoord). Keep the limits in sync with PurxiyOutline.

#define MAX_SEGMENTS 192
#define MAX_INSTANCES 8
#define MAX_BOXES 16

layout(std140) uniform PurxiyOutline {
    mat4 OutlineViewProj;  // camera-relative position -> clip space
    mat4 OutlineInverse;   // clip space -> camera-relative position
    vec4 OutlineRect;      // the rectangle drawn this frame, in NDC: x0, y0, x1, y1
    vec4 OutlineScreen;    // xy = framebuffer size (px), z = pixels per 1080p pixel, w = unused
    vec4 OutlineColorA;    // the colour (straight alpha)
    vec4 OutlineColorB;    // the colour towards the top (the gradient's end)
    vec4 OutlineLine;      // x = half line width (px), y = glow reach (px), z = glow, w = corner arm (blocks, 0 = whole edges)
    vec4 OutlineFill;      // x = fill, y = aim light, z = hidden edges, w = shimmer
    vec4 OutlineAim;       // xyz = where the crosshair hits, w = how much of the aim light is on
    vec4 OutlineFace;      // xyz = normal of the face you aim at, w = mining progress (eased)
    vec4 OutlineSheen;     // x = position of the shimmer's band, y = its width, z = dash length (blocks), w = unused
    vec4 OutlineCounts;    // x = segments, y = boxes
    // per instance (an outline: the target, the shape it morphs from, ghosts fading out, ripples):
    // [0] xyz = bounds min, w = opacity; [1] xyz = bounds max, w = heat; [2] x = mining arms, y = flash
    vec4 OutlineInstances[MAX_INSTANCES * 3];
    // boxes of the target's shape, for its faces: [0] min, [1] max
    vec4 OutlineBoxes[MAX_BOXES * 2];
    // per edge: [0] from (x, y px; z = window depth, pulled towards the camera; w = clip w), [1] to (same),
    // [2] xyz = from (camera-relative), w = instance; [3] xyz = to (camera-relative), w = length (blocks)
    vec4 OutlineSegments[MAX_SEGMENTS * 4];
};

// How far edges and faces are pulled towards the camera for the depth test: they lie on the surface they outline.
const float OUTLINE_SHRINK = 1.0 - 1.0 / 192.0;
