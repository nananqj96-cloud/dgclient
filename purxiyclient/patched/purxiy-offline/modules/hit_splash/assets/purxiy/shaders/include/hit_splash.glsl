#version 330

// Hit Splash (see dev.purxiy.hitsplash.SplashRenderer). Positions are camera-relative, like the rest of the world; sizes
// are in blocks. Keep the limits in sync with HitSplash and the layout with SplashRenderer.

#define MAX_CLUSTERS 16
#define MAX_SPLATS 128
#define MAX_DROPS 96

layout(std140) uniform PurxiyHitSplash {
    mat4 SplashViewProj;  // camera-relative position -> clip space
    mat4 SplashInverse;   // clip space -> camera-relative position
    vec4 SplashScreen;    // xy = framebuffer size (px), z = the projection's vertical focal length, w = glow (0..1)
    vec4 SplashFirst;     // the palette's first colour, w = splash time (s)
    vec4 SplashSecond;    // its second colour, w = 1 with Reduce motion
    vec4 SplashAccent;    // its accent
    // per cluster: [0] xyz = its box's low corner, w = first splat; [1] xyz = high corner, w = splats;
    //              [2] = the rectangle it covers on screen (NDC x0, y0, x1, y1)
    vec4 SplashClusters[MAX_CLUSTERS * 3];
    // per splat: [0] xyz = centre on the ground, w = size (blocks per shape unit);
    //            [1] xy = the way it was thrown (cos, sin), z = tile (+8 mirrored), w = how far it has spread (shape units);
    //            [2] x = heat, y = how far it has worn away (shape units), z = the flash of landing, w = opacity
    vec4 SplashSplats[MAX_SPLATS * 3];
    // per droplet: [0] xyz = head, w = radius (blocks); [1] xyz = end of its streak, w = brightness (negative: the flash of a hit)
    vec4 SplashDrops[MAX_DROPS * 2];
};
