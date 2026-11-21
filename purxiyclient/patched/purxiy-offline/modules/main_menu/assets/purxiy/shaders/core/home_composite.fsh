#version 330

// Lays the main menu's world over the screen: sharp behind the title, frosted and dimmed into a dusk of the accent colour behind the
// other menus. The world is drawn at twice the size on ordinary screens: sampling between four of its pixels averages
// them, which smooths the edges of the blocks.

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;

layout(std140) uniform HomeComposite {
    vec4 Mix;   // x = blur, y = dim, z = vignette, w = saturation
    vec4 Size;  // xy = size of the world's picture
    vec4 Dusk;  // rgb = what the world is multiplied by as it dims behind the menus (the accent colour's dusk)
    vec4 Lift;  // rgb = and what is added to it
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec3 sharp = texture(SceneSampler, texCoord).rgb;
    vec3 soft = texture(BlurSampler, texCoord).rgb;
    vec3 c = mix(sharp, soft, Mix.x);
    float grey = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(grey), c, Mix.w);
    c = mix(c, c * Dusk.rgb + Lift.rgb, Mix.y);
    vec2 v = texCoord - 0.5;
    float vignette = smoothstep(0.86, 0.22, length(v * vec2(1.0, 0.86)));
    c *= mix(1.0, vignette, Mix.z);
    fragColor = vec4(c, 1.0);
}
