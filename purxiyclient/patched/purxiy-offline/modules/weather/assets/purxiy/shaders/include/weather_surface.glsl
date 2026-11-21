// The frame of the ground pass (see dev.purxiy.weather.WeatherSurface); keep in sync with it. Positions are
// camera-relative, like the rest of the world; sizes are in blocks.
layout(std140) uniform WeatherSurface {
    // clip space to the camera-relative world, and back
    mat4 SurfaceInverse;
    mat4 SurfaceViewProj;
    // x, y = framebuffer size (px), z = blocks per pixel one block from the camera, w = seconds
    vec4 SurfaceScreen;
    // xyz = where in its block the camera is, w = the camera's height in the world
    vec4 SurfaceCamera;
    // xyz = the camera's block, w = development view (0: none)
    ivec4 SurfaceBlock;
    // x = how long it has rained (0..1), y = snow cover, z = molten cover, w = the Coverage setting
    vec4 SurfaceLook;
    // x = 1: reflections traced through the scene, y = lightning flash, z = wind, w = intensity
    vec4 SurfaceEffects;
    // xyz = towards the sun (or the moon at night), w = how strongly it shines
    vec4 SurfaceSun;
    // rgb = its light, a = daylight
    vec4 SurfaceLight;
    // rgb = the sky overhead, a = the near plane
    vec4 SurfaceZenith;
    // rgb = the sky at the horizon, a = the far plane
    vec4 SurfaceHorizon;
    // sky probe: x = how fast a direction in view takes the sky's colour, y = how fast one out of view takes its
    // neighbours', z = 1: start from the colours above
    vec4 SurfaceProbe;
};
