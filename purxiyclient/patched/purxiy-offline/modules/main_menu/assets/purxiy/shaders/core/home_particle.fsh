#version 330

#moj_import <purxiy:home.glsl>

uniform sampler2D Sampler0;

in vec2 vUv;
in vec4 vColor;
in float vFog;
in float vGlow;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vUv);
    if (tex.a < 0.1) {
        discard;
    }
    vec3 col = tex.rgb * vColor.rgb;
    if (vGlow < 0.5) {
        col *= Light.rgb + Ambient.rgb;
    }
    col = mix(col, FogColor.rgb, vFog);
    float a = tex.a * vColor.a * (1.0 - vFog * 0.6);
    // what glows only adds light; petals and leaves cover what is behind them
    fragColor = vGlow > 0.5 ? vec4(col * a, 0.0) : vec4(col * a, a);
}
