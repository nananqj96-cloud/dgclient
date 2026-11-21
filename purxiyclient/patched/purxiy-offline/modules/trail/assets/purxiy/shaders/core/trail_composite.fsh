#version 330

// The trail's layer (premultiplied, as bright as the brightest light of each pixel) over the scene.

uniform sampler2D LayerSampler;

out vec4 fragColor;

void main() {
    vec4 trail = texelFetch(LayerSampler, ivec2(gl_FragCoord.xy), 0);
    if (trail.a <= 0.0 && trail.r + trail.g + trail.b <= 0.0) {
        discard;
    }
    fragColor = trail;
}
