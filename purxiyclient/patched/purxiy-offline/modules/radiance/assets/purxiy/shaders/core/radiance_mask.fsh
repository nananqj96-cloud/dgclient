#version 330

#moj_import <minecraft:dynamictransforms.glsl>

// The silhouettes of what radiates: their slot (the draw's colour) wherever their texture is not see-through, cut out as
// vanilla cuts out entities.

uniform sampler2D Sampler0;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    if (texture(Sampler0, texCoord0).a < 0.1) {
        discard;
    }
    fragColor = vec4(ColorModulator.r, 0.0, 0.0, 1.0);
}
