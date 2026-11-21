#version 330

// Shared by the vertex and fragment stage of the ambience (see dev.purxiy.ambience.PurxiyAmbience).

layout(std140) uniform PurxiyAmbience {
    vec4 AmbDepth;  // x = near plane, y = far plane (blocks), z = distance to a surface at which particles start to fade
    vec4 AmbScreen; // x = width, y = height (px), z = smallest radius drawn (px)
    vec4 AmbLook;   // x = glow, y = depth of field, z = twinkle
};

// Shapes, as in dev.purxiy.ambience.AmbienceStyle.
const int ORB = 0;
const int SPARKLE = 1;
const int PETAL = 2;
const int FLAKE = 3;

// The size (px) the glow of a billboard is measured in: its body, but never less than a few pixels, so even small and
// distant particles glow like the bloom around a light.
float ambienceGlowSize(float body) {
    return max(body, 2.5);
}

// How far the light of a billboard reaches from its centre (px): its glow, or the rays of a sparkle, which grow while
// it flares. Everything has faded out there, so the quad can end there without a visible edge.
float ambienceReach(int shape, float body, float flare) {
    if (shape == SPARKLE) {
        return body * (5.0 + 8.0 * AmbLook.x) * (1.0 + flare * 0.6);
    }
    return ambienceGlowSize(body) * (3.0 + 4.0 * AmbLook.x);
}
