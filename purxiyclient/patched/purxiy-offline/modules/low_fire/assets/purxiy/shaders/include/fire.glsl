#version 330

// Low Fire (see dev.purxiy.fire.PurxiyFire): the fire you see while you burn, low on the screen and in the palette.
// Positions of the flames are in screen heights (x from the left edge, y up from the bottom); embers are in framebuffer
// pixels (gl_FragCoord, y up). Keep the limits in sync with PurxiyFire.

#define MAX_TONGUES 48
#define MAX_EMBERS 96

layout(std140) uniform PurxiyFire {
    vec4 FireScreen;  // xy = framebuffer size (px), z = pixels per 1080p pixel, w = the flames' clock (s)
    vec4 FireRect;    // where the flames are drawn: x0, y0, x1, y1 in NDC
    vec4 FireShape;   // x = how high the flames reach now (screen heights), y = opacity, z = lean (-1..1), w = how high their tongues reach at most
    vec4 FireStyle;   // x = flames, y = pixel, z = glow (how much of each style: they cross-fade), w = calm (Fire Resistance)
    vec4 FireLife;    // x = flare, y = the flash of catching fire, z = steam (0..1 while it billows, 1 = none), w = presence
    vec4 FireLook;    // x = heat haze, y = how far up the haze reaches (screen heights), z = size (the height setting / 30%), w = the height setting
    vec4 FireSprite;  // the pixel fire in its texture: x = u0, y = u1, z = the height of a frame (v), w = frames
    vec4 FirePixel;   // x = the frame showing now, y = the width of a tile (screen heights), z = tiles, w = unused
    vec4 FireLayers;  // x, y = spacing of the tongues of the front and back layer (screen heights), z, w = how many cells either side reach a pixel
    vec4 FireRim;     // the ramp of the fire's colours: its cool rim,
    vec4 FireSecond;  // the palette's second colour,
    vec4 FireFirst;   // its first colour,
    vec4 FireHeart;   // and the white-hot heart
    vec4 FireAccent;  // the palette's accent
    // per tongue, the front layer first: [0] x = where it stands, y = height, z = half its width, w = sway (all screen
    // heights); [1] x = phase of the wave running up it, y = the wave's size, z = the wisp breaking off its tip (0..1,
    // negative for none), w = where the wisp is (x)
    vec4 FireTongues[2 * MAX_TONGUES * 2];
    // per ember: [0] xy = position (px), z = radius (px), w = brightness; [1] xy = velocity (px/s), z = heat (0..1), w = seed
    vec4 FireEmbers[MAX_EMBERS * 2];
};

// The streak of an ember: only fast embers stretch, a little (never in the Pixel style).
float fireStreak(float speed) {
    return min(speed * 0.006, 5.0 * FireScreen.z) * (1.0 - FireStyle.y);
}
