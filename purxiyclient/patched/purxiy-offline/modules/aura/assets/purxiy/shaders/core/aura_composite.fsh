#version 330

// The aura's layer (premultiplied, as bright as the brightest light of each pixel) over the scene.

uniform sampler2D LayerSampler;

out vec4 fragColor;

void main() {
    vec4 aura = texelFetch(LayerSampler, ivec2(gl_FragCoord.xy), 0);
    if (aura.a <= 0.0 && aura.r + aura.g + aura.b <= 0.0) {
        discard;
    }
    fragColor = aura;
}
