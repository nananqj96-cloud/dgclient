#version 330

// Radiance (see dev.purxiy.radiance.PurxiyRadiance): a glowing outline of light around what radiates. Screen positions
// are framebuffer pixels (gl_FragCoord, y up). Keep the limits in sync with PurxiyRadiance.

#define MAX_SLOTS 128
#define MAX_RECTS 16

layout(std140) uniform PurxiyRadiance {
    vec4 RadianceScreen;  // xy = framebuffer size (px), z = pixels per 1080p pixel, w = flow (animation time, s)
    vec4 RadianceLook;    // x = line width (px), y = line, z = halo, w = wisps (how much of each look)
    vec4 RadianceGlow;    // x = reach of the halo (px), y = reach of the aura (px), z = glow, w = fill
    vec4 RadianceFirst;   // the palette's first colour (rgb), w = opacity (the module fading in and out)
    vec4 RadianceSecond;  // its second colour, w = reach of the distance field (px)
    vec4 RadianceAccent;  // its accent, w = sparkles
    vec4 RadianceRects[MAX_RECTS]; // where the passes run: x0, y0, x1, y1 in NDC
};

layout(std140) uniform PurxiyRadianceSlots {
    // per slot: [0] its rectangle on screen (px: x0, y0, x1, y1); [1] xyz = tint, w = how much of it; [2] x = presence,
    // y = focus, z = flash, w = ripple (0..1, 1 = none); [3] x = reveal, y = scale, z = seed, w = depth (blocks from the
    // camera; 0 for your hand)
    vec4 RadianceSlots[MAX_SLOTS * 4];
};

// Slots travel as bytes (the mask holds the slot of every pixel, 0 for none).
int radianceByte(float value) {
    return int(value * 255.0 + 0.5);
}

// The slot of a pixel of the mask; beyond the border of the screen, that of the pixel on it (the border is no edge).
int radianceSlotAt(sampler2D mask, ivec2 p, ivec2 limit) {
    return radianceByte(texelFetch(mask, clamp(p, ivec2(0), limit), 0).r);
}

float radianceDepth(int slot);

// Whether a neighbour ends a slot's silhouette, so the pixel is an edge: nothing radiates there, or something further
// away does (then this edge is drawn over it, so each keeps its own outline).
bool radianceEnds(int slot, int other) {
    return other != slot && (other == 0 || radianceDepth(other) > radianceDepth(slot));
}

// The distance field: per pixel, xy = the way to the nearest edge pixel (px, +128 in a byte each), z = that edge's slot
// (a byte), w = 1 where there is one. The coarse field (half resolution) measures the way from twice its position.
vec4 radiancePack(vec2 offset, int slot) {
    return vec4((offset + 128.0) / 255.0, float(slot) / 255.0, 1.0);
}

vec2 radianceOffset(vec4 texel) {
    return floor(texel.xy * 255.0 + 0.5) - 128.0;
}

float radianceDepth(int slot) {
    return RadianceSlots[slot * 4 + 3].w;
}

// The size of the coarse field, less one: its last pixel.
ivec2 radianceCoarseLimit() {
    return (ivec2(RadianceScreen.xy) + 1) / 2 - 1;
}
