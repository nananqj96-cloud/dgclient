#version 330

// What flies through Cyberspace (see dev.purxiy.cyberspace.CyberspaceRenderer#drawFlight); keep in sync with it.
layout(std140) uniform PurxiyCyberspaceFlight {
    // the camera-relative world to clip space
    mat4 FlViewProj;
    // the camera's right and up, in the world
    vec4 FlRight;
    vec4 FlUp;
    // per packet or bit, four vec4s:
    //   head (camera-relative), half its size
    //   the trail's corner, how visible it is (0..1)
    //   the trail's end, how bright its trail is
    //   x = tint (0 first colour .. 1 accent), y = 1 for a bit, z = its turn (radians), w = how bright its head is
    vec4 FlObjects[512];
};
