#version 330

// Glass (see dev.purxiy.glass.PurxiyGlass): players and items drawn as glass. Positions are in a space with the camera
// at its origin (relative to the camera for the world, your view for your hand); sizes in blocks, times in seconds.
// Keep the limits in sync with GlassRenderer.

#define MAX_BODIES 64

// flags of a quad (GlassGeometry)
#define FLAG_EDGE 1
#define FLAG_FOIL 2
#define FLAG_ITEM 4

layout(std140) uniform PurxiyGlass {
    mat4 GlassModel;       // position -> shading space (the camera at its origin)
    mat4 GlassClip;        // shading space -> clip space
    mat4 GlassInverse;     // clip space -> shading space (the world: rebuilds the scene from its depth)
    vec4 GlassScreen;      // xy = framebuffer size (px), z = time (s), w = pixels per 1080p pixel
    vec4 GlassTint;        // rgb = the glass's colour, w = how strongly light through it takes that colour
    vec4 GlassGlow;        // rgb = the colour of its light, w = how much light it gives (0..1)
    vec4 GlassLook;        // x = frost (0..1), y = shimmer (0..1), z = how milky, w = how far light travels bent (blocks)
    vec4 GlassUp;          // xyz = up, w = daylight (0 night .. 1 day)
    vec4 GlassRight;       // xyz = the camera's right, w = 1 for your hand
    vec4 GlassKey;         // xyz = where the key light is (up and to the left of the camera), w = its strength
    vec4 GlassFill;        // xyz = where a softer light is (to the camera's right), w = its strength
    vec4 GlassSun;         // xyz = towards the sun (or the moon), w = its strength
    vec4 GlassSunColor;    // rgb = its colour, w = where the shimmer is on every body (0..1), or -1 (development)
    vec4 GlassSky;         // rgb = the sky overhead
    vec4 GlassHorizon;     // rgb = the sky at the horizon (the fog), w = the light of what has none of its own (the preview)
    // per body: [2i] xyz = its middle, w = its radius (blocks); [2i + 1] x = hurt flash, y = a random seed, z = 1 for items
    vec4 GlassBodies[MAX_BODIES * 2];
};

float glassLuma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}
