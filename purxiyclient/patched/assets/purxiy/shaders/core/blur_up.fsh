#version 330

// Dual Kawase upsample (Marius Bjørge, "Bandwidth-Efficient Rendering", SIGGRAPH 2015).

uniform sampler2D InSampler;

layout(std140) uniform BlurInfo {
    vec4 BlurParams; // xy = half a source texel, z = kernel offset
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 hp = BlurParams.xy * BlurParams.z;
    vec4 sum = texture(InSampler, texCoord + vec2(-hp.x * 2.0, 0.0));
    sum += texture(InSampler, texCoord + vec2(-hp.x, hp.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, hp.y * 2.0));
    sum += texture(InSampler, texCoord + vec2(hp.x, hp.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(hp.x * 2.0, 0.0));
    sum += texture(InSampler, texCoord + vec2(hp.x, -hp.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -hp.y * 2.0));
    sum += texture(InSampler, texCoord + vec2(-hp.x, -hp.y)) * 2.0;
    fragColor = vec4((sum / 12.0).rgb, 1.0);
}
