#version 330

#moj_import <purxiy:wield.glsl>

// A ribbon of light where the blade passed: x = its age (0 now, 1 gone), y = across it (0 its inner edge, 0.9 the path of
// the tip, 1 its outer glow). Soft towards the hilt, a white-hot line where the tip went, fading as it ages; it adds
// light to what is behind it and veils it a little, so it reads on a bright sky as on a dark cave.

#ifdef MASKED
// your hands as they are now (with a shader pack, whose depth is its own): the light stays behind them
uniform sampler2D MaskSampler;
#endif

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
#ifdef MASKED
    if (texelFetch(MaskSampler, ivec2(gl_FragCoord.xy), 0).r > 0.5) {
        discard;
    }
#endif
    float age = clamp(texCoord.x, 0.0, 1.0);
    float across = texCoord.y;
    float life = pow(1.0 - age, 1.6);
    // the body: nothing at the inner edge, swelling towards the tip's path
    float body = smoothstep(0.0, 0.9, across);
    body *= body * body;
    // the line the tip drew, and the glow past it
    float line = exp(-pow((across - 0.9) / 0.028, 2.0));
    float rim = 1.0 - smoothstep(0.9, 1.0, across);
    float glow = (body * 0.32 + line) * rim;
    float a = glow * life * vertexColor.a;
    vec3 colour = mix(WieldDeep.rgb, vertexColor.rgb, smoothstep(0.15, 0.9, across));
    colour = mix(colour, vec3(1.0), line * 0.45 * (1.0 - age));
    fragColor = vec4(colour * a, clamp(a * 0.3, 0.0, 1.0));
}
