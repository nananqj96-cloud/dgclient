#version 330

// The light of a two-handed thing (see dev.purxiy.wield.WieldFx): slash arcs and the cuts of hits.

layout(std140) uniform PurxiyWield {
    mat4 WieldProjection;  // the projection your hand is drawn with (the arcs come already in its view space)
    vec4 WieldDeep;        // the deeper colour under the light (rgb), w = time (s)
    vec4 WieldScreen;      // xy = size of the screen (px)
};
