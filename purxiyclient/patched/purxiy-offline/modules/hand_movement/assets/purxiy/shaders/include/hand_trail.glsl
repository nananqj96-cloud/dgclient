#version 330

// The swing trail of your hand (see dev.purxiy.hand.HandTrail). Keep MAX_GHOSTS in sync with HandTrail.

#define MAX_GHOSTS 160

layout(std140) uniform PurxiyHandTrail {
    mat4 TrailProjection;          // the projection your hand is drawn with
    mat4 TrailModelView;           // and its model-view: from the pose space the hand was kept in to the view
    vec4 TrailFirst;               // Radiance's first colour (rgb), w = how much its light colours the trail (0 without it)
    vec4 TrailSecond;              // its second colour, w = strength of the trail
    vec4 TrailAccent;              // its accent, w = time (s)
    vec4 TrailScreen;              // xy = size of the screen (px), z = number of moments
    vec4 TrailLight;               // xy = your hand's light level, as lightmap coordinates
    vec4 TrailRect;                // where the trail can be this frame: x0, y0, x1, y1 in NDC
    mat4 TrailGhosts[MAX_GHOSTS];  // per moment: from the hand as it is now to the hand as it was then (pose space)
    vec4 TrailAges[MAX_GHOSTS];    // per moment: x = its age (0 now .. 1 the end of the trail)
};
