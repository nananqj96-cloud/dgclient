#version 330

#moj_import <purxiy:bokeh_shape.glsl>
#moj_import <purxiy:bokeh.glsl>

// Bokeh: a disc of light in the lens's shape, with a soft edge that is a touch brighter (as a real lens draws it) and a
// lilac fringe. A disc behind the focus hides behind whatever is sharper in front of it, and softly behind blurred
// foreground as far as that covers the spot (the gather's alpha); one in front of the focus lies over everything.

uniform sampler2D PrepSampler;
uniform sampler2D GatherSampler;

in vec2 local;
in vec3 light;
flat in float spriteCoc;
flat in vec2 spriteScreen;

out vec4 fragColor;

void main() {
    float radius = abs(spriteCoc);
    float d = bokehLensGauge(local, spriteScreen);
    vec3 edge = d / bokehFringe(BkLook.y, radius);
    // an edge about a texel of the grid wide, and a little softer: creamy, not cut out
    float soft = BkScreen.z / radius + 0.05;
    vec3 cover = clamp((1.0 - edge) / soft + 0.5, 0.0, 1.0);
    float rim = 0.92 + 0.14 * smoothstep(0.65, 1.0, d);
    float visible = 1.0;
    if (spriteCoc > 0.0) {
        // what stands in front of the light cuts its disc: sharply where it is sharp itself, and softly (over as far as
        // it is blurred) where it is blurred, so a blurred screen of leaves does not cut the disc into glitter
        ivec2 at = ivec2(gl_FragCoord.xy);
        float here = bokehDecode(texelFetch(PrepSampler, at, 0).a);
        int reach = int(clamp(abs(here) / BkScreen.z * 0.5, 0.0, 3.0) + 0.5);
        ivec2 last = ivec2(BkGrid.xy) - 1;
        visible = 0.0;
        for (int i = 0; i < 5; i++) {
            ivec2 o = i == 0 ? ivec2(0) : i == 1 ? ivec2(reach, 0) : i == 2 ? ivec2(-reach, 0) : i == 3 ? ivec2(0, reach) : ivec2(0, -reach);
            ivec2 q = clamp(at + o, ivec2(0), last);
            float c = i == 0 ? here : bokehDecode(texelFetch(PrepSampler, q, 0).a);
            visible += smoothstep(0.2, 0.8, c / spriteCoc) * (1.0 - texelFetch(GatherSampler, q, 0).a);
        }
        visible *= 0.2;
    }
    vec3 colour = light * cover * rim * visible;
    // dithered, so faint discs do not band in eight bits
    fragColor = vec4(colour + (bokehNoise(gl_FragCoord.xy) - 0.5) / 255.0, 0.0);
}
