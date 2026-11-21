#version 330

#moj_import <purxiy:hand_trail.glsl>

// A moment of the trail: your hand's own colours, fading with the moment's age; with Radiance on your hand they turn
// towards its light, from its first colour close behind the hand to its second along the sweep, with a drift of the
// accent flowing through. Premultiplied.

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;
in float age;
in float textured;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
#ifdef MASK
    // your hand as it is now, for the mask the trail keeps clear of (with a shader pack)
    fragColor = vec4(1.0);
    return;
#endif
    // the colour the sprite reads as from a distance (its pixel pattern would flicker along the sweep), a little lighter,
    // brightest right behind your hand
    vec3 color = mix(vertexColor.rgb, texel.rgb * vertexColor.rgb, textured);
    color = mix(color, vec3(1.0), 0.14) * (1.12 - 0.22 * age);
    float radiance = TrailFirst.w;
    if (radiance > 0.0) {
        vec3 light = mix(TrailFirst.rgb, TrailSecond.rgb, smoothstep(0.0, 0.9, age));
        light = mix(light, TrailAccent.rgb, 0.3 * (0.5 + 0.5 * sin(age * 7.0 - TrailAccent.w * 2.4)));
        float luma = dot(color, vec3(0.299, 0.587, 0.114));
        color = mix(color, light * (0.8 + 0.5 * luma), radiance * (0.5 + 0.35 * age));
    }
    float fade = pow(1.0 - age, 2.2);
    float alpha = clamp(TrailSecond.w * 0.7 * fade, 0.0, 1.0);
    // in Radiance's light the trail is light itself: it covers less and adds more
    fragColor = vec4(color * alpha * (1.0 + 0.35 * radiance), alpha * (1.0 - 0.45 * radiance));
}
